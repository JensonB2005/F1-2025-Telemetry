package com.f1telemetry.viewer.telemetry

/**
 * Little-endian sequential reader over a raw UDP payload.
 *
 * The F1 25 (and 22/23/24) telemetry packets are packed C structs transmitted
 * little-endian. This reader advances an internal cursor as fields are read and
 * lets callers [seek] to an absolute offset, which we use to jump to a specific
 * car's block inside the per-car arrays.
 */
class ByteReader(private val buf: ByteArray, private val limit: Int = buf.size) {
    var pos: Int = 0
        private set

    fun seek(offset: Int) { pos = offset }
    fun skip(n: Int) { pos += n }
    fun remaining(): Int = limit - pos
    fun hasRemaining(n: Int): Boolean = pos + n <= limit

    fun u8(): Int = buf[pos++].toInt() and 0xFF
    fun i8(): Int = buf[pos++].toInt()

    fun u16(): Int {
        val v = (buf[pos].toInt() and 0xFF) or ((buf[pos + 1].toInt() and 0xFF) shl 8)
        pos += 2
        return v
    }

    fun i16(): Int {
        val v = u16()
        return if (v >= 0x8000) v - 0x10000 else v
    }

    fun u32(): Long {
        val v = (buf[pos].toLong() and 0xFF) or
                ((buf[pos + 1].toLong() and 0xFF) shl 8) or
                ((buf[pos + 2].toLong() and 0xFF) shl 16) or
                ((buf[pos + 3].toLong() and 0xFF) shl 24)
        pos += 4
        return v
    }

    fun i32(): Int {
        val v = (buf[pos].toInt() and 0xFF) or
                ((buf[pos + 1].toInt() and 0xFF) shl 8) or
                ((buf[pos + 2].toInt() and 0xFF) shl 16) or
                ((buf[pos + 3].toInt() and 0xFF) shl 24)
        pos += 4
        return v
    }

    fun u64(): Long {
        var v = 0L
        for (i in 0 until 8) {
            v = v or ((buf[pos + i].toLong() and 0xFF) shl (8 * i))
        }
        pos += 8
        return v
    }

    fun f32(): Float = Float.fromBits(i32())

    fun f64(): Double = Double.fromBits(
        (0 until 8).fold(0L) { acc, i -> acc or ((buf[pos + i].toLong() and 0xFF) shl (8 * i)) }
            .also { pos += 8 }
    )

    /** Reads a fixed-length ASCII string, trimming at the first NUL. */
    fun str(length: Int): String {
        val end = minOf(pos + length, limit)
        val sb = StringBuilder()
        var i = pos
        while (i < end) {
            val c = buf[i].toInt() and 0xFF
            if (c == 0) break
            sb.append(c.toChar())
            i++
        }
        pos += length
        return sb.toString()
    }

    fun u8Array(n: Int): IntArray = IntArray(n) { u8() }
    fun u16Array(n: Int): IntArray = IntArray(n) { u16() }
    fun f32Array(n: Int): FloatArray = FloatArray(n) { f32() }
}
