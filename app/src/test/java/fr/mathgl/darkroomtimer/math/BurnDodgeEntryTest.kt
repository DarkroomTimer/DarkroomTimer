package fr.mathgl.darkroomtimer.math

import org.junit.Assert.*
import org.junit.Test

class BurnDodgeEntryTest {

    @Test
    fun `burn entry calculates correct adjustment time`() {
        val entry = BurnDodgeEntry(
            id = 0,
            label = "ciel",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 3,
            contrastGrade = ContrastGrade.GRADE_2
        )

        // For base 16000ms, +1/3 stop = 16000 * (2^(1/3) - 1) ≈ 4096ms
        val adjustment = entry.adjustmentTimeMs(16000L)
        assertTrue("Adjustment should be positive for burn", adjustment > 0)
        assertTrue("Adjustment should be around 4096ms", adjustment in 4000L..4200L)
    }

    @Test
    fun `dodge entry calculates correct adjustment time`() {
        val entry = BurnDodgeEntry(
            id = 0,
            label = "visage",
            type = BurnDodgeType.DODGE,
            numerator = 1,
            denominator = 2,
            contrastGrade = ContrastGrade.GRADE_2
        )

        // For base 16000ms, -1/2 stop = 16000 * (1 - 2^(-0.5)) = 16000 * (1 - 0.707) ≈ 4686ms
        val adjustment = entry.adjustmentTimeMs(16000L)
        assertTrue("Adjustment should be positive (time to subtract)", adjustment > 0)
        assertTrue("Adjustment should be around 4686ms", adjustment in 4600L..4800L)
    }

    @Test
    fun `fractionLabel formats burn correctly`() {
        val entry = BurnDodgeEntry(
            id = 0,
            label = "",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 3,
            contrastGrade = ContrastGrade.GRADE_2
        )

        assertEquals("+1/3 stop", entry.fractionLabel)
    }

    @Test
    fun `fractionLabel formats dodge correctly`() {
        val entry = BurnDodgeEntry(
            id = 0,
            label = "",
            type = BurnDodgeType.DODGE,
            numerator = 1,
            denominator = 2,
            contrastGrade = ContrastGrade.GRADE_2
        )

        assertEquals("-1/2 stop", entry.fractionLabel)
    }

    @Test
    fun `fractionLabel formats 1 stop correctly`() {
        val burnEntry = BurnDodgeEntry(
            id = 0,
            label = "",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 1,
            contrastGrade = ContrastGrade.GRADE_2
        )

        val dodgeEntry = BurnDodgeEntry(
            id = 0,
            label = "",
            type = BurnDodgeType.DODGE,
            numerator = 1,
            denominator = 1,
            contrastGrade = ContrastGrade.GRADE_2
        )

        assertEquals("+1/1 stop", burnEntry.fractionLabel)
        assertEquals("-1/1 stop", dodgeEntry.fractionLabel)
    }

    @Test
    fun `adjustment time is zero when fraction is zero`() {
        // This shouldn't happen normally as denominator is never 0
        // But we test with a very small fraction
        val entry = BurnDodgeEntry(
            id = 0,
            label = "",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 12,
            contrastGrade = ContrastGrade.GRADE_2
        )

        val adjustment = entry.adjustmentTimeMs(10000L)
        assertTrue("Small fraction should give small adjustment", adjustment > 0)
        assertTrue("Small fraction adjustment should be small", adjustment < 1000L)
    }

    @Test
    fun `different base times produce proportional adjustments`() {
        val entry = BurnDodgeEntry(
            id = 0,
            label = "",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 1,
            contrastGrade = ContrastGrade.GRADE_2
        )

        // +1 stop = double the time, so adjustment = base
        assertEquals(8000L, entry.adjustmentTimeMs(8000L))
        assertEquals(16000L, entry.adjustmentTimeMs(16000L))
    }
}
