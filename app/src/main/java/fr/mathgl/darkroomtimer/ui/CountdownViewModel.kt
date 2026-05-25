package fr.mathgl.darkroomtimer.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.audio.AudioPreferences
import fr.mathgl.darkroomtimer.audio.AudioSystem
import fr.mathgl.darkroomtimer.audio.ToneGeneratorAudioEngine
import fr.mathgl.darkroomtimer.math.BurnDodgeEntry
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.math.FStopMath
import fr.mathgl.darkroomtimer.storage.PreferenceManager
import fr.mathgl.darkroomtimer.system.BurnDodgeManager
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.ForegroundTimerService
import fr.mathgl.darkroomtimer.system.ConnectionState
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.RelaySystemConfigFlat
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
    val configuredTimeMs: Long,
    val burnDodgeEntries: List<BurnDodgeEntry>,
    val burnDodgeVisible: Boolean,
    val maxEntriesReached: Boolean,
    val showTimeEditor: Boolean = false,
    val enlargerOverride: Boolean = false,
    val safelightOverride: Boolean = false,
    val relayType: String = "NULL",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val errorMessage: String? = null,
    val fStopCorrectionNumerator: Int = 0,
    val fStopCorrectionDenominator: Int = 1
)

open class CountdownViewModel(
    application: Application,
    private val relaySystemFactory: (kotlinx.coroutines.CoroutineScope) -> RelaySystem,
    private val relayType: String = "NULL",
    private val timer: CountdownTimer = CountdownTimer()
) : AndroidViewModel(application) {

    private val burnDodgeManager = BurnDodgeManager()
    private lateinit var relaySystem: RelaySystem
    private var audioSystem: AudioSystem? = null
    private var tickJob: Job? = null
    private var baseTimeMs: Long = timer.configuredTimeMs

    private fun calculatedTimeMs(): Long {
        val state = _uiState.value
        if (state.fStopCorrectionNumerator == 0) return baseTimeMs
        return FStopMath
            .adjustTime(baseTimeMs, state.fStopCorrectionNumerator, state.fStopCorrectionDenominator, 1)
            .coerceIn(100L, 999_000L)
    }

    fun applyFStopDelta(numerator: Int, denominator: Int) { /* implemented in Task 3 */ }

    private val _uiState = MutableStateFlow(
        CountdownUiState(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayStates.INITIAL,
            selectedGrade = ContrastGrade.DEFAULT,
            configuredTimeMs = timer.configuredTimeMs,
            burnDodgeEntries = emptyList(),
            burnDodgeVisible = false,
            maxEntriesReached = false
        )
    )
    val uiState: StateFlow<CountdownUiState> = _uiState.asStateFlow()

    init {
        relaySystem = relaySystemFactory(viewModelScope)
        _uiState.update { it.copy(relayType = relayType) }

        // Initialize audio from preferences
        try {
            val context = getApplication<Application>()
            val preferenceManager = PreferenceManager.getInstance(context)
            val audioPreferences = AudioPreferences(preferenceManager.prefs)
            val audioEngine = ToneGeneratorAudioEngine(audioPreferences.buzzerVolume)
            audioSystem = AudioSystem(audioEngine, audioPreferences, audioPreferences.buzzerVolume)
        } catch (e: Exception) {
            // audio unavailable in test environment
        }

        // Load defaults from preferences
        try {
            val context = getApplication<Application>()
            val prefs = PreferenceManager.getInstance(context)
            timer.configuredTimeMs = prefs.defaultExposureMs
            baseTimeMs = timer.configuredTimeMs
            _uiState.update { it.copy(
                displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
                configuredTimeMs = timer.configuredTimeMs,
                selectedGrade = prefs.defaultContrastGrade
            ) }
        } catch (e: Exception) {
            // prefs unavailable in test environment, keep hardcoded defaults
        }

        viewModelScope.launch {
            relaySystem.relayStates.collect { relayState ->
                _uiState.update { it.copy(relayState = relayState) }
            }
        }

        viewModelScope.launch {
            relaySystem.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
            }
        }

        // Connect relay if not Null/Demo (network drivers need connection)
        viewModelScope.launch {
            relaySystem.connect().onFailure { e ->
                _uiState.update { it.copy(errorMessage = "Connection failed: ${e.message}") }
            }
        }
    }

    fun start() {
        if (timer.state != TimerState.STOPPED) return
        timer.configuredTimeMs = calculatedTimeMs()       // use calculated time
        timer.start()

        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            val result = if (relaySystem.capabilities.canPause) {
                relaySystem.startTimedExposure(timer.configuredTimeMs)
            } else {
                val res1 = relaySystem.setEnlarger(true)
                val res2 = relaySystem.setSafelight(true)
                if (res1.isSuccess && res2.isSuccess) Result.success(Unit) else Result.failure(Exception("Relay activation failed"))
            }

            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = "Hardware Error: ${result.exceptionOrNull()?.message}") }
                timer.stop()
            }
        }

        audioSystem?.startExposure()
        _uiState.update { it.copy(timerState = TimerState.RUNNING, enlargerOverride = false, safelightOverride = false) }
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = launchTickJob()
    }

    fun pause() {
        if (timer.state != TimerState.RUNNING) return
        timer.pause()
        tickJob?.cancel(); tickJob = null
        viewModelScope.launch {
            val res1 = relaySystem.setEnlarger(false)
            val res2 = relaySystem.setSafelight(false)
            if (!res1.isSuccess || !res2.isSuccess) {
                _uiState.update { it.copy(errorMessage = "Pause failed: Hardware did not respond") }
            }
        }
        audioSystem?.pause()
        _uiState.update { it.copy(
            timerState = TimerState.PAUSED,
            displayTime = CountdownTimer.formatTime(timer.remainingMs())
        ) }
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        viewModelScope.launch {
            val res1 = relaySystem.setEnlarger(true)
            val res2 = relaySystem.setSafelight(true)
            if (!res1.isSuccess || !res2.isSuccess) {
                _uiState.update { it.copy(errorMessage = "Resume failed: Hardware did not respond") }
                timer.pause()
            }
        }
        audioSystem?.resume()
        _uiState.update { it.copy(timerState = TimerState.RUNNING) }
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = launchTickJob()
    }

    private fun launchTickJob(): Job = viewModelScope.launch {
        while (true) {
            val ended = timer.tick()
            val remaining = maxOf(0L, timer.remainingMs())
            if (ended) timer.configuredTimeMs = baseTimeMs           // restore base on natural end
            _uiState.update { it.copy(
                displayTime = if (ended) CountdownTimer.formatTime(calculatedTimeMs())
                              else CountdownTimer.formatTime(remaining),
                timerState = timer.state,
                enlargerOverride = if (ended) false else it.enlargerOverride,
                safelightOverride = if (ended) false else it.safelightOverride
            ) }
            sendServiceIntent(
                if (ended) ForegroundTimerService.ACTION_STOP else ForegroundTimerService.ACTION_UPDATE,
                remaining
            )
            if (ended) {
                viewModelScope.launch {
                    val res1 = relaySystem.setEnlarger(false)
                    val res2 = relaySystem.setSafelight(false)
                    if (!res1.isSuccess || !res2.isSuccess) {
                        _uiState.update { it.copy(errorMessage = "CRITICAL: Failed to shut off relays on timer end!") }
                    }
                }
                audioSystem?.stopExposure()
                tickJob = null
                break
            }
            delay(50L)
        }
    }

    fun stop() {
        if (timer.state == TimerState.STOPPED) return
        val wasPaused = timer.state == TimerState.PAUSED
        val timerCompletedNaturally = timer.state == TimerState.RUNNING
        tickJob?.cancel(); tickJob = null
        timer.stop()
        timer.configuredTimeMs = baseTimeMs                           // restore base
        viewModelScope.launch {
            val res1 = relaySystem.setEnlarger(false)
            val res2 = relaySystem.setSafelight(false)
            if (!res1.isSuccess || !res2.isSuccess) {
                _uiState.update { it.copy(errorMessage = "CRITICAL: Failed to shut off relays!") }
            }
        }
        if (timerCompletedNaturally) {
            audioSystem?.stopExposure()
        }
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(calculatedTimeMs()),  // use calculated time
            timerState = TimerState.STOPPED,
            enlargerOverride = false,
            safelightOverride = false
        ) }
        if (!wasPaused) {
            sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
        }
    }

    fun adjustTime(deltaMs: Long) {
        if (timer.state == TimerState.RUNNING) return
        if (timer.state == TimerState.STOPPED) {
            val newBase = (baseTimeMs + deltaMs).coerceIn(100L, 999_000L)
            baseTimeMs = newBase
            val calc = calculatedTimeMs()
            timer.configuredTimeMs = calc
            _uiState.update { it.copy(
                displayTime = CountdownTimer.formatTime(calc),
                configuredTimeMs = newBase
            ) }
        } else {
            // PAUSED: fine-tune remaining time; does not affect baseTimeMs or correction
            val newTime = (timer.configuredTimeMs + deltaMs).coerceIn(100L, 999_000L)
            timer.configuredTimeMs = newTime
            _uiState.update { it.copy(
                displayTime = CountdownTimer.formatTime(timer.remainingMs())
            ) }
        }
    }

    fun openTimeEditor() {
        if (_uiState.value.timerState == TimerState.RUNNING) return
        _uiState.update { it.copy(showTimeEditor = true) }
    }

    fun closeTimeEditor() {
        _uiState.update { it.copy(showTimeEditor = false) }
    }

    fun setTimeFromInput(minutes: Int, seconds: Int, tenths: Int) {
        val newBase = (minutes * 60_000L + seconds * 1_000L + tenths * 100L).coerceIn(100L, 999_000L)
        baseTimeMs = newBase
        timer.configuredTimeMs = newBase
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(newBase),
            configuredTimeMs = newBase,
            fStopCorrectionNumerator = 0,
            fStopCorrectionDenominator = 1,
            showTimeEditor = false
        ) }
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.update { it.copy(selectedGrade = grade) }
    }

    fun toggleEnlargerOverride() {
        if (_uiState.value.timerState == TimerState.RUNNING) return
        val newOverride = !_uiState.value.enlargerOverride
        viewModelScope.launch { relaySystem.setEnlarger(newOverride) }
        _uiState.update { it.copy(enlargerOverride = newOverride) }
    }

    fun toggleSafelightOverride() {
        if (_uiState.value.timerState == TimerState.RUNNING) return
        val newOverride = !_uiState.value.safelightOverride
        viewModelScope.launch { relaySystem.setSafelight(newOverride) }
        _uiState.update { it.copy(safelightOverride = newOverride) }
    }

    fun addBurnDodgeEntry(
        label: String,
        type: BurnDodgeType,
        numerator: Int,
        denominator: Int,
        contrastGrade: ContrastGrade
    ) {
        burnDodgeManager.addEntry(label, type, numerator, denominator, contrastGrade)
        updateBurnDodgeState()
    }

    fun removeBurnDodgeEntry(id: Int) {
        burnDodgeManager.removeEntry(id)
        updateBurnDodgeState()
    }

    fun clearBurnDodgeEntries() {
        burnDodgeManager.clear()
        updateBurnDodgeState()
    }

    fun toggleBurnDodgePanel() {
        _uiState.update { it.copy(burnDodgeVisible = !it.burnDodgeVisible) }
    }

    private fun updateBurnDodgeState() {
        _uiState.update { it.copy(
            burnDodgeEntries = burnDodgeManager.entriesList,
            maxEntriesReached = burnDodgeManager.isFull
        ) }
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
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try { relaySystem.disconnect() } catch (e: Exception) { /* ignore */ }
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
                    "CountdownViewModel requires CreationExtras. Use factory with create(modelClass, extras) override."
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
                return CountdownViewModel(
                    application,
                    prefs.relaySystemConfig::buildRelaySystem,
                    prefs.relaySystemConfig.enlargerType,
                    CountdownTimer()
                ) as T
            }
        }
    }
}
