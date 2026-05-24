package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

data class DriverCapabilities(
    val canPause: Boolean,
    val hasSafelight: Boolean
)

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

    val connectionState: StateFlow<ConnectionState> = enlarger.connectionState

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

    open suspend fun connect() {
        enlarger.connect()
        safelight?.connect()
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
