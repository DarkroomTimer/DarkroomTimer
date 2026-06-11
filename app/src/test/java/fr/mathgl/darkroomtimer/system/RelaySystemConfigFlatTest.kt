package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.system.drivers.DemoRelayController
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeHttpRelayController
import fr.mathgl.darkroomtimer.system.drivers.NullRelayController
import fr.mathgl.darkroomtimer.system.drivers.TasmotaRelayController
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelaySystemConfigFlatTest {

    private fun buildSystem(config: RelaySystemConfigFlat): RelaySystem =
        config.buildRelaySystem(TestScope(StandardTestDispatcher()))

    @Test
    fun `default config builds Null enlarger without safelight`() {
        val system = buildSystem(RelaySystemConfigFlat())
        assertTrue(system.enlarger is NullRelayController)
        assertNull(system.safelight)
        assertFalse(system.capabilities.hasSafelight)
    }

    @Test
    fun `unknown enlarger type falls back to NullRelayController`() {
        val system = buildSystem(RelaySystemConfigFlat(enlargerType = "BOGUS"))
        assertTrue(system.enlarger is NullRelayController)
    }

    @Test
    fun `enlarger type mapping creates matching controllers`() {
        assertTrue(buildSystem(RelaySystemConfigFlat(enlargerType = "DEMO")).enlarger is DemoRelayController)
        assertTrue(
            buildSystem(RelaySystemConfigFlat(enlargerType = "TASMOTA", enlargerHost = "10.0.0.2"))
                .enlarger is TasmotaRelayController
        )
        assertTrue(
            buildSystem(
                RelaySystemConfigFlat(
                    enlargerType = "ESPHOME_HTTP",
                    enlargerHost = "10.0.0.3",
                    enlargerEntityId = "switch.enlarger"
                )
            ).enlarger is ESPhomeHttpRelayController
        )
    }

    @Test
    fun `enlarger timing mode maps to canPause`() {
        val timed = buildSystem(
            RelaySystemConfigFlat(
                enlargerType = "TASMOTA",
                enlargerHost = "10.0.0.2",
                enlargerTimingMode = "TIMED_POWER"
            )
        )
        assertFalse(timed.enlarger.canPause)

        val explicit = buildSystem(
            RelaySystemConfigFlat(
                enlargerType = "TASMOTA",
                enlargerHost = "10.0.0.2",
                enlargerTimingMode = "EXPLICIT_ON_OFF"
            )
        )
        assertTrue(explicit.enlarger.canPause)
    }

    @Test
    fun `safelight disabled means no safelight capability`() {
        val system = buildSystem(
            RelaySystemConfigFlat(enlargerType = "TASMOTA", enlargerHost = "10.0.0.2", safelightEnabled = false)
        )
        assertNull(system.safelight)
        assertFalse(system.capabilities.hasSafelight)
    }

    @Test
    fun `same-device safelight inherits enlarger type and is always explicit on-off`() {
        val system = buildSystem(
            RelaySystemConfigFlat(
                enlargerType = "TASMOTA",
                enlargerHost = "10.0.0.2",
                safelightEnabled = true,
                safelightSameDevice = true
            )
        )
        assertNotNull(system.safelight)
        assertTrue(system.capabilities.hasSafelight)
        assertTrue(system.safelight is TasmotaRelayController)
        // Safelight is app-controlled regardless of the enlarger timing mode
        assertEquals(true, system.safelight?.canPause)
    }

    @Test
    fun `independent safelight uses its own type`() {
        val system = buildSystem(
            RelaySystemConfigFlat(
                enlargerType = "TASMOTA",
                enlargerHost = "10.0.0.2",
                safelightEnabled = true,
                safelightSameDevice = false,
                safelightType = "DEMO"
            )
        )
        assertTrue(system.safelight is DemoRelayController)
    }
}
