package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*

data class DriverCapabilities(
    val canPause: Boolean,
    val hasSafelight: Boolean
)

/**
 * A simple RelaySystem for standalone mode (no physical hardware).
 * Uses mock controllers that always succeed.
 */
class StandaloneRelaySystem(
    private val scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.MainScope()
) : RelaySystem(
    enlarger = object : RelayController {
        override val state = MutableStateFlow(RelayState.OFF)
        override val connectionState = MutableStateFlow(ConnectionState.Connected)
        override val canPause = true
        override suspend fun connect(): Result<Unit> = Result.success(Unit)
        override suspend fun disconnect() {}
        override suspend fun set(on: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun startTimed(durationMs: Long): Result<Unit> = Result.success(Unit)
    },
    safelight = object : RelayController {
        override val state = MutableStateFlow(RelayState.OFF)
        override val connectionState = MutableStateFlow(ConnectionState.Connected)
        override val canPause = true
        override suspend fun connect(): Result<Unit> = Result.success(Unit)
        override suspend fun disconnect() {}
        override suspend fun set(on: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun startTimed(durationMs: Long): Result<Unit> = Result.success(Unit)
    },
    scope = scope
) {
    override suspend fun startTimedExposure(durationMs: Long): Result<Unit> = Result.success(Unit)
}

open class RelaySystem(
    open val enlarger: RelayController,
    open val safelight: RelayController? = null,
    scope: CoroutineScope
) {
    val capabilities: DriverCapabilities
        get() = DriverCapabilities(
            canPause     = enlarger.canPause,
            hasSafelight = safelight != null
        )

    val connectionState: StateFlow<ConnectionState> = combine(
        enlarger.connectionState,
        safelight?.connectionState ?: flowOf(ConnectionState.Connected)
    ) { enlargerState, safelightState ->
        when {
            enlargerState is ConnectionState.Error -> enlargerState
            safelightState is ConnectionState.Error -> safelightState
            enlargerState is ConnectionState.Connecting || safelightState is ConnectionState.Connecting -> ConnectionState.Connecting
            enlargerState is ConnectionState.Connected && safelightState is ConnectionState.Connected -> ConnectionState.Connected
            else -> ConnectionState.Disconnected
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionState.Disconnected
    )

    val relayStates: StateFlow<RelayStates> = combine(
        enlarger.state,
        safelight?.state ?: flowOf(RelayState.UNKNOWN)
    ) { enlargerState, safelightState ->
        RelayStates(enlarger = enlargerState, safelight = safelightState)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RelayStates.INITIAL
    )

    open suspend fun connect(): Result<Unit> {
        val enlargerRes = enlarger.connect()
        val safelightRes = safelight?.connect() ?: Result.success(Unit)

        return if (enlargerRes.isSuccess && safelightRes.isSuccess) {
            Result.success(Unit)
        } else {
            val errorMsg = listOfNotNull(
                enlargerRes.exceptionOrNull()?.message,
                safelightRes.exceptionOrNull()?.message
            ).joinToString("; ")
            Result.failure(Exception(errorMsg.ifEmpty { "Connection failed" }))
        }
    }

    open suspend fun disconnect() {
        enlarger.disconnect()
        safelight?.disconnect()
    }

    open suspend fun setEnlarger(on: Boolean): Result<Unit> = enlarger.set(on)
    open suspend fun setSafelight(on: Boolean): Result<Unit> =
        safelight?.set(on) ?: Result.success(Unit)

    open suspend fun startTimedExposure(durationMs: Long): Result<Unit> = enlarger.startTimed(durationMs)
}
