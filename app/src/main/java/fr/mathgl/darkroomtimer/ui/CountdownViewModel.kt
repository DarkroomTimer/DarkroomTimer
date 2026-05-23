package fr.mathgl.darkroomtimer.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.ForegroundTimerService
import fr.mathgl.darkroomtimer.system.RelayState
import fr.mathgl.darkroomtimer.system.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CountdownUiState(
    val displayTime: String,
    val timerState: TimerState,
    val relayState: RelayState,
    val selectedGrade: ContrastGrade,
    val configuredTimeMs: Long
)

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

    private val timer = CountdownTimer()
    private var tickJob: Job? = null

    private val _uiState = MutableStateFlow(
        CountdownUiState(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayState.INITIAL,
            selectedGrade = ContrastGrade.DEFAULT,
            configuredTimeMs = timer.configuredTimeMs
        )
    )
    val uiState: StateFlow<CountdownUiState> = _uiState.asStateFlow()

    fun start() {
        if (timer.state != TimerState.STOPPED) return
        timer.start()
        _uiState.value = currentState().copy(
            timerState = TimerState.RUNNING,
            relayState = RelayState.RUNNING
        )
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                val remaining = maxOf(0L, timer.remainingMs())
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(remaining),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
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
        _uiState.value = currentState().copy(
            timerState = TimerState.PAUSED,
            relayState = RelayState.IDLE
        )
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        _uiState.value = currentState().copy(
            timerState = TimerState.RUNNING,
            relayState = RelayState.RUNNING
        )
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                val remaining = maxOf(0L, timer.remainingMs())
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(remaining),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
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
        _uiState.value = currentState().copy(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayState.IDLE
        )
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun adjustTime(deltaMs: Long) {
        if (timer.state == TimerState.RUNNING) return
        val newTime = (timer.configuredTimeMs + deltaMs).coerceIn(100L, 999_000L)
        timer.configuredTimeMs = newTime
        _uiState.value = currentState().copy(
            displayTime = CountdownTimer.formatTime(newTime),
            configuredTimeMs = newTime
        )
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.value = currentState().copy(selectedGrade = grade)
    }

    private fun currentState() = _uiState.value

    private fun sendServiceIntent(action: String, remainingMs: Long) {
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
