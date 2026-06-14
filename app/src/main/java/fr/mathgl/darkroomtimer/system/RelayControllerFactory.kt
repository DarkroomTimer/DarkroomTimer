package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.system.drivers.*
import kotlinx.coroutines.CoroutineDispatcher
import java.util.Base64

object RelayControllerFactory {
    fun create(config: RelayControllerConfig, dispatcher: CoroutineDispatcher): RelayController =
        when (config) {
            is RelayControllerConfig.Null -> NullRelayController()
            is RelayControllerConfig.Demo -> DemoRelayController()
            is RelayControllerConfig.Tasmota -> TasmotaRelayController(
                host = config.host,
                port = config.port,
                channel = config.channel,
                // Blank credentials must not produce a Basic-Auth header
                username = config.username.ifBlank { null },
                password = config.password.ifBlank { null },
                timingMode = config.timingMode
            )
            is RelayControllerConfig.ESPhomeHttp -> ESPhomeHttpRelayController(
                host = config.host,
                port = config.port,
                entityId = config.entityId
            )
            is RelayControllerConfig.ESPhomeNative -> ESPhomeNativeRelayController(
                host = config.host,
                port = config.port,
                entityId = config.entityId,
                encryptionKey = Base64.getDecoder().decode(config.encryptionKey)
            )
        }
}
