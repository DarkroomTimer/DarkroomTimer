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
import fr.mathgl.darkroomtimer.storage.PreferenceManager
import fr.mathgl.darkroomtimer.system.BurnDodgeManager
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.ForegroundTimerService
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.StandaloneRelaySystem
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
    val maxEntriesReached: Boolean
)

open class CountdownViewModel(
    application: Application,
    private val relaySystem: RelaySystem
) : AndroidViewModel(application) {

    private val timer = CountdownTimer()
    private val burnDodgeManager = BurnDodgeManager()
    private var audioSystem: AudioSystem? = null
    private var tickJob: Job? = null

    private fun getAudioSystem(): AudioSystem {
        if (audioSystem == null) {
            try {
                val context = getApplication<Application>()
                val preferenceManager = PreferenceManager.getInstance(context)
                val audioPreferences = AudioPreferences(preferenceManager.prefs)
                val audioEngine = ToneGeneratorAudioEngine(audioPreferences.buzzerVolume)
                audioSystem = AudioSystem(audioEngine, audioPreferences, audioPreferences.buzzerVolume)
            } catch (e: Exception) {
                // In test environments or when prefs are unavailable, audioSystem remains null
                // Audio operations will be silently skipped
            }
        }
        return audioSystem!!
    }

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

        audioSystem?.startExposure()
        _uiState.update { it.copy(timerState = TimerState.RUNNING) }
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
        audioSystem?.pause()
        _uiState.update { it.copy(timerState = TimerState.PAUSED) }
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        viewModelScope.launch {
            relaySystem.setEnlarger(true)
            relaySystem.setSafelight(true)
        }
        audioSystem?.resume()
        _uiState.update { it.copy(timerState = TimerState.RUNNING) }
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
        val wasPaused = timer.state == TimerState.PAUSED
        val timerCompletedNaturally = timer.state == TimerState.RUNNING
        tickJob?.cancel(); tickJob = null
        timer.stop()
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        if (timerCompletedNaturally) {
            audioSystem?.stopExposure()
        }
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED
        ) }
        if (!wasPaused) {
            sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
        }
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
                val relaySystem = StandaloneRelaySystem()
                return CountdownViewModel(application, relaySystem) as T
            }
        }
    }
}
