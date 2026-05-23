package fr.mathgl.darkroomtimer.math

import org.junit.Assert.assertEquals
import org.junit.Test

class FStopMathTest {

    @Test
    fun testAdjustTime() {
        val base = 8000L
        val num = 1
        val den = 3

        // Validation Table
        assertEquals("Step 0", 8000L, FStopMath.adjustTime(base, num, den, 0))
        assertEquals("Step 1", 10079L, FStopMath.adjustTime(base, num, den, 1))
        assertEquals("Step 2", 12699L, FStopMath.adjustTime(base, num, den, 2))
        assertEquals("Step 3", 16000L, FStopMath.adjustTime(base, num, den, 3))
        assertEquals("Step -1", 6349L, FStopMath.adjustTime(base, num, den, -1))
        assertEquals("Step -3", 4000L, FStopMath.adjustTime(base, num, den, -3))
    }

    @Test
    fun testAdjustTimeEdgeCases() {
        // Denominator 0 -> 0 stops
        assertEquals("Denominator 0", 8000L, FStopMath.adjustTime(8000L, 1, 0, 1))

        // Result >= 0
        assertEquals("Negative result should be 0", 0L, FStopMath.adjustTime(8000L, -100, 1, 1))
    }

    @Test
    fun testSimplify() {
        assertEquals(Pair(1, 3), FStopMath.simplify(2, 6))
        assertEquals(Pair(1, 1), FStopMath.simplify(5, 5))
        assertEquals(Pair(-1, 3), FStopMath.simplify(-2, 6))
        assertEquals(Pair(-1, 3), FStopMath.simplify(2, -6))
        assertEquals(Pair(1, 3), FStopMath.simplify(-2, -6))
    }

    @Test
    fun testFormatStop() {
        assertEquals("0", FStopMath.formatStop(0, 1))
        assertEquals("1", FStopMath.formatStop(1, 1))
        assertEquals("1/3", FStopMath.formatStop(1, 3))
        assertEquals("1 1/3", FStopMath.formatStop(4, 3))
        assertEquals("-2/3", FStopMath.formatStop(-2, 3))
        assertEquals("-2 1/3", FStopMath.formatStop(-7, 3))
        assertEquals("-1", FStopMath.formatStop(-3, 3))
        assertEquals("0", FStopMath.formatStop(1, 0))
    }
}
