package com.f1telemetry.viewer.ui

object Fmt {
    fun lapTime(ms: Long): String {
        if (ms <= 0) return "--:--.---"
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return "%d:%02d.%03d".format(minutes, seconds, millis)
    }

    fun delta(ms: Int): String {
        if (ms == 0) return "0.000"
        val sign = if (ms > 0) "+" else "-"
        val a = kotlin.math.abs(ms)
        return "%s%d.%03d".format(sign, a / 1000, a % 1000)
    }

    fun sector(ms: Int): String {
        if (ms <= 0) return "--.---"
        return "%d.%03d".format(ms / 1000, ms % 1000)
    }

    fun size(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
