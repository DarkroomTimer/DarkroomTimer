package fr.mathgl.darkroomtimer.system

import org.junit.Assert.assertEquals
import org.junit.Test

class LuminosityManagerTest {

    @Test
    fun `test smoothing filter average`() {
        val calculator = LuminosityCalculator(3000L)

        // t=0, lux=10
        assertEquals(10f, calculator.updateSmoothingFilter(10f, 0), 0.01f)
        // t=1000, lux=20
        assertEquals(15f, calculator.updateSmoothingFilter(20f, 1000), 0.01f)
        // t=2000, lux=30
        assertEquals(20f, calculator.updateSmoothingFilter(30f, 2000), 0.01f)
        // t=3000, lux=40.
        // Window is 3s. At t=3000, samples are at 0, 1000, 2000, 3000.
        // The sample at t=0 should still be in window (3000 - 0 <= 3000)
        assertEquals(25f, calculator.updateSmoothingFilter(40f, 3000), 0.01f)

        // t=3001, lux=50.
        // Sample at t=0 is now outside (3001 - 0 > 3000)
        // Remaining: 1000(20), 2000(30), 3000(40), 3001(50) -> avg = 140/4 = 35
        assertEquals(35f, calculator.updateSmoothingFilter(50f, 3001), 0.01f)
    }

    @Test
    fun `test adaptive mapping and clamping`() {
        val calculator = LuminosityCalculator()
        val config = LuminosityManager.Config(
            mode = LuminosityManager.Mode.ADAPTIVE,
            minBrightness = 0.1f,
            maxBrightness = 0.5f,
            maxLux = 100f
        )

        // lux = 0 -> mapped 0 -> clamped to 0.1
        assertEquals(0.1f, calculator.calculateBrightness(0f, config), 0.01f)
        // lux = 20 -> mapped 0.2 -> in range [0.1, 0.5] -> 0.2
        assertEquals(0.2f, calculator.calculateBrightness(20f, config), 0.01f)
        // lux = 80 -> mapped 0.8 -> clamped to 0.5
        assertEquals(0.5f, calculator.calculateBrightness(80f, config), 0.01f)
    }

    @Test
    fun `test fixed mode mapping`() {
        val calculator = LuminosityCalculator()
        val config = LuminosityManager.Config(
            mode = LuminosityManager.Mode.FIXED,
            fixedBrightness = 0.3f
        )

        assertEquals(0.3f, calculator.calculateBrightness(0f, config), 0.01f)
        assertEquals(0.3f, calculator.calculateBrightness(100f, config), 0.01f)
    }
}
