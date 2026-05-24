package fr.mathgl.darkroomtimer.math

import org.junit.Assert.*
import org.junit.Test

class TeststripEngineTest {

    @Test
    fun `should calculate correct times for base 8000, 1_3 stop, 6 patches`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        val times = engine.patchTimesMs

        assertEquals(8000L, times[0])   // Patch 1: 8.0s
        assertEquals(10079L, times[1])  // Patch 2: 10.1s
        assertEquals(12699L, times[2])  // Patch 3: 12.7s
        assertEquals(16000L, times[3])  // Patch 4: 16.0s
        assertEquals(20159L, times[4])  // Patch 5: 20.2s
        assertEquals(25398L, times[5])  // Patch 6: 25.4s
    }

    @Test
    fun `should calculate differential times for incremental mode`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        val diffs = engine.differentialTimesMs

        assertEquals(8000L, diffs[0])    // Patch 1: 8.0s (base)
        assertEquals(2079L, diffs[1])    // Patch 2: 10079 - 8000
        assertEquals(2620L, diffs[2])    // Patch 3: 12699 - 10079
        assertEquals(3301L, diffs[3])    // Patch 4: 16000 - 12699
        assertEquals(4159L, diffs[4])    // Patch 5: 20159 - 16000
        assertEquals(5239L, diffs[5])    // Patch 6: 25398 - 20159
    }

    @Test
    fun `should throw for baseTime below 100ms`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 99, numerator = 1, denominator = 3, patchCount = 6)
        }
    }

    @Test
    fun `should throw for baseTime above 999000ms`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 999_001, numerator = 1, denominator = 3, patchCount = 6)
        }
    }

    @Test
    fun `should throw for patchCount below 3`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 2)
        }
    }

    @Test
    fun `should throw for patchCount above 7`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 8)
        }
    }

    @Test
    fun `should throw for denominator zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 0, patchCount = 6)
        }
    }

    @Test
    fun `should use all time as base for 1 stop increment`() {
        val engine = TeststripEngine(baseTimeMs = 10000, numerator = 1, denominator = 1, patchCount = 4)
        val times = engine.patchTimesMs

        assertEquals(10000L, times[0])  // 10000 * 2^0 = 10000
        assertEquals(20000L, times[1])  // 10000 * 2^1 = 20000
        assertEquals(40000L, times[2])  // 10000 * 2^2 = 40000
        assertEquals(80000L, times[3])  // 10000 * 2^3 = 80000
    }

    @Test
    fun `should use 1_2 stop increment correctly`() {
        val engine = TeststripEngine(baseTimeMs = 10000, numerator = 1, denominator = 2, patchCount = 3)
        val times = engine.patchTimesMs

        assertEquals(10000L, times[0])      // 10000 * 2^0 = 10000
        assertEquals(14142L, times[1])      // 10000 * 2^0.5 ≈ 14142
        assertEquals(20000L, times[2])      // 10000 * 2^1 = 20000
    }

    @Test
    fun `differentialTimeForPatch returns correct diff for given patch index`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)

        assertEquals(8000L, engine.differentialTimeForPatch(0))
        assertEquals(2079L, engine.differentialTimeForPatch(1))
        assertEquals(2620L, engine.differentialTimeForPatch(2))
        assertEquals(5239L, engine.differentialTimeForPatch(5))
    }

    @Test
    fun `formatStopTime produces correct MM_SS_d format`() {
        assertEquals("00:08.0", TeststripEngine.formatStopTime(8000L))
        assertEquals("01:05.4", TeststripEngine.formatStopTime(65400L))
        assertEquals("16:39.0", TeststripEngine.formatStopTime(999_000L))
        assertEquals("00:00.1", TeststripEngine.formatStopTime(100L))
    }

    @Test
    fun `formatStopTime clamps negative values to zero`() {
        assertEquals("00:00.0", TeststripEngine.formatStopTime(-500L))
    }

    @Test
    fun `simplifiedFraction returns reduced numerator and denominator`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        val (n, d) = engine.simplifiedFraction

        assertEquals(1, n)
        assertEquals(3, d)
    }

    @Test
    fun `simplifiedFraction reduces 2_6 to 1_3`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 2, denominator = 6, patchCount = 6)
        val (n, d) = engine.simplifiedFraction

        assertEquals(1, n)
        assertEquals(3, d)
    }
}
