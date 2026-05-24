package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockRelaySystem(
    val scope: CoroutineScope
) : RelaySystem(
    enlarger = MockRelayController(),
    safelight = MockRelayController(),
    scope = scope
) {
    var startTimedExposureCalled = false
    var setEnlargerCalled = false
    var setSafelightCalled = false

    override suspend fun startTimedExposure(durationMs: Long): Result<Unit> {
        startTimedExposureCalled = true
        return Result.success(Unit)
    }

    override suspend fun setEnlarger(on: Boolean): Result<Unit> {
        setEnlargerCalled = true
        return Result.success(Unit)
    }

    override suspend fun setSafelight(on: Boolean): Result<Unit> {
        setSafelightCalled = true
        return Result.success(Unit)
    }
}

class MockRelayController(
    override val canPause: Boolean = true
) : RelayController {
    override val state: StateFlow<RelayState> = MutableStateFlow(RelayState.UNKNOWN).asStateFlow()
    override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected).asStateFlow()
    override suspend fun connect(): Result<Unit> = Result.success(Unit)
    override suspend fun disconnect() {}
    override suspend fun set(on: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun startTimed(durationMs: Long): Result<Unit> = Result.success(Unit)
}

class MockRelaySystemNoPause(
    val scope: CoroutineScope,
    controller: MockRelayController
) : RelaySystem(
    enlarger = controller,
    safelight = controller,
    scope = scope
) {
    var startTimedExposureCalled = false
    var setEnlargerCalled = false
    var setSafelightCalled = false

    override suspend fun startTimedExposure(durationMs: Long): Result<Unit> {
        startTimedExposureCalled = true
        return Result.success(Unit)
    }

    override suspend fun setEnlarger(on: Boolean): Result<Unit> {
        setEnlargerCalled = true
        return Result.success(Unit)
    }

    override suspend fun setSafelight(on: Boolean): Result<Unit> {
        setSafelightCalled = true
        return Result.success(Unit)
    }
}
