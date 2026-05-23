package fr.mathgl.darkroomtimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.CountdownTimer
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

class CountdownViewModel : ViewModel() {

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
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(maxOf(0L, timer.remainingMs())),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
                if (ended) {
                    tickJob = null
                    break
                }
            }
        }
    }

    fun pause() {
        if (timer.state != TimerState.RUNNING) return
        timer.pause()
        tickJob?.cancel()
        tickJob = null
        _uiState.value = currentState().copy(
            timerState = TimerState.PAUSED,
            relayState = RelayState.IDLE
        )
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        _uiState.value = currentState().copy(
            timerState = TimerState.RUNNING,
            relayState = RelayState.RUNNING
        )
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(maxOf(0L, timer.remainingMs())),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
                if (ended) { tickJob = null; break }
            }
        }
    }

    fun stop() {
        if (timer.state == TimerState.STOPPED) return
        tickJob?.cancel()
        tickJob = null
        timer.stop()
        _uiState.value = currentState().copy(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayState.IDLE
        )
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

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
