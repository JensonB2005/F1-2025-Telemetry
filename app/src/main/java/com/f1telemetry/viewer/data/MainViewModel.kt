package com.f1telemetry.viewer.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.f1telemetry.viewer.net.SessionRecorder
import com.f1telemetry.viewer.net.SessionReplayer
import com.f1telemetry.viewer.net.UdpTelemetryReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Mode { IDLE, LIVE, REPLAY }

data class ControllerState(
    val mode: Mode = Mode.IDLE,
    val port: Int = 20777,
    val recording: Boolean = false,
    val recordingName: String? = null,
    val error: String? = null,
    val recordings: List<RecordingFile> = emptyList(),
    val replay: ReplayState = ReplayState(),
)

data class RecordingFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val modified: Long,
)

data class ReplayState(
    val playing: Boolean = false,
    val fileName: String? = null,
    val speed: Float = 1f,
    val packetIndex: Long = 0,
    val totalPackets: Long = 0,
    val offsetMs: Long = 0,
    val durationMs: Long = 0,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val telemetry: StateFlow<TelemetryState> = TelemetryRepository.state

    private val _controller = MutableStateFlow(ControllerState())
    val controller: StateFlow<ControllerState> = _controller.asStateFlow()

    private val recordingsDir: File =
        File(getApplication<Application>().filesDir, "recordings").apply { mkdirs() }

    private var networkJob: Job? = null
    private var replayJob: Job? = null
    private var staleJob: Job? = null

    init {
        refreshRecordings()
        staleJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                TelemetryRepository.markDisconnectedIfStale(System.currentTimeMillis())
            }
        }
        // Start listening automatically so the app is ready without touching the screen.
        startLive()
    }

    /** Only accept a valid UDP port; ignore junk so the socket never gets an out-of-range value. */
    fun setPort(port: Int) {
        if (port in 1..65535) _controller.value = _controller.value.copy(port = port)
    }

    private fun resolvedPort(): Int = _controller.value.port.let { if (it in 1..65535) it else 20777 }

    fun startLive() {
        stopReplay()
        networkJob?.cancel()
        val port = resolvedPort()
        // Repair the stored port if it was somehow invalid so the UI reflects reality.
        _controller.value = _controller.value.copy(port = port, mode = Mode.LIVE, error = null)
        TelemetryRepository.resetSession("Live UDP :$port")
        val receiver = UdpTelemetryReceiver(port, getApplication())
        networkJob = viewModelScope.launch(Dispatchers.IO) {
            receiver.listen(
                onError = { e ->
                    _controller.value = _controller.value.copy(error = e.message ?: "UDP error")
                },
                onPacket = { buf, len, sender -> TelemetryRepository.onRawPacket(buf, len, sender) },
            )
        }
    }

    fun stopLive() {
        networkJob?.cancel(); networkJob = null
        if (_controller.value.recording) stopRecording()
        _controller.value = _controller.value.copy(mode = Mode.IDLE)
    }

    /**
     * Sends dummy packets to the app's own listening socket (loopback + this
     * device's LAN IP) to verify the receive path. If the datagram counter then
     * increments, the app is listening correctly and any missing live data is a
     * game/network configuration problem, not the app.
     */
    fun sendTestPacket() {
        if (_controller.value.mode != Mode.LIVE) startLive()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val port = resolvedPort()
                val data = ByteArray(64).also {
                    it[0] = (2025 and 0xFF).toByte()
                    it[1] = ((2025 shr 8) and 0xFF).toByte()
                }
                DatagramSocket().use { sender ->
                    sender.broadcast = true
                    listOf("127.0.0.1", "255.255.255.255").forEach { host ->
                        runCatching {
                            sender.send(DatagramPacket(data, data.size, InetAddress.getByName(host), port))
                        }
                    }
                }
            }.onFailure { e ->
                _controller.value = _controller.value.copy(error = "Self-test failed: ${e.message}")
            }
        }
    }

    fun toggleRecording() {
        if (_controller.value.recording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(recordingsDir, "session_$stamp.f1rec")
        val rec = SessionRecorder(file)
        recorderRef = rec
        TelemetryRepository.setRecorder(rec)
        _controller.value = _controller.value.copy(recording = true, recordingName = file.name)
    }

    private fun stopRecording() {
        recorderRef?.close()
        recorderRef = null
        TelemetryRepository.setRecorder(null)
        _controller.value = _controller.value.copy(recording = false, recordingName = null)
        refreshRecordings()
    }

    // Keep a reference so we can flush/close on stop.
    private var recorderRef: SessionRecorder? = null

    fun refreshRecordings() {
        val files = recordingsDir.listFiles { f -> f.name.endsWith(".f1rec") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { RecordingFile(it.name, it.absolutePath, it.length(), it.lastModified()) }
            ?: emptyList()
        _controller.value = _controller.value.copy(recordings = files)
    }

    fun deleteRecording(path: String) {
        File(path).delete()
        refreshRecordings()
    }

    fun playRecording(path: String, speed: Float = 1f) {
        stopLive()
        replayJob?.cancel()
        val file = File(path)
        if (!file.exists()) return
        TelemetryRepository.resetSession("Replay: ${file.name}")
        val replayer = SessionReplayer(file)
        val (total, duration) = replayer.index()
        _controller.value = _controller.value.copy(
            mode = Mode.REPLAY,
            replay = ReplayState(
                playing = true, fileName = file.name, speed = speed,
                totalPackets = total, durationMs = duration,
            ),
        )
        replayJob = viewModelScope.launch(Dispatchers.IO) {
            replayer.play(
                speed = speed,
                onProgress = { idx, offset ->
                    val c = _controller.value
                    _controller.value = c.copy(replay = c.replay.copy(packetIndex = idx, offsetMs = offset))
                },
                onPacket = { buf, len -> TelemetryRepository.onRawPacket(buf, len) },
            )
            val c = _controller.value
            _controller.value = c.copy(replay = c.replay.copy(playing = false))
        }
    }

    fun setReplaySpeed(speed: Float) {
        val c = _controller.value
        _controller.value = c.copy(replay = c.replay.copy(speed = speed))
        if (c.replay.playing && c.replay.fileName != null) {
            val path = File(recordingsDir, c.replay.fileName).absolutePath
            playRecording(path, speed)
        }
    }

    fun stopReplay() {
        replayJob?.cancel(); replayJob = null
        val c = _controller.value
        if (c.mode == Mode.REPLAY) {
            _controller.value = c.copy(mode = Mode.IDLE, replay = c.replay.copy(playing = false))
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkJob?.cancel()
        replayJob?.cancel()
        staleJob?.cancel()
        recorderRef?.close()
        TelemetryRepository.setRecorder(null)
    }
}
