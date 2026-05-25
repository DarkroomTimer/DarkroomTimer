package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.system.drivers.DemoRelayController
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeHttpRelayController
import fr.mathgl.darkroomtimer.system.drivers.NullRelayController
import fr.mathgl.darkroomtimer.system.drivers.TasmotaRelayController

/**
 * Flat, JSON-serializable representation of the full relay system configuration.
 * Stored as a single JSON string in SharedPreferences.
 */
data class RelaySystemConfigFlat(
    // Enlarger
    val enlargerType: String = "NULL",        // "NULL", "DEMO", "TASMOTA", "ESPHOME_HTTP"
    val enlargerHost: String = "",
    val enlargerPort: Int = 80,
    val enlargerChannel: Int = 1,             // Tasmota: 1 or 2
    val enlargerUsername: String = "",
    val enlargerPassword: String = "",
    val enlargerEntityId: String = "",        // ESPHome
    val enlargerTimingMode: String = "TIMED_POWER", // Tasmota: "TIMED_POWER" or "EXPLICIT_ON_OFF"
    // Safelight
    val safelightEnabled: Boolean = false,
    val safelightSameDevice: Boolean = true,  // true = same host/port as enlarger, only channel differs
    val safelightType: String = "NULL",       // only used when safelightSameDevice = false
    val safelightHost: String = "",
    val safelightPort: Int = 80,
    val safelightChannel: Int = 2,            // Tasmota: channel 2 when same device
    val safelightEntityId: String = "",       // ESPHome when independent
    val safelightUsername: String = "",
    val safelightPassword: String = ""
) {
    fun buildRelaySystem(scope: kotlinx.coroutines.CoroutineScope): RelaySystem {
        val enlarger = buildEnlarger()
        val safelight = if (safelightEnabled) buildSafelight() else null
        return RelaySystem(enlarger = enlarger, safelight = safelight, scope = scope)
    }

    private fun buildEnlarger(): RelayController = when (enlargerType) {
        "DEMO"         -> DemoRelayController()
        "TASMOTA"      -> TasmotaRelayController(
            host       = enlargerHost,
            port       = enlargerPort,
            channel    = enlargerChannel,
            username   = enlargerUsername.ifBlank { null },
            password   = enlargerPassword.ifBlank { null },
            timingMode = if (enlargerTimingMode == "EXPLICIT_ON_OFF") TimingMode.EXPLICIT_ON_OFF
                         else TimingMode.TIMED_POWER
        )
        "ESPHOME_HTTP" -> ESPhomeHttpRelayController(
            host     = enlargerHost,
            port     = enlargerPort,
            entityId = enlargerEntityId
        )
        else           -> NullRelayController()  // "NULL" and fallback
    }

    private fun buildSafelight(): RelayController {
        if (safelightSameDevice) {
            // Same device as enlarger: inherit host/port, but different channel
            return when (enlargerType) {
                "DEMO" -> DemoRelayController()
                "TASMOTA" -> TasmotaRelayController(
                    host       = enlargerHost,
                    port       = enlargerPort,
                    channel    = safelightChannel,
                    username   = enlargerUsername.ifBlank { null },
                    password   = enlargerPassword.ifBlank { null },
                    timingMode = TimingMode.EXPLICIT_ON_OFF  // safelight always app-controlled
                )
                "ESPHOME_HTTP" -> ESPhomeHttpRelayController(
                    host     = enlargerHost,
                    port     = enlargerPort,
                    entityId = safelightEntityId
                )
                else -> NullRelayController()
            }
        }
        return when (safelightType) {
            "DEMO"         -> DemoRelayController()
            "TASMOTA"      -> TasmotaRelayController(
                host       = safelightHost,
                port       = safelightPort,
                channel    = safelightChannel,
                username   = safelightUsername.ifBlank { null },
                password   = safelightPassword.ifBlank { null },
                timingMode = TimingMode.EXPLICIT_ON_OFF
            )
            "ESPHOME_HTTP" -> ESPhomeHttpRelayController(
                host     = safelightHost,
                port     = safelightPort,
                entityId = safelightEntityId
            )
            else           -> NullRelayController()
        }
    }
}
