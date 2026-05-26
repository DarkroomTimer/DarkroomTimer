// app/src/test/java/fr/mathgl/darkroomtimer/ui/DigitTimePickerLogicTest.kt
package fr.mathgl.darkroomtimer.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitTimePickerLogicTest {

    // ── msToDigits ───────────────────────────────────────────────────────────

    @Test
    fun `msToDigits MM_SS_T for 8 seconds`() {
        assertArrayEquals(intArrayOf(0, 0, 0, 8, 0), msToDigits(8_000L, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `msToDigits MM_SS_T for 1min 23s 4 tenths`() {
        // 1*60000 + 23*1000 + 4*100 = 83400
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), msToDigits(83_400L, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `msToDigits MM_SS_T for max value 999000`() {
        // 999s = 16min 39s
        assertArrayEquals(intArrayOf(1, 6, 3, 9, 0), msToDigits(999_000L, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `msToDigits HH_MM_SS for 1h 30min 45s`() {
        val ms = (1 * 3600 + 30 * 60 + 45) * 1000L // = 5_445_000
        assertArrayEquals(intArrayOf(0, 1, 3, 0, 4, 5), msToDigits(ms, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `msToDigits HH_MM_SS for 99h 59min 59s`() {
        assertArrayEquals(intArrayOf(9, 9, 5, 9, 5, 9), msToDigits(359_999_000L, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    // ── applyDelta MM:SS.T ───────────────────────────────────────────────────

    @Test
    fun `applyDelta T carry into S2 — 00_09_9 + T becomes 00_10_0`() {
        // 9.9s = 9900ms, increment tenths (index 4, weight 100ms)
        assertEquals(10_000L, applyDelta(9_900L, 4, +1, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta S2 carry into S1 — 00_59_0 + S2 becomes 01_00_0`() {
        // 59s = 59000ms, increment S2 (index 3, weight 1000ms)
        assertEquals(60_000L, applyDelta(59_000L, 3, +1, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta S1 carry into M — 00_50_0 + S1 becomes 01_00_0`() {
        // 50s = 50000ms, increment S1 (index 2, weight 10000ms)
        assertEquals(60_000L, applyDelta(50_000L, 2, +1, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta clamp to min 100ms`() {
        assertEquals(100L, applyDelta(200L, 4, -2, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta clamp to max 999000ms`() {
        assertEquals(999_000L, applyDelta(998_900L, 4, +2, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    // ── applyDelta HH:MM:SS ──────────────────────────────────────────────────

    @Test
    fun `applyDelta HH_MM_SS S2 carry — 00_00_59 + S2 becomes 00_01_00`() {
        assertEquals(60_000L, applyDelta(59_000L, 5, +1, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `applyDelta HH_MM_SS M carry into H — 00_59_00 + M2 becomes 01_00_00`() {
        val ms = 59 * 60 * 1000L // 59 minutes
        assertEquals(3_600_000L, applyDelta(ms, 3, +1, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `applyDelta HH_MM_SS clamp to max 359999000`() {
        assertEquals(359_999_000L, applyDelta(359_998_000L, 5, +2, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `applyDelta HH_MM_SS clamp to min 1000`() {
        assertEquals(1_000L, applyDelta(2_000L, 5, -2, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }
}
