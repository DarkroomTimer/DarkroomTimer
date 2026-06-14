package fr.mathgl.darkroomtimer.ui.development

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.audio.AudioSystem
import fr.mathgl.darkroomtimer.audio.createAudioSystem
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentSession
import fr.mathgl.darkroomtimer.development.DevelopmentSessionState
import fr.mathgl.darkroomtimer.development.DevelopmentSessionStateSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevelopmentFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedProfile = MutableStateFlow<DevelopmentProfile?>(null)
    val selectedProfile: StateFlow<DevelopmentProfile?> = _selectedProfile.asStateFlow()

    private val _sessionSnapshot = MutableStateFlow<DevelopmentSessionStateSnapshot?>(null)
    val sessionSnapshot: StateFlow<DevelopmentSessionStateSnapshot?> = _sessionSnapshot.asStateFlow()

    private val _editingProfile = MutableStateFlow<DevelopmentProfile?>(null)
    val editingProfile: StateFlow<DevelopmentProfile?> = _editingProfile.asStateFlow()

    private var currentSession: DevelopmentSession? = null
    private var sessionCollectJob: Job? = null
    private var prevSnapshot: DevelopmentSessionStateSnapshot? = null

    private val audioSystem: AudioSystem? = createAudioSystem(application)

    fun setSelectedProfile(profile: DevelopmentProfile) {
        _selectedProfile.value = profile
    }

    fun setEditingProfile(profile: DevelopmentProfile?) {
        _editingProfile.value = profile
    }

    fun clearEditingProfile() {
        _editingProfile.value = null
    }

    fun startSession(profile: DevelopmentProfile) {
        sessionCollectJob?.cancel()
        prevSnapshot = null
        _selectedProfile.value = profile
        val session = DevelopmentSession(profile)
        currentSession = session
        sessionCollectJob = viewModelScope.launch {
            var tickJob: Job? = null
            session.stateFlow.collect { snapshot ->
                val prev = prevSnapshot
                if (prev != null) {
                    when {
                        snapshot.isPreEndAlertTriggered && !prev.isPreEndAlertTriggered ->
                            audioSystem?.stopExposure()
                        snapshot.isCompleted && !prev.isCompleted ->
                            audioSystem?.stopTeststripSession()
                        snapshot.isStepEnded && !prev.isStepEnded ->
                            audioSystem?.stopTeststripPatch()
                    }
                }
                prevSnapshot = snapshot
                _sessionSnapshot.value = snapshot
                tickJob?.cancel()
                if (snapshot.state == DevelopmentSessionState.ACTIVE) {
                    tickJob = launch {
                        while (session.isRunning) {
                            delay(1000)
                            if (session.isRunning) session.tick()
                        }
                    }
                }
            }
        }
    }

    fun cancelSession() {
        sessionCollectJob?.cancel()
        sessionCollectJob = null
        currentSession = null
        prevSnapshot = null
        _sessionSnapshot.value = null
        _selectedProfile.value = null
        audioSystem?.stop()
    }

    fun sessionStart() {
        currentSession?.start()
        audioSystem?.startExposure()
    }

    fun sessionPause() {
        currentSession?.pause()
        audioSystem?.pause()
    }

    fun sessionResume() {
        currentSession?.resume()
        audioSystem?.resume()
    }

    fun sessionNextStep() { currentSession?.nextStep() }

    override fun onCleared() {
        super.onCleared()
        audioSystem?.release()
    }
}
