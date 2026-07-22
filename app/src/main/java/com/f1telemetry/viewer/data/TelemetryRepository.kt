package com.f1telemetry.viewer.data

import com.f1telemetry.viewer.analysis.HandlingEstimator
import com.f1telemetry.viewer.analysis.MistakeDetector
import com.f1telemetry.viewer.analysis.SetupAdvisor
import com.f1telemetry.viewer.net.SessionRecorder
import com.f1telemetry.viewer.telemetry.MotionData
import com.f1telemetry.viewer.telemetry.Parsed
import com.f1telemetry.viewer.telemetry.PacketParser
import com.f1telemetry.viewer.telemetry.TelemetryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central store: consumes raw UDP payloads (live or replayed), parses them,
 * maintains the aggregated [TelemetryState], builds per-lap traces, and runs
 * the mistake / setup analysis. Packets are processed on a single stream at a
 * time (live receiver OR replayer), so the working buffers need no locking.
 */
object TelemetryRepository {

    private val _state = MutableStateFlow(TelemetryState())
    val state: StateFlow<TelemetryState> = _state.asStateFlow()

    private val handling = HandlingEstimator()
    private val working = ArrayList<TraceSample>(1400)
    private var curLapNum = -1
    private var lapInvalidFlag = false
    private var lastSampleDistance = -1000f
    private var latestMotion = MotionData()
    private var lastAdviceMs = 0L
    private var lastTracePublishMs = 0L
    private val lapInsights = ArrayList<Insight>()
    private var liveInsights: List<Insight> = emptyList()

    @Volatile private var recorder: SessionRecorder? = null

    fun setRecorder(r: SessionRecorder?) { recorder = r }
    fun isRecording(): Boolean = recorder != null

    /** Reset transient analysis state (e.g. when switching source or session). */
    fun resetSession(source: String) {
        handling.reset()
        working.clear()
        curLapNum = -1
        lapInvalidFlag = false
        lastSampleDistance = -1000f
        lapInsights.clear()
        liveInsights = emptyList()
        _state.value = TelemetryState(source = source)
    }

    fun onRawPacket(buf: ByteArray, length: Int, nowMs: Long = System.currentTimeMillis()) {
        recorder?.write(buf, length)
        val parsed = PacketParser.parse(buf, length) ?: return
        apply(parsed, nowMs)
    }

    private fun apply(parsed: Parsed, nowMs: Long) {
        val prev = _state.value
        var next = prev.copy(
            connected = true,
            lastPacketAtMs = nowMs,
            packetsReceived = prev.packetsReceived + 1,
            header = parsed.header,
            packetFormat = parsed.header.packetFormat,
            gameYear = parsed.header.gameYear,
        )

        when (parsed) {
            is Parsed.Telemetry -> {
                next = next.copy(telemetry = parsed.data)
                handling.sample(parsed.data.steer, latestMotion.gForceLat, parsed.data.speedKmh)
            }
            is Parsed.Motion -> { latestMotion = parsed.data; next = next.copy(motion = parsed.data) }
            is Parsed.Status -> next = next.copy(status = parsed.data)
            is Parsed.Damage -> next = next.copy(damage = parsed.data)
            is Parsed.Setup -> next = next.copy(setup = parsed.data)
            is Parsed.Session -> next = next.copy(session = parsed.info)
            is Parsed.Participants -> next = next.copy(participants = parsed.list)
            is Parsed.History -> {
                if (parsed.carIdx == parsed.header.playerCarIndex && parsed.bestLapTimeMs > 0) {
                    next = next.copy(bestLapTimeMs = parsed.bestLapTimeMs)
                }
            }
            is Parsed.Event -> next = next.copy(events = pushEvent(prev.events, parsed.event))
            is Parsed.Lap -> next = handleLap(next, parsed)
            is Parsed.Other -> {}
        }

        // Periodic live setup advice (once per second).
        if (nowMs - lastAdviceMs > 1000) {
            lastAdviceMs = nowMs
            liveInsights = SetupAdvisor.advise(next, handling.balance())
        }
        next = next.copy(insights = (liveInsights + lapInsights.asReversed()).take(40))
        _state.value = next
    }

    private fun handleLap(state: TelemetryState, parsed: Parsed.Lap): TelemetryState {
        val lap = parsed.data
        var next = state.copy(lap = lap)
        if (lap.currentLapInvalid) lapInvalidFlag = true

        // Lap boundary: finalise the completed lap.
        if (curLapNum >= 1 && lap.currentLapNum != curLapNum && working.size > 10) {
            val finished = LapTrace(
                lapNumber = curLapNum,
                samples = ArrayList(working),
                lapTimeMs = lap.lastLapTimeMs,
                valid = !lapInvalidFlag,
            )
            next = next.copy(lastTrace = finished)
            val newBest = state.bestTrace == null ||
                (finished.valid && finished.lapTimeMs in 1 until (state.bestTrace?.lapTimeMs ?: Long.MAX_VALUE))
            if (newBest && finished.valid) next = next.copy(bestTrace = finished)

            val insights = MistakeDetector.analyseLap(finished, next.bestTrace)
            lapInsights.addAll(insights)
            while (lapInsights.size > 30) lapInsights.removeAt(0)

            working.clear()
            lastSampleDistance = -1000f
            lapInvalidFlag = lap.currentLapInvalid
        }
        curLapNum = lap.currentLapNum

        // Distance-based sampling of the current lap.
        val t = state.telemetry
        if (lap.lapDistance >= 0 && lap.lapDistance - lastSampleDistance >= 5f) {
            lastSampleDistance = lap.lapDistance
            working.add(
                TraceSample(
                    distance = lap.lapDistance,
                    timeMs = lap.currentLapTimeMs,
                    speed = t.speedKmh,
                    throttle = t.throttle,
                    brake = t.brake,
                    steer = t.steer,
                    gear = t.gear,
                    gLat = latestMotion.gForceLat,
                    gLon = latestMotion.gForceLon,
                    rpm = t.engineRpm,
                )
            )
        }

        // Publish a live snapshot of the current lap trace (throttled).
        val nowMs = state.lastPacketAtMs
        if (nowMs - lastTracePublishMs > 120 && working.isNotEmpty()) {
            lastTracePublishMs = nowMs
            next = next.copy(
                currentTrace = LapTrace(lap.currentLapNum, ArrayList(working), lap.currentLapTimeMs, !lapInvalidFlag)
            )
        }
        return next
    }

    private fun pushEvent(list: List<TelemetryEvent>, e: TelemetryEvent): List<TelemetryEvent> {
        val out = ArrayList<TelemetryEvent>(list.size + 1)
        out.add(e)
        out.addAll(list)
        while (out.size > 30) out.removeAt(out.size - 1)
        return out
    }

    fun markDisconnectedIfStale(nowMs: Long) {
        val s = _state.value
        if (s.connected && nowMs - s.lastPacketAtMs > 3000) {
            _state.value = s.copy(connected = false)
        }
    }
}
