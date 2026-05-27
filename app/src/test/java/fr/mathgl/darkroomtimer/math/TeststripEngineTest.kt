package fr.mathgl.darkroomtimer.math

import org.junit.Assert.*
import org.junit.Test

class TeststripEngineTest {

    @Test
    fun `F-Stop Separate mode calculates absolute times correctly`() {
        // baseTimeMs = 1000ms (1s), increment = 1/2 stop, patchCount = 3
        // index 0: 1000 * 2^(0) = 1000
        // index 1: 1000 * 2^(0.5) = 1414
        // index 2: 1000 * 2^(1.0) = 2000
        val engine = TeststripEngine(
            baseTimeMs = 1000L,
            numerator = 1,
            denominator = 2,
            patchCount = 3,
            mode = TeststripMode.SEPARATE,
            incrementType = IncrementType.F_STOP
        )

        assertEquals(1000L, engine.getRelayDuration(0))
        assertEquals(1414L, engine.getRelayDuration(1))
        assertEquals(2000L, engine.getRelayDuration(2))
    }

    @Test
    fun `F-Stop Incremental mode calculates differential times correctly`() {
        // baseTimeMs = 1000ms (1s), increment = 1/2 stop, patchCount = 3
        // index 0: 1000
        // index 1: 1414 - 1000 = 414
        // index 2: 2000 - 1414 = 586
        val engine = TeststripEngine(
            baseTimeMs = 1000L,
            numerator = 1,
            denominator = 2,
            patchCount = 3,
            mode = TeststripMode.INCREMENTAL,
            incrementType = IncrementType.F_STOP
        )

        assertEquals(1000L, engine.getRelayDuration(0))
        assertEquals(414L, engine.getRelayDuration(1))
        assertEquals(586L, engine.getRelayDuration(2))
    }

    @Test
    fun `Seconds Separate mode calculates absolute times correctly`() {
        // baseTimeMs = 1000ms, incrementMs = 500ms, patchCount = 3
        // index 0: 1000
        // index 1: 1000 + 500 = 1500
        // index 2: 1000 + 1000 = 2000
        val engine = TeststripEngine(
            baseTimeMs = 1000L,
            numerator = 0,
            denominator = 1,
            patchCount = 3,
            mode = TeststripMode.SEPARATE,
            incrementType = IncrementType.SECONDS,
            incrementMs = 500L
        )

        assertEquals(1000L, engine.getRelayDuration(0))
        assertEquals(1500L, engine.getRelayDuration(1))
        assertEquals(2000L, engine.getRelayDuration(2))
    }

    @Test
    fun `Seconds Incremental mode calculates differential times correctly`() {
        // baseTimeMs = 1000ms, incrementMs = 500ms, patchCount = 3
        // index 0: 1000
        // index 1: 1500 - 1000 = 500
        // index 2: 2000 - 1500 = 500
        val engine = TeststripEngine(
            baseTimeMs = 1000L,
            numerator = 0,
            denominator = 1,
            patchCount = 3,
            mode = TeststripMode.INCREMENTAL,
            incrementType = IncrementType.SECONDS,
            incrementMs = 500L
        )

        assertEquals(1000L, engine.getRelayDuration(0))
        assertEquals(500L, engine.getRelayDuration(1))
        assertEquals(500L, engine.getRelayDuration(2))
    }

    @Test
    fun `should throw for baseTime below 100ms`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(
                baseTimeMs = 99, numerator = 1, denominator = 3, patchCount = 6,
                mode = TeststripMode.SEPARATE, incrementType = IncrementType.F_STOP
            )
        }
    }

    @Test
    fun `should throw for baseTime above 999000ms`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(
                baseTimeMs = 999_001, numerator = 1, denominator = 3, patchCount = 6,
                mode = TeststripMode.SEPARATE, incrementType = IncrementType.F_STOP
            )
        }
    }

    @Test
    fun `should throw for patchCount below 3`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(
                baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 2,
                mode = TeststripMode.SEPARATE, incrementType = IncrementType.F_STOP
            )
        }
    }

    @Test
    fun `should throw for patchCount above 12`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(
                baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 13,
                mode = TeststripMode.SEPARATE, incrementType = IncrementType.F_STOP
            )
        }
    }

    @Test
    fun `should throw for denominator zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(
                baseTimeMs = 8000, numerator = 1, denominator = 0, patchCount = 6,
                mode = TeststripMode.SEPARATE, incrementType = IncrementType.F_STOP
            )
        }
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
        val engine = TeststripEngine(
            baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6,
            mode = TeststripMode.SEPARATE, incrementType = IncrementType.F_STOP
        )
        val (n, d) = engine.simplifiedFraction

        assertEquals(1, n)
        assertEquals(3, d)
    }

    @Test
    fun `simplifiedFraction reduces 2_6 to 1_3`() {
        val engine = TeststripEngine(
            baseTimeMs = 8000, numerator = 2, denominator = 6, patchCount = 6,
            mode = TeststripMode.SEPARATE, incrementType = IncrementType.F_STOP
        )
        val (n, d) = engine.simplifiedFraction

        assertEquals(1, n)
        assertEquals(3, d)
    }
}
