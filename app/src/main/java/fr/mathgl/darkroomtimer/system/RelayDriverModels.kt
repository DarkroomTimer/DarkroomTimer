package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.flow.StateFlow

enum class RelayState { ON, OFF, UNKNOWN }

enum class TimingMode { TIMED_POWER, EXPLICIT_ON_OFF }

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting   : ConnectionState()
    object Connected    : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class RelayStates(
    val enlarger: RelayState  = RelayState.UNKNOWN,
    val safelight: RelayState = RelayState.UNKNOWN
) {
    companion object {
        val INITIAL = RelayStates(enlarger = RelayState.OFF, safelight = RelayState.ON)
        val RUNNING = RelayStates(enlarger = RelayState.ON, safelight = RelayState.OFF)
        val IDLE    = RelayStates(enlarger = RelayState.OFF, safelight = RelayState.OFF)
    }
}
