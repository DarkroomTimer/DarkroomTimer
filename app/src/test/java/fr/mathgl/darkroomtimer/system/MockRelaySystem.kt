package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockRelaySystem(
    scope: CoroutineScope,
    canPause: Boolean = true,
    var shouldFailRelays: Boolean = false
) : RelaySystem(MockRelayController(canPause), MockRelayController(canPause), scope) {

    val setEnlargerCalls = mutableListOf<Boolean>()
    val setSafelightCalls = mutableListOf<Boolean>()
    var startTimedExposureCallCount = 0

    override suspend fun startTimedExposure(durationMs: Long): Result<Unit> {
        if (shouldFailRelays) return Result.failure(Exception("Mock relay failure"))
        startTimedExposureCallCount++
        return Result.success(Unit)
    }

    override suspend fun setEnlarger(on: Boolean): Result<Unit> {
        if (shouldFailRelays) return Result.failure(Exception("Mock relay failure"))
        setEnlargerCalls.add(on)
        return Result.success(Unit)
    }

    override suspend fun setSafelight(on: Boolean): Result<Unit> {
        if (shouldFailRelays) return Result.failure(Exception("Mock relay failure"))
        setSafelightCalls.add(on)
        return Result.success(Unit)
    }
}

class MockRelayController(
    override val canPause: Boolean = true
) : RelayController {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<RelayState> = MutableStateFlow(RelayState.UNKNOWN).asStateFlow()
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(): Result<Unit> {
        _connectionState.value = ConnectionState.Connected
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun set(on: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun startTimed(durationMs: Long): Result<Unit> = Result.success(Unit)
}
