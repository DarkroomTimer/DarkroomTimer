package fr.mathgl.darkroomtimer.development

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.audio.AudioSystem
import fr.mathgl.darkroomtimer.audio.createAudioSystem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DevelopmentViewModel(
    application: Application,
    private val profile: DevelopmentProfile
) : AndroidViewModel(application) {

    private val session = DevelopmentSession(profile)

    private val audioSystem: AudioSystem? = createAudioSystem(getApplication())
    private var tickJob: Job? = null

    private fun getAudioSystem(): AudioSystem? = audioSystem

    val stateFlow: StateFlow<DevelopmentSessionStateSnapshot> = session.stateFlow

    val isRunning: Boolean get() = session.state == DevelopmentSessionState.ACTIVE
    val isPaused: Boolean get() = session.state == DevelopmentSessionState.PAUSED
    val isCompleted: Boolean get() = session.state == DevelopmentSessionState.COMPLETED

    val currentStep = session.currentStep
    val currentStepElapsedSeconds = session.currentStepElapsedSeconds
    val currentStepRemainingSeconds = session.currentStepRemainingSeconds
    val progress = session.progress
    val isPreEndAlertTriggered = session.isPreEndAlertTriggered

    init {
        // Observe state changes for alert handling
        viewModelScope.launch {
            session.stateFlow.collect { snapshot ->
                // Handle pre-end alert if triggered
                if (snapshot.isPreEndAlertTriggered) {
                    playPreEndAlert()
                }
            }
        }
    }

    fun start() {
        session.start()
        getAudioSystem()?.startExposure()
        startTickJob()
    }

    fun pause() {
        session.pause()
        getAudioSystem()?.pause()
        tickJob?.cancel()
        tickJob = null
    }

    fun resume() {
        session.resume()
        getAudioSystem()?.resume()
        startTickJob()
    }

    fun nextStep() {
        session.nextStep()
    }

    fun cancel() {
        session.cancel()
        getAudioSystem()?.stop()
        tickJob?.cancel()
        tickJob = null
    }

    override fun onCleared() {
        super.onCleared()
        getAudioSystem()?.release()
        tickJob?.cancel()
    }

    private fun startTickJob() {
        if (session.state != DevelopmentSessionState.ACTIVE) return
        tickJob = viewModelScope.launch {
            while (session.state == DevelopmentSessionState.ACTIVE) {
                delay(1000)
                session.tick()
            }
        }
    }

    private fun playPreEndAlert() {
        // Stop exposure (plays a single beep)
        getAudioSystem()?.stopExposure()
    }
}
