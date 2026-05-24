package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.math.TeststripEngine
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.TeststripSession
import fr.mathgl.darkroomtimer.system.TeststripState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeststripUiState(
    val sessionState: TeststripState,
    val currentPatchIndex: Int,
    val patchCount: Int,
    val patchTimesMs: List<Long>,
    val differentialTimesMs: List<Long>,
    val exposedPatches: Set<Int>,
    val displayTime: String,
    val remainingTimeMs: Long,
    val isSessionComplete: Boolean,
    val baseTimeMs: Long,
    val numerator: Int,
    val denominator: Int,
    val selectedGrade: ContrastGrade
)

class TeststripViewModel(
    application: Application,
    private val relaySystem: RelaySystem
) : AndroidViewModel(application) {

    private var exposureJob: Job? = null
    private var tickJob: Job? = null

    private val _uiState = MutableStateFlow(TeststripUiState(
        sessionState = TeststripState.CONFIGURED,
        currentPatchIndex = -1,
        patchCount = 6,
        patchTimesMs = emptyList(),
        differentialTimesMs = emptyList(),
        exposedPatches = emptySet(),
        displayTime = "00:08.0",
        remainingTimeMs = 8000L,
        isSessionComplete = false,
        baseTimeMs = 8000L,
        numerator = 1,
        denominator = 3,
        selectedGrade = ContrastGrade.DEFAULT
    ))
    val uiState: StateFlow<TeststripUiState> = _uiState.asStateFlow()

    private val engine: TeststripEngine
    private val session: TeststripSession

    init {
        engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        session = TeststripSession(engine = engine)
        updateUiState()
    }

    private fun updateUiState() {
        _uiState.update { it.copy(
            sessionState = session.state,
            currentPatchIndex = session.currentPatchIndex,
            patchTimesMs = engine.patchTimesMs,
            differentialTimesMs = engine.differentialTimesMs,
            exposedPatches = (0 until engine.patchCount).filter { i -> session.isPatchExposed(i) }.toSet(),
            displayTime = session.formatTime(session.remainingTimeMs),
            remainingTimeMs = session.remainingTimeMs,
            isSessionComplete = session.isSessionComplete,
            patchCount = engine.patchCount,
            baseTimeMs = engine.baseTimeMs,
            numerator = engine.numerator,
            denominator = engine.denominator
        ) }
    }

    fun startSession() {
        if (session.state != TeststripState.CONFIGURED && session.state != TeststripState.BETWEEN_PATCHES) return
        session.start()
        startExposure()
        updateUiState()
    }

    fun pause() {
        if (session.state != TeststripState.EXPOSING) return
        exposureJob?.cancel()
        exposureJob = null
        tickJob?.cancel()
        tickJob = null
        session.pause()
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        updateUiState()
    }

    fun resume() {
        if (session.state != TeststripState.PAUSED) return
        session.resume()
        updateUiState()
        startExposure()
    }

    fun finishExposure() {
        if (session.state != TeststripState.EXPOSING) return
        exposureJob?.cancel()
        exposureJob = null
        tickJob?.cancel()
        tickJob = null
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        session.finishExposure()
        updateUiState()
    }

    fun nextPatch() {
        if (session.state != TeststripState.BETWEEN_PATCHES) return
        session.nextPatch()
        updateUiState()
        startExposure()
    }

    fun restartCurrentPatch() {
        if (session.state != TeststripState.BETWEEN_PATCHES) return
        session.restartCurrentPatch()
        updateUiState()
        startExposure()
    }

    fun abandon() {
        exposureJob?.cancel()
        tickJob?.cancel()
        exposureJob = null
        tickJob = null
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        session.abandon()
        updateUiState()
    }

    fun updateBaseTime(newTimeMs: Long) {
        if (session.state != TeststripState.CONFIGURED && session.state != TeststripState.BETWEEN_PATCHES) return
        engine.baseTimeMs = newTimeMs
        updateUiState()
    }

    fun updateStopFraction(numerator: Int, denominator: Int) {
        if (session.state != TeststripState.CONFIGURED && session.state != TeststripState.BETWEEN_PATCHES) return
        engine.numerator = numerator
        engine.denominator = denominator
        updateUiState()
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.update { it.copy(selectedGrade = grade) }
    }

    private fun startExposure() {
        if (session.state != TeststripState.EXPOSING) return
        val durationMs = session.currentExposureTimeMs
        viewModelScope.launch {
            relaySystem.startTimedExposure(durationMs)
        }
        exposureJob = viewModelScope.launch {
            delay(durationMs)
            finishExposure()
        }
        tickJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                updateUiState()
                if (session.state != TeststripState.EXPOSING) break
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exposureJob?.cancel()
        tickJob?.cancel()
    }
}
