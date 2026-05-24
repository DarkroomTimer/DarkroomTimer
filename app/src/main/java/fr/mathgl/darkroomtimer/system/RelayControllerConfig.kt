package fr.mathgl.darkroomtimer.system

sealed class RelayControllerConfig {
    object Null : RelayControllerConfig()
    object Demo : RelayControllerConfig()
    data class Tasmota(
        val host: String,
        val port: Int = 80,
        val username: String = "",
        val password: String = "",
        val channel: Int = 1,
        val timingMode: TimingMode = TimingMode.TIMED_POWER
    ) : RelayControllerConfig()
    data class ESPhomeHttp(
        val host: String,
        val port: Int = 80,
        val entityId: String
    ) : RelayControllerConfig()
}
