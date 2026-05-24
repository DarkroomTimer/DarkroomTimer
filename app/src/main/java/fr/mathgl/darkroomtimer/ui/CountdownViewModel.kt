package fr.mathgl.darkroomtimer.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.ForegroundTimerService
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountdownUiState(
    val displayTime: String,
    val timerState: TimerState,
    val relayState: RelayStates,
    val selectedGrade: ContrastGrade,
    val configuredTimeMs: Long
)

open class CountdownViewModel(
    application: Application,
    private val relaySystem: RelaySystem
) : AndroidViewModel(application) {

    private val timer = CountdownTimer()
    private var tickJob: Job? = null

    private val _uiState = MutableStateFlow(
        CountdownUiState(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayStates.INITIAL,
            selectedGrade = ContrastGrade.DEFAULT,
            configuredTimeMs = timer.configuredTimeMs
        )
    )
    val uiState: StateFlow<CountdownUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            relaySystem.relayStates.collect { relayState ->
                _uiState.update { it.copy(relayState = relayState) }
            }
        }
    }

    fun start() {
        if (timer.state != TimerState.STOPPED) return
        timer.start()

        viewModelScope.launch {
            if (relaySystem.capabilities.canPause) {
                relaySystem.startTimedExposure(timer.configuredTimeMs)
            } else {
                relaySystem.setEnlarger(true)
                relaySystem.setSafelight(true)
            }
        }

        // IMPORTANT: remove the hardcoded relayState update here, as it's now handled by the flow
        _uiState.update { it.copy(
            timerState = TimerState.RUNNING
        ) }
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                val remaining = maxOf(0L, timer.remainingMs())
                _uiState.update { it.copy(
                    displayTime = CountdownTimer.formatTime(remaining),
                    timerState = timer.state
                ) }
                sendServiceIntent(
                    if (ended) ForegroundTimerService.ACTION_STOP else ForegroundTimerService.ACTION_UPDATE,
                    remaining
                )
                if (ended) { tickJob = null; break }
            }
        }
    }

    fun pause() {
        if (timer.state != TimerState.RUNNING) return
        timer.pause()
        tickJob?.cancel(); tickJob = null
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        _uiState.update { it.copy(
            timerState = TimerState.PAUSED
        ) }
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        viewModelScope.launch {
            if (relaySystem.capabilities.canPause) {
                // For timed power drivers, resuming is complex.
                // Simple implementation: trigger ON again.
                relaySystem.setEnlarger(true)
            } else {
                relaySystem.setEnlarger(true)
                relaySystem.setSafelight(true)
            }
        }
        _uiState.update { it.copy(
            timerState = TimerState.RUNNING
        ) }
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                val remaining = maxOf(0L, timer.remainingMs())
                _uiState.update { it.copy(
                    displayTime = CountdownTimer.formatTime(remaining),
                    timerState = timer.state
                ) }
                sendServiceIntent(
                    if (ended) ForegroundTimerService.ACTION_STOP else ForegroundTimerService.ACTION_UPDATE,
                    remaining
                )
                if (ended) { tickJob = null; break }
            }
        }
    }

    fun stop() {
        if (timer.state == TimerState.STOPPED) return
        tickJob?.cancel(); tickJob = null
        timer.stop()
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED
        ) }
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun adjustTime(deltaMs: Long) {
        if (timer.state == TimerState.RUNNING) return
        val newTime = (timer.configuredTimeMs + deltaMs).coerceIn(100L, 999_000L)
        timer.configuredTimeMs = newTime
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(newTime),
            configuredTimeMs = newTime
        ) }
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.update { it.copy(selectedGrade = grade) }
    }

    private fun currentState() = _uiState.value

    open fun sendServiceIntent(action: String, remainingMs: Long) {
        val intent = Intent(getApplication(), ForegroundTimerService::class.java).apply {
            this.action = action
            putExtra(ForegroundTimerService.EXTRA_REMAINING_MS, remainingMs)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
