package fr.mathgl.darkroomtimer.ui.exposure

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.audio.AudioSystem
import fr.mathgl.darkroomtimer.audio.createAudioSystem
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.math.IncrementType
import fr.mathgl.darkroomtimer.math.TeststripEngine
import fr.mathgl.darkroomtimer.math.TeststripMode
import fr.mathgl.darkroomtimer.repository.RelayRepository
import fr.mathgl.darkroomtimer.repository.SettingsRepository
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.ConnectionState
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
    val selectedPatchIndex: Int,
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
    val selectedGrade: ContrastGrade,
    val mode: TeststripMode,
    val incrementType: IncrementType,
    val incrementMs: Long,
    val isRelayConnected: Boolean,
    val errorMessage: String? = null
)

private const val TAG = "DT/TeststripVM"

class TeststripViewModel(
    application: Application,
    private val relayRepository: RelayRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private var exposureJob: Job? = null
    private var tickJob: Job? = null
    private var audioSystem: AudioSystem? = null
    private var selectedPatchIndex: Int = 0

    private val _uiState = MutableStateFlow(TeststripUiState(
        sessionState = TeststripState.INIT,
        currentPatchIndex = -1,
        selectedPatchIndex = 0,
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
        selectedGrade = ContrastGrade.DEFAULT,
        mode = TeststripMode.SEPARATE,
        incrementType = IncrementType.F_STOP,
        incrementMs = 0L,
        isRelayConnected = false
    ))
    val uiState: StateFlow<TeststripUiState> = _uiState.asStateFlow()

    private val engine: TeststripEngine
    private val session: TeststripSession

    init {
        audioSystem = createAudioSystem(getApplication())

        var initBaseMs = settingsRepository.teststripBaseMs
        var initNumerator = settingsRepository.teststripStopNumerator
        var initDenominator = settingsRepository.teststripStopDenominator
        var initPatchCount = settingsRepository.teststripPatchCount
        var initMode = runCatching { TeststripMode.valueOf(settingsRepository.teststripMode) }.getOrDefault(TeststripMode.SEPARATE)
        var initIncrementType = runCatching { IncrementType.valueOf(settingsRepository.teststripIncrementType) }.getOrDefault(IncrementType.F_STOP)
        var initIncrementMs = settingsRepository.teststripIncrementMs

        engine = TeststripEngine(
            baseTimeMs = initBaseMs,
            numerator = initNumerator,
            denominator = initDenominator,
            patchCount = initPatchCount,
            mode = initMode,
            incrementType = initIncrementType
        )
        engine.updateIncrementMs(initIncrementMs)
        session = TeststripSession(engine = engine)
        updateUiState()

        viewModelScope.launch {
            relayRepository.connect().onFailure { e ->
                _uiState.update { it.copy(errorMessage = "Connection failed: ${e.message}") }
            }
        }

        viewModelScope.launch {
            relayRepository.connectionState.collect {
                updateUiState()
            }
        }
    }

    private fun updateUiState() {
        _uiState.update { it.copy(
            sessionState = session.state,
            currentPatchIndex = session.currentPatchIndex,
            selectedPatchIndex = selectedPatchIndex,
            patchTimesMs = engine.patchTimesMs,
            differentialTimesMs = engine.differentialTimesMs,
            exposedPatches = (0 until engine.patchCount).filter { i -> session.isPatchExposed(i) }.toSet(),
            displayTime = session.formatTime(session.remainingTimeMs),
            remainingTimeMs = session.remainingTimeMs,
            isSessionComplete = session.isSessionComplete,
            patchCount = engine.patchCount,
            baseTimeMs = engine.baseTimeMs,
            numerator = engine.numerator,
            denominator = engine.denominator,
            mode = engine.mode,
            incrementType = engine.incrementType,
            incrementMs = engine.incrementMs,
            isRelayConnected = relayRepository.connectionState.value is ConnectionState.Connected
        ) }
    }

    fun startSession() {
        if (session.state != TeststripState.INIT && session.state != TeststripState.BETWEEN_PATCHES) return
        Log.d(TAG, "startSession: patch ${session.currentPatchIndex + 1}")
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
        selectedPatchIndex = session.currentPatchIndex
        viewModelScope.launch { shutOffRelays("pause") }
        audioSystem?.pause()
        updateUiState()
    }

    fun resume() {
        if (session.state != TeststripState.PAUSED) return
        session.resume()
        updateUiState()
        audioSystem?.resume()
        startExposure()
    }

    fun finishExposure() {
        if (session.state != TeststripState.EXPOSING) return
        Log.d(TAG, "finishExposure: patch ${session.currentPatchIndex}")
        exposureJob?.cancel()
        exposureJob = null
        tickJob?.cancel()
        tickJob = null
        viewModelScope.launch { shutOffRelays("finish exposure") }
        audioSystem?.stopTeststripPatch()
        session.finishExposure()
        selectedPatchIndex = (session.currentPatchIndex + 1) % engine.patchCount

        if (session.isSessionComplete) {
            audioSystem?.stopTeststripSession()
        }
        updateUiState()
    }

    fun nextPatch() {
        if (session.state != TeststripState.BETWEEN_PATCHES) return
        Log.d(TAG, "nextPatch: → patch $selectedPatchIndex")
        session.nextPatch(selectedPatchIndex)
        updateUiState()
        startExposure()
    }

    fun selectPreviousPatch() {
        if (session.state != TeststripState.BETWEEN_PATCHES && session.state != TeststripState.PAUSED) return
        selectedPatchIndex = (selectedPatchIndex - 1 + engine.patchCount) % engine.patchCount
        updateUiState()
    }

    fun selectNextPatch() {
        if (session.state != TeststripState.BETWEEN_PATCHES && session.state != TeststripState.PAUSED) return
        selectedPatchIndex = (selectedPatchIndex + 1) % engine.patchCount
        updateUiState()
    }

    fun goFromPaused() {
        if (session.state != TeststripState.PAUSED) return
        session.skipToPatch(selectedPatchIndex)
        updateUiState()
        audioSystem?.stopTeststripPatch()
        startExposure()
    }

    fun abandon() {
        exposureJob?.cancel()
        tickJob?.cancel()
        exposureJob = null
        tickJob = null
        viewModelScope.launch { shutOffRelays("abandon") }
        audioSystem?.stop()
        session.abandon()
        selectedPatchIndex = 0
        updateUiState()
    }

    fun updateBaseTime(newTimeMs: Long) {
        if (session.state != TeststripState.INIT && session.state != TeststripState.BETWEEN_PATCHES) return
        engine.updateBaseTime(newTimeMs)
        settingsRepository.teststripBaseMs = newTimeMs
        updateUiState()
    }

    fun updateStopFraction(numerator: Int, denominator: Int) {
        if (session.state != TeststripState.INIT && session.state != TeststripState.BETWEEN_PATCHES) return
        engine.updateStopFraction(numerator, denominator)
        settingsRepository.teststripStopNumerator = numerator
        settingsRepository.teststripStopDenominator = denominator
        updateUiState()
    }

    fun updatePatchCount(count: Int) {
        if (session.state != TeststripState.INIT) return
        engine.updatePatchCount(count)
        settingsRepository.teststripPatchCount = count
        updateUiState()
    }

    fun updateMode(mode: TeststripMode) {
        if (session.state != TeststripState.INIT) return
        engine.updateMode(mode)
        settingsRepository.teststripMode = mode.name
        updateUiState()
    }

    fun updateIncrementType(type: IncrementType) {
        if (session.state != TeststripState.INIT) return
        engine.updateIncrementType(type)
        settingsRepository.teststripIncrementType = type.name
        updateUiState()
    }

    fun updateIncrementMs(ms: Long) {
        if (session.state != TeststripState.INIT) return
        engine.updateIncrementMs(ms)
        settingsRepository.teststripIncrementMs = ms
        updateUiState()
    }

    fun adjustIncrement(delta: Int) {
        if (session.state != TeststripState.INIT) return
        engine.adjustIncrement(delta)
        when (engine.incrementType) {
            IncrementType.F_STOP -> settingsRepository.teststripStopNumerator = engine.numerator
            IncrementType.SECONDS -> settingsRepository.teststripIncrementMs = engine.incrementMs
        }
        updateUiState()
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.update { it.copy(selectedGrade = grade) }
    }

    private suspend fun shutOffRelays(errorContext: String) {
        val r1 = relayRepository.setEnlarger(false)
        val r2 = relayRepository.setSafelight(false)
        if (!r1.isSuccess || !r2.isSuccess) {
            Log.e(TAG, "shutOffRelays ($errorContext): enlarger=${r1.exceptionOrNull()?.message} safelight=${r2.exceptionOrNull()?.message}")
            _uiState.update { it.copy(errorMessage = "Failed to shut off relays ($errorContext)") }
        }
    }

    private fun startExposure() {
        if (session.state != TeststripState.EXPOSING) return
        val durationMs = session.remainingTimeMs
        Log.d(TAG, "startExposure: patch ${session.currentPatchIndex} duration=${durationMs}ms")
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            relayRepository.startTimedExposure(durationMs).onFailure { e ->
                Log.e(TAG, "startExposure: relay command failed — ${e.message}")
                _uiState.update { it.copy(errorMessage = "Hardware Error: ${e.message}") }
                session.pause()
                updateUiState()
            }
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
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try { relayRepository.disconnect() } catch (e: Exception) { /* ignore */ }
        }
        audioSystem?.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>
            ): T {
                throw IllegalStateException(
                    "TeststripViewModel requires CreationExtras. Use factory with create(modelClass, extras) override."
                )
            }

            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = extras[
                    ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                ] as? Application
                    ?: throw IllegalStateException("Application not available")
                val prefs = fr.mathgl.darkroomtimer.storage.PreferenceManager.getInstance(application)
                val settingsRepo = SettingsRepository(application)
                val relaySystem = prefs.relaySystemConfig.buildRelaySystem(kotlinx.coroutines.MainScope())
                val relayRepo = RelayRepository(relaySystem)
                return TeststripViewModel(
                    application,
                    relayRepo,
                    settingsRepo
                ) as T
            }
        }
    }
}
