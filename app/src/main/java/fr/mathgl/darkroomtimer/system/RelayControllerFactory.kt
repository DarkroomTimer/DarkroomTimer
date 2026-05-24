package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.system.drivers.*
import kotlinx.coroutines.CoroutineDispatcher

object RelayControllerFactory {
    fun create(config: RelayControllerConfig, dispatcher: CoroutineDispatcher): RelayController =
        when (config) {
            is RelayControllerConfig.Null -> NullRelayController()
            is RelayControllerConfig.Demo -> DemoRelayController()
            is RelayControllerConfig.Tasmota -> TasmotaRelayController(
                host = config.host,
                port = config.port,
                channel = config.channel,
                username = config.username,
                password = config.password,
                timingMode = config.timingMode
            )
            is RelayControllerConfig.ESPhomeHttp -> ESPhomeHttpRelayController(
                host = config.host,
                port = config.port,
                entityId = config.entityId
            )
        }
}
