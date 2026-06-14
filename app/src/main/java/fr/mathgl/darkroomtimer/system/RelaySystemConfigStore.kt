package fr.mathgl.darkroomtimer.system

import android.util.Log
import fr.mathgl.darkroomtimer.system.drivers.DemoRelayController
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeHttpRelayController
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeNativeRelayController
import fr.mathgl.darkroomtimer.system.drivers.NullRelayController
import fr.mathgl.darkroomtimer.system.drivers.TasmotaRelayController
import java.util.Base64

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
    val enlargerEncryptionKey: String = "",   // ESPHome Native: base64-encoded PSK
    val enlargerTimingMode: String = "TIMED_POWER", // Tasmota: "TIMED_POWER" or "EXPLICIT_ON_OFF"
    // Safelight
    val safelightEnabled: Boolean = false,
    val safelightSameDevice: Boolean = true,  // true = same host/port as enlarger, only channel differs
    val safelightType: String = "NULL",       // only used when safelightSameDevice = false
    val safelightHost: String = "",
    val safelightPort: Int = 80,
    val safelightChannel: Int = 2,            // Tasmota: channel 2 when same device
    val safelightEntityId: String = "",       // ESPHome when independent
    val safelightEncryptionKey: String = "",  // ESPHome Native: base64-encoded PSK
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
        "ESPHOME_NATIVE" -> ESPhomeNativeRelayController(
            host          = enlargerHost,
            port          = enlargerPort,
            entityId      = enlargerEntityId,
            encryptionKey = Base64.getDecoder().decode(enlargerEncryptionKey)
        )
        else           -> {
            if (enlargerType != "NULL") Log.w(TAG, "Unknown enlarger type '$enlargerType', using NullRelayController")
            NullRelayController()
        }
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
                "ESPHOME_NATIVE" -> ESPhomeNativeRelayController(
                    host          = enlargerHost,
                    port          = enlargerPort,
                    entityId      = safelightEntityId,
                    encryptionKey = Base64.getDecoder().decode(enlargerEncryptionKey)
                )
                else -> {
                    if (enlargerType != "NULL") Log.w(TAG, "Unknown enlarger type '$enlargerType' for same-device safelight, using NullRelayController")
                    NullRelayController()
                }
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
            "ESPHOME_NATIVE" -> ESPhomeNativeRelayController(
                host          = safelightHost,
                port          = safelightPort,
                entityId      = safelightEntityId,
                encryptionKey = Base64.getDecoder().decode(safelightEncryptionKey)
            )
            else           -> {
                if (safelightType != "NULL") Log.w(TAG, "Unknown safelight type '$safelightType', using NullRelayController")
                NullRelayController()
            }
        }
    }

    companion object {
        private const val TAG = "RelaySystemConfigFlat"
    }
}
