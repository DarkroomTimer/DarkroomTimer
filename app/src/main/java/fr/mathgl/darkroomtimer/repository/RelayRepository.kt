package fr.mathgl.darkroomtimer.repository

import fr.mathgl.darkroomtimer.system.ConnectionState
import fr.mathgl.darkroomtimer.system.DriverCapabilities
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.RelaySystemConfigFlat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RelayRepository {
    private val scope: CoroutineScope
    private var relaySystem: RelaySystem
    var capabilities: DriverCapabilities
        private set

    // For tests: pre-built RelaySystem
    constructor(relaySystem: RelaySystem) {
        scope = MainScope()
        this.relaySystem = relaySystem
        capabilities = relaySystem.capabilities
        startCollecting()
    }

    // For production: builds RelaySystem with the repo's own scope (no orphaned MainScope)
    constructor(config: RelaySystemConfigFlat) {
        scope = MainScope()
        relaySystem = config.buildRelaySystem(scope)
        capabilities = relaySystem.capabilities
        startCollecting()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _relayStates = MutableStateFlow(RelayStates.INITIAL)

    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    val relayStates: StateFlow<RelayStates> = _relayStates.asStateFlow()

    private var collectConnectionJob: Job? = null
    private var collectRelayJob: Job? = null

    private fun startCollecting() {
        collectConnectionJob?.cancel()
        collectRelayJob?.cancel()
        collectConnectionJob = scope.launch {
            relaySystem.connectionState.collect { _connectionState.value = it }
        }
        collectRelayJob = scope.launch {
            relaySystem.relayStates.collect { _relayStates.value = it }
        }
    }

    fun close() { scope.cancel() }

    suspend fun reloadConfig(newConfig: RelaySystemConfigFlat) {
        relaySystem.disconnect()
        relaySystem = newConfig.buildRelaySystem(scope)
        capabilities = relaySystem.capabilities
        startCollecting()
    }

    suspend fun connect(): Result<Unit> = relaySystem.connect()
    suspend fun disconnect() = relaySystem.disconnect()
    suspend fun setEnlarger(on: Boolean): Result<Unit> = relaySystem.setEnlarger(on)
    suspend fun setSafelight(on: Boolean): Result<Unit> = relaySystem.setSafelight(on)
    suspend fun startTimedExposure(durationMs: Long): Result<Unit> = relaySystem.startTimedExposure(durationMs)
}
