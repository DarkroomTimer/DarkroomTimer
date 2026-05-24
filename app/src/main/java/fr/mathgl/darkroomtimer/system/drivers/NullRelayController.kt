package fr.mathgl.darkroomtimer.system.drivers

import fr.mathgl.darkroomtimer.system.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NullRelayController : RelayController {
    override val canPause = true
    override val state = MutableStateFlow(RelayState.OFF)
    override val connectionState = MutableStateFlow(ConnectionState.Connected)

    override suspend fun connect(): Result<Unit> = Result.success(Unit)
    override suspend fun disconnect() {}
    override suspend fun set(on: Boolean): Result<Unit> {
        state.value = if (on) RelayState.ON else RelayState.OFF
        return Result.success(Unit)
    }
    override suspend fun startTimed(durationMs: Long): Result<Unit> = set(true)
}
