package fr.mathgl.darkroomtimer.repository

import fr.mathgl.darkroomtimer.system.ConnectionState
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.RelaySystem
import kotlinx.coroutines.flow.StateFlow

class RelayRepository(
    private val relaySystem: RelaySystem
) {
    val connectionState: StateFlow<ConnectionState> = relaySystem.connectionState
    val relayStates: StateFlow<RelayStates> = relaySystem.relayStates

    val capabilities = relaySystem.capabilities

    suspend fun connect(): Result<Unit> = relaySystem.connect()
    suspend fun disconnect() = relaySystem.disconnect()
    suspend fun setEnlarger(on: Boolean): Result<Unit> = relaySystem.setEnlarger(on)
    suspend fun setSafelight(on: Boolean): Result<Unit> = relaySystem.setSafelight(on)
    suspend fun startTimedExposure(durationMs: Long): Result<Unit> = relaySystem.startTimedExposure(durationMs)
}
