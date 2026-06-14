package fr.mathgl.darkroomtimer.ui.exposure

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.audio.AudioSystem
import fr.mathgl.darkroomtimer.audio.createAudioSystem
import fr.mathgl.darkroomtimer.math.BurnDodgeEntry
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.math.FStopMath
import fr.mathgl.darkroomtimer.repository.RelayRepository
import fr.mathgl.darkroomtimer.repository.SettingsRepository
import fr.mathgl.darkroomtimer.ui.exposure.ConnectionTint
import fr.mathgl.darkroomtimer.system.BurnDodgeManager
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.ForegroundTimerService
import fr.mathgl.darkroomtimer.system.ConnectionState
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.TimerState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class CountdownUiState(
    val displayTime: String,
    val displayTimeMs: Long,
    val timerState: TimerState,
    val relayState: RelayStates,
    val selectedGrade: ContrastGrade,
    val configuredTimeMs: Long,
    val baseTimeMs: Long,
    val burnDodgeEntries: List<BurnDodgeEntry>,
    val burnDodgeVisible: Boolean,
    val maxEntriesReached: Boolean,
    val enlargerOverride: Boolean = false,
    val safelightOverride: Boolean = false,
    val relayType: String = "NULL",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val connectionTint: ConnectionTint = ConnectionTint.DIM,
    val errorMessage: String? = null,
    val fStopCorrectionNumerator: Int = 0,
    val fStopCorrectionDenominator: Int = 1,
    val isMetronomeEnabled: Boolean = false
)

private const val TAG = "DT/CountdownVM"

