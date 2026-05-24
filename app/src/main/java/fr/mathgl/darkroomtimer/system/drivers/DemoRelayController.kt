package fr.mathgl.darkroomtimer.system.drivers

import fr.mathgl.darkroomtimer.system.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DemoRelayController : RelayController {
    override val canPause = true
    override val state = MutableStateFlow<RelayState>(RelayState.OFF)
    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override suspend fun connect(): Result<Unit> {
        connectionState.value = ConnectionState.Connected
        return Result.success(Unit)
    }
    override suspend fun disconnect() {
        connectionState.value = ConnectionState.Disconnected
    }
    override suspend fun set(on: Boolean): Result<Unit> {
        state.value = if (on) RelayState.ON else RelayState.OFF
        return Result.success(Unit)
    }
    override suspend fun startTimed(durationMs: Long): Result<Unit> = set(true)
}
