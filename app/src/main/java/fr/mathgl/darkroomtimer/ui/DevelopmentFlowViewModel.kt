package fr.mathgl.darkroomtimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class DevelopmentFlowViewModel : ViewModel() {

    private val _selectedProfile = MutableStateFlow<DevelopmentProfile?>(null)
    val selectedProfile: StateFlow<DevelopmentProfile?> = _selectedProfile.asStateFlow()

    private val _sessionSnapshot = MutableStateFlow<DevelopmentSessionStateSnapshot?>(null)
    val sessionSnapshot: StateFlow<DevelopmentSessionStateSnapshot?> = _sessionSnapshot.asStateFlow()

    private val _editingProfile = MutableStateFlow<DevelopmentProfile?>(null)
    val editingProfile: StateFlow<DevelopmentProfile?> = _editingProfile.asStateFlow()

    private var currentSession: DevelopmentSession? = null

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
        _selectedProfile.value = profile
        val session = DevelopmentSession(profile)
        currentSession = session
        viewModelScope.launch {
            var tickJob: Job? = null
            session.stateFlow.collect { snapshot ->
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
        currentSession = null
        _sessionSnapshot.value = null
        _selectedProfile.value = null
    }

    fun sessionStart() { currentSession?.start() }
    fun sessionPause() { currentSession?.pause() }
    fun sessionResume() { currentSession?.resume() }
    fun sessionNextStep() { currentSession?.nextStep() }
}