open class CountdownViewModel(
    application: Application,
    private val relayRepository: RelayRepository,
    private val settingsRepository: SettingsRepository,
    private val relayType: String = "NULL",
    private val timer: CountdownTimer = CountdownTimer()
) : AndroidViewModel(application) {

    private val burnDodgeManager = BurnDodgeManager()
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

    fun applyFStopDelta(numerator: Int, denominator: Int) {
        if (timer.state != TimerState.STOPPED) return
        val currentN = _uiState.value.fStopCorrectionNumerator
        val currentD = _uiState.value.fStopCorrectionDenominator
        val rawN = currentN * denominator + numerator * currentD
        val rawD = currentD * denominator
        val (simplN, simplD) = FStopMath.simplify(rawN, rawD)
        val calc = FStopMath.adjustTime(baseTimeMs, simplN, simplD, 1)
        if (calc !in 100L..999_000L) return
        timer.configuredTimeMs = calc
        _uiState.update { it.copy(
            fStopCorrectionNumerator = simplN,
            fStopCorrectionDenominator = simplD,
            displayTime = CountdownTimer.formatTime(calc),
            displayTimeMs = calc
        ) }
    }

    fun resetFStopCorrection() {
        if (timer.state != TimerState.STOPPED) return
        timer.configuredTimeMs = baseTimeMs
        _uiState.update { it.copy(
            fStopCorrectionNumerator = 0,
            fStopCorrectionDenominator = 1,
            displayTime = CountdownTimer.formatTime(baseTimeMs),
            displayTimeMs = baseTimeMs
        ) }
    }

    fun setFStopCorrectionAsBase() {
        if (timer.state != TimerState.STOPPED) return
        val calc = calculatedTimeMs()
        baseTimeMs = calc
        timer.configuredTimeMs = calc
        settingsRepository.defaultExposureMs = calc
        _uiState.update { it.copy(
            configuredTimeMs = calc,
            baseTimeMs = calc,
            fStopCorrectionNumerator = 0,
            fStopCorrectionDenominator = 1,
            displayTime = CountdownTimer.formatTime(calc),
            displayTimeMs = calc
        ) }
    }

    private val _uiState = MutableStateFlow(
        CountdownUiState(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            displayTimeMs = timer.configuredTimeMs,
            timerState = TimerState.STOPPED,
            relayState = RelayStates.INITIAL,
            selectedGrade = ContrastGrade.DEFAULT,
            configuredTimeMs = timer.configuredTimeMs,
            baseTimeMs = baseTimeMs,
            burnDodgeEntries = emptyList(),
            burnDodgeVisible = false,
            maxEntriesReached = false
        )
    )
    val uiState: StateFlow<CountdownUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(relayType = relayType) }

        audioSystem = createAudioSystem(getApplication())

        // Load defaults from settings repository
        try {
            timer.configuredTimeMs = settingsRepository.defaultExposureMs
            baseTimeMs = timer.configuredTimeMs
            _uiState.update { it.copy(
                displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
                displayTimeMs = timer.configuredTimeMs,
                configuredTimeMs = timer.configuredTimeMs,
                baseTimeMs = timer.configuredTimeMs,
                selectedGrade = settingsRepository.defaultContrastGrade,
                isMetronomeEnabled = settingsRepository.metronomeEnabled
            ) }
        } catch (e: Exception) {
            // prefs unavailable in test environment, keep hardcoded defaults
        }

        viewModelScope.launch {
            relayRepository.relayStates.collect { relayState ->
                _uiState.update { it.copy(relayState = relayState) }
            }
        }

        viewModelScope.launch {
            relayRepository.connectionState.collect { connState ->
                _uiState.update { state ->
                    state.copy(
                        connectionState = connState,
                        connectionTint = calculateConnectionTint(state.relayType, connState)
                    )
                }
            }
        }

        // Connect relay if not Null/Demo (network drivers need connection)
        viewModelScope.launch {
            relayRepository.connect().onFailure { e ->
                _uiState.update { it.copy(errorMessage = "Connection failed: ${e.message}") }
            }
        }

        viewModelScope.launch {
            try {
                fr.mathgl.darkroomtimer.storage.PreferenceManager.getInstance(getApplication())
                    .relayConfigFlow.collect { newConfig ->
                        if (_uiState.value.timerState != TimerState.STOPPED) return@collect
                        relayRepository.reloadConfig(newConfig)
                        relayRepository.connect().onFailure { e ->
                            _uiState.update { it.copy(errorMessage = "Reconnection failed: ${e.message}") }
                        }
                        _uiState.update { it.copy(relayType = newConfig.enlargerType) }
                    }
            } catch (_: Exception) {
                // PreferenceManager unavailable in test environment
            }
        }
    }

    fun start() {
        if (timer.state != TimerState.STOPPED) return
        timer.configuredTimeMs = calculatedTimeMs()       // use calculated time
        timer.start()
        Log.d(TAG, "start: ${timer.configuredTimeMs}ms")

        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            relayRepository.setSafelight(false)
            val result = if (!relayRepository.capabilities.canPause) {
                // TIMED_POWER : Tasmota gère l'extinction via TimedPower
                relayRepository.startTimedExposure(timer.configuredTimeMs)
            } else {
                // EXPLICIT_ON_OFF : le tick job envoie Power OFF en fin de minuteur
                relayRepository.setEnlarger(true)
            }

            if (result.isFailure) {
                Log.e(TAG, "start: relay command failed — ${result.exceptionOrNull()?.message}")
                _uiState.update { it.copy(errorMessage = "Hardware Error: ${result.exceptionOrNull()?.message}") }
                timer.stop()
                sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
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
        Log.d(TAG, "pause: ${timer.remainingMs()}ms remaining")
        tickJob?.cancel(); tickJob = null
        viewModelScope.launch { shutOffRelays("pause") }
        audioSystem?.pause()
        _uiState.update { it.copy(
            timerState = TimerState.PAUSED,
            displayTime = CountdownTimer.formatTime(timer.remainingMs()),
            displayTimeMs = timer.remainingMs()
        ) }
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        Log.d(TAG, "resume: ${timer.remainingMs()}ms remaining")
        viewModelScope.launch {
            val res1 = relayRepository.setEnlarger(true)
            if (!res1.isSuccess) {
                Log.e(TAG, "resume: relay command failed — enlarger=${res1.exceptionOrNull()?.message}")
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
            if (ended) timer.configuredTimeMs = baseTimeMs
            _uiState.update { it.copy(
                displayTime = if (ended) CountdownTimer.formatTime(calculatedTimeMs())
                              else CountdownTimer.formatTime(remaining),
                displayTimeMs = if (ended) calculatedTimeMs() else remaining,
                timerState = timer.state,
                enlargerOverride = if (ended) false else it.enlargerOverride,
                safelightOverride = if (ended) false else it.safelightOverride
            ) }
            sendServiceIntent(
                if (ended) ForegroundTimerService.ACTION_STOP else ForegroundTimerService.ACTION_UPDATE,
                remaining
            )
            if (ended) {
                Log.d(TAG, "timer completed")
                viewModelScope.launch { shutOffRelays("timer end") }
                audioSystem?.stopExposure()
                tickJob = null
                break
            }
            // Timer was stopped externally (e.g., relay failure)
            if (timer.state == TimerState.STOPPED) {
                audioSystem?.stopExposure()
                sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
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
        Log.d(TAG, "stop: wasPaused=$wasPaused")
        tickJob?.cancel(); tickJob = null
        timer.stop()
        timer.configuredTimeMs = baseTimeMs                           // restore base; start() will re-apply correction
        viewModelScope.launch { shutOffRelays("stop") }
        if (timerCompletedNaturally) {
            audioSystem?.stopExposure()
        }
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(calculatedTimeMs()),  // base × correction = next countdown duration
            displayTimeMs = calculatedTimeMs(),
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
            settingsRepository.defaultExposureMs = newBase
            val calc = calculatedTimeMs()
            _uiState.update { it.copy(
                displayTime = CountdownTimer.formatTime(calc),
                displayTimeMs = calc,
                configuredTimeMs = newBase,
                baseTimeMs = newBase
            ) }
        } else {
            // PAUSED: fine-tune remaining time; does not affect baseTimeMs or correction
            val newTime = (timer.configuredTimeMs + deltaMs).coerceIn(100L, 999_000L)
            timer.configuredTimeMs = newTime
            _uiState.update { it.copy(
                displayTime = CountdownTimer.formatTime(timer.remainingMs()),
                displayTimeMs = timer.remainingMs()
            ) }
        }
    }

    fun setBaseTime(ms: Long) {
        if (timer.state != TimerState.STOPPED) return
        val clamped = ms.coerceIn(100L, 999_000L)
        baseTimeMs = clamped
        timer.configuredTimeMs = clamped
        settingsRepository.defaultExposureMs = clamped
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(clamped),
            displayTimeMs = clamped,
            configuredTimeMs = clamped,
            baseTimeMs = clamped,
            fStopCorrectionNumerator = 0,
            fStopCorrectionDenominator = 1
        )}
    }

    fun setRemainingTime(ms: Long) {
        if (timer.state != TimerState.PAUSED) return
        val clamped = ms.coerceIn(100L, 999_000L)
        val delta = clamped - timer.remainingMs()
        timer.configuredTimeMs = (timer.configuredTimeMs + delta).coerceIn(100L, 999_000L)
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(timer.remainingMs()),
            displayTimeMs = timer.remainingMs()
        )}
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.update { it.copy(selectedGrade = grade) }
    }

    fun toggleEnlargerOverride() {
        if (_uiState.value.timerState == TimerState.RUNNING) return
        val newOverride = !_uiState.value.enlargerOverride
        viewModelScope.launch { relayRepository.setEnlarger(newOverride) }
        _uiState.update { it.copy(enlargerOverride = newOverride) }
    }

    fun toggleSafelightOverride() {
        if (_uiState.value.timerState == TimerState.RUNNING) return
        val newOverride = !_uiState.value.safelightOverride
        viewModelScope.launch { relayRepository.setSafelight(newOverride) }
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

    fun updateBurnDodgeEntry(
        id: Int,
        label: String,
        type: BurnDodgeType,
        numerator: Int,
        denominator: Int,
        contrastGrade: ContrastGrade
    ) {
        burnDodgeManager.updateEntry(id, label, type, numerator, denominator, contrastGrade)
        updateBurnDodgeState()
    }

    fun moveBurnDodgeEntryUp(id: Int) {
        burnDodgeManager.moveUp(id)
        updateBurnDodgeState()
    }

    fun moveBurnDodgeEntryDown(id: Int) {
        burnDodgeManager.moveDown(id)
        updateBurnDodgeState()
    }

    fun toggleMetronome() {
        val newEnabled = !_uiState.value.isMetronomeEnabled
        settingsRepository.metronomeEnabled = newEnabled
        val isExposureRunning = _uiState.value.timerState == TimerState.RUNNING
        audioSystem?.setMetronomeEnabled(newEnabled, activateNow = isExposureRunning)
        _uiState.update { it.copy(isMetronomeEnabled = newEnabled) }
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

    private suspend fun shutOffRelays(errorContext: String) {
        val r1 = relayRepository.setEnlarger(false)
        val r2 = relayRepository.setSafelight(false)
        if (!r1.isSuccess || !r2.isSuccess) {
            Log.e(TAG, "shutOffRelays ($errorContext): enlarger=${r1.exceptionOrNull()?.message} safelight=${r2.exceptionOrNull()?.message}")
            _uiState.update { it.copy(errorMessage = "Failed to shut off relays ($errorContext)") }
        }
    }

    private fun calculateConnectionTint(type: String, state: ConnectionState): ConnectionTint {
        return when {
            type == "NULL" || type == "DEMO" -> ConnectionTint.DIM
            state is ConnectionState.Connected  -> ConnectionTint.BRIGHT
            state is ConnectionState.Connecting -> ConnectionTint.MEDIUM
            state is ConnectionState.Error      -> ConnectionTint.BRIGHT
            else                                -> ConnectionTint.DIM
        }
    }

    open fun sendServiceIntent(action: String, remainingMs: Long) {
        val intent = Intent(getApplication(), ForegroundTimerService::class.java).apply {
            this.action = action
            putExtra(ForegroundTimerService.EXTRA_REMAINING_MS, remainingMs)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
        if (timer.state != TimerState.STOPPED) {
            sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
        }
        relayRepository.close()
        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { withTimeout(5_000L) { relayRepository.disconnect() } } catch (_: Exception) { }
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
                val settingsRepo = SettingsRepository(application)
                val relayRepo = RelayRepository(prefs.relaySystemConfig)
                return CountdownViewModel(
                    application,
                    relayRepo,
                    settingsRepo,
                    prefs.relaySystemConfig.enlargerType,
                    CountdownTimer()
                ) as T
            }
        }
    }
}
