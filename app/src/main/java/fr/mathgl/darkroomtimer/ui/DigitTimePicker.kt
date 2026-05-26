// app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt
package fr.mathgl.darkroomtimer.ui

enum class DigitTimeFormat {
    MINUTES_SECONDS_TENTHS,  // MM:SS.T — countdown, teststrip
    HOURS_MINUTES_SECONDS    // HH:MM:SS — développement
}

internal fun msToDigits(ms: Long, format: DigitTimeFormat): IntArray = when (format) {
    DigitTimeFormat.MINUTES_SECONDS_TENTHS -> {
        val t = (ms / 100 % 10).toInt()
        val totalS = ms / 1000
        val s = (totalS % 60).toInt()
        val m = (totalS / 60).toInt()
        intArrayOf(m / 10, m % 10, s / 10, s % 10, t)
    }
    DigitTimeFormat.HOURS_MINUTES_SECONDS -> {
        val totalS = ms / 1000
        val s = (totalS % 60).toInt()
        val totalM = totalS / 60
        val m = (totalM % 60).toInt()
        val h = (totalM / 60).toInt()
        intArrayOf(h / 10, h % 10, m / 10, m % 10, s / 10, s % 10)
    }
}

internal fun applyDelta(ms: Long, digitIndex: Int, delta: Int, format: DigitTimeFormat): Long {
    val (min, max) = when (format) {
        DigitTimeFormat.MINUTES_SECONDS_TENTHS -> 100L to 999_000L
        DigitTimeFormat.HOURS_MINUTES_SECONDS  -> 1_000L to 359_999_000L
    }
    val weight = when (format) {
        DigitTimeFormat.MINUTES_SECONDS_TENTHS ->
            longArrayOf(600_000L, 60_000L, 10_000L, 1_000L, 100L)[digitIndex]
        DigitTimeFormat.HOURS_MINUTES_SECONDS  ->
            longArrayOf(36_000_000L, 3_600_000L, 600_000L, 60_000L, 10_000L, 1_000L)[digitIndex]
    }
    return (ms + delta * weight).coerceIn(min, max)
}
