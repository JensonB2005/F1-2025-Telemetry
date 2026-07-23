package com.f1telemetry.viewer.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.coroutineContext

/**
 * Replays a ".f1rec" recording, dispatching each stored payload to [onPacket]
 * with timing scaled by [speed] (1.0 = real time). Coroutine-based so it can be
 * cancelled and honours pause via the caller cancelling/restarting.
 */
class SessionReplayer(private val file: File) {

    var totalPackets: Long = 0
        private set

    suspend fun play(
        speed: Float,
        startAtMs: Long = 0L,
        onProgress: (packetIndex: Long, offsetMs: Long) -> Unit,
        onPacket: (buf: ByteArray, length: Int) -> Unit,
    ) {
        DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
            val magic = ByteArray(5)
            input.readFully(magic)
            if (String(magic) != "F1REC") return
            input.skipBytes(3)
            var lastOffset = startAtMs
            var index = 0L
            while (coroutineContext.isActive) {
                val offsetMs: Long
                val length: Int
                try {
                    offsetMs = input.readLong()
                    length = input.readInt()
                } catch (e: Exception) {
                    break // EOF
                }
                if (length <= 0 || length > 4096) break
                val buf = ByteArray(length)
                input.readFully(buf)
                index++
                if (offsetMs < startAtMs) continue
                val waitMs = ((offsetMs - lastOffset) / speed).toLong().coerceIn(0, 5000)
                if (waitMs > 0) delay(waitMs)
                coroutineContext.ensureActive()
                onPacket(buf, length)
                onProgress(index, offsetMs)
                lastOffset = offsetMs
            }
        }
    }

    /** Scans the file to count packets and duration without dispatching. */
    fun index(): Pair<Long, Long> {
        var count = 0L
        var lastOffset = 0L
        try {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                val magic = ByteArray(5)
                input.readFully(magic)
                if (String(magic) != "F1REC") return 0L to 0L
                input.skipBytes(3)
                while (true) {
                    val offsetMs = input.readLong()
                    val length = input.readInt()
                    if (length <= 0 || length > 4096) break
                    input.skipBytes(length)
                    count++
                    lastOffset = offsetMs
                }
            }
        } catch (_: Exception) {}
        totalPackets = count
        return count to lastOffset
    }
}
