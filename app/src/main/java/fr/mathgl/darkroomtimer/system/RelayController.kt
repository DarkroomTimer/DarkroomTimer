package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.flow.StateFlow

interface RelayController {
    val canPause: Boolean
    val state: StateFlow<RelayState>
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun set(on: Boolean): Result<Unit>
    suspend fun startTimed(durationMs: Long): Result<Unit>
}
