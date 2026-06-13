package fr.mathgl.darkroomtimer.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FStopMathTest {

    @ParameterizedTest(name = "Step {0} with base {1} and fraction {2}/{3} should be {4}")
    @CsvSource(
        "0,  8000, 1, 3, 8000",
        "1,  8000, 1, 3, 10079",
        "2,  8000, 1, 3, 12699",
        "3,  8000, 1, 3, 16000",
        "-1, 8000, 1, 3, 6350",
        "-3, 8000, 1, 3, 4000"
    )
    fun testAdjustTime(step: Int, base: Long, num: Int, den: Int, expected: Long) {
        assertEquals(expected, FStopMath.adjustTime(base, num, den, step))
    }

    @Test
    fun testAdjustTimeEdgeCases() {
        // Denominator 0 -> 0 stops
        assertEquals(8000L, FStopMath.adjustTime(8000L, 1, 0, 1))

        // Result >= 0
        assertEquals(0L, FStopMath.adjustTime(8000L, -100, 1, 1))
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
