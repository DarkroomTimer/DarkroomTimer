package fr.mathgl.darkroomtimer.development

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DevelopmentSessionState { CONFIGURED, ACTIVE, PAUSED, COMPLETED }

data class DevelopmentSessionStateSnapshot(
    val state: DevelopmentSessionState,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val currentStep: DevelopmentStep?,
    val remainingSteps: Int,
    val progress: Int, // 0-100
    val isPreEndAlertTriggered: Boolean,
    val isCompleted: Boolean
)

/**
 * Machine d'états pour une session de développement chimique.
 * Gère la progression dans les étapes d'un profil.
 */
class DevelopmentSession(
    private val profile: DevelopmentProfile,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val _stateFlow = MutableStateFlow(initialState())
    val stateFlow: StateFlow<DevelopmentSessionStateSnapshot> = _stateFlow.asStateFlow()

    val state: DevelopmentSessionState get() = _stateFlow.value.state
    val currentStepIndex: Int get() = _stateFlow.value.currentStepIndex
    val totalSteps: Int get() = profile.stepCount()
    val remainingSteps: Int get() = totalSteps - currentStepIndex
    val progress: Int get() = if (totalSteps > 0) ((currentStepIndex * 100) / totalSteps) else 0
    val isPreEndAlertTriggered: Boolean get() = _stateFlow.value.isPreEndAlertTriggered
    val isCompleted: Boolean get() = _stateFlow.value.isCompleted

    val isRunning: Boolean get() = state == DevelopmentSessionState.ACTIVE
    val isPaused: Boolean get() = state == DevelopmentSessionState.PAUSED

    val currentStep: DevelopmentStep?
        get() = if (currentStepIndex in 0 until totalSteps) {
            profile.steps[currentStepIndex]
        } else null

    val currentStepElapsedSeconds: Long
        get() = _stateFlow.value.currentStep?.elapsedSeconds ?: 0L

    val currentStepRemainingSeconds: Int
        get() = currentStep?.remainingSeconds(currentStepElapsedSeconds) ?: 0

    private fun initialState(): DevelopmentSessionStateSnapshot {
        return DevelopmentSessionStateSnapshot(
            state = DevelopmentSessionState.CONFIGURED,
            currentStepIndex = -1,
            totalSteps = totalSteps,
            currentStep = null,
            remainingSteps = totalSteps,
            progress = 0,
            isPreEndAlertTriggered = false,
            isCompleted = false
        )
    }

    fun start() {
        require(_stateFlow.value.state == DevelopmentSessionState.CONFIGURED) {
            "Cannot start from state ${_stateFlow.value.state}"
        }
        val firstStep = profile.steps.firstOrNull()?.copyWithElapsed(0L)
        updateState { it.copy(
            state = DevelopmentSessionState.ACTIVE,
            currentStepIndex = 0,
            currentStep = firstStep,
            remainingSteps = totalSteps - 1,
            progress = 0
        ) }
    }

    fun pause() {
        require(_stateFlow.value.state == DevelopmentSessionState.ACTIVE) {
            "Cannot pause from state ${_stateFlow.value.state}"
        }
        updateState { it.copy(state = DevelopmentSessionState.PAUSED) }
    }

    fun resume() {
        require(_stateFlow.value.state == DevelopmentSessionState.PAUSED) {
            "Cannot resume from state ${_stateFlow.value.state}"
        }
        updateState { it.copy(state = DevelopmentSessionState.ACTIVE) }
    }

    /** Incrémenter le temps écoulé pour l'étape courante */
    fun tick() {
        if (_stateFlow.value.state != DevelopmentSessionState.ACTIVE) return

        val current = _stateFlow.value
        val currentStep = current.currentStep ?: return

        val newElapsed = currentStep.elapsedSeconds + 1

        // Check if step has ended
        if (currentStep.hasEnded(newElapsed)) {
            val currentIdx = current.currentStepIndex

            // Check if this is the last step - mark completed regardless of mode
            if (currentIdx + 1 >= totalSteps) {
                // Session completed
                updateState { it.copy(
                    state = DevelopmentSessionState.COMPLETED,
                    isCompleted = true,
                    progress = 100,
                    isPreEndAlertTriggered = false
                ) }
            } else if (profile.navigationMode == DevelopmentNavigationMode.AUTOMATIC) {
                // Auto-advance to next step in AUTOMATIC mode
                val nextStep = profile.steps[currentIdx + 1].copyWithElapsed(0L)
                updateState { it.copy(
                    currentStepIndex = currentIdx + 1,
                    currentStep = nextStep,
                    remainingSteps = totalSteps - (currentIdx + 1),
                    progress = ((currentIdx + 1) * 100) / totalSteps,
                    isPreEndAlertTriggered = false
                ) }
            } else {
                // In MANUAL mode, stay on the current step but clear the preEndAlert
                // The step has ended, so preEndAlert is no longer relevant
                updateState { it.copy(
                    isPreEndAlertTriggered = false
                ) }
            }
        } else {
            // Update elapsed time for current step
            val updatedStep = currentStep.copyWithElapsed(newElapsed)
            val isPreEndAlert = isPreEndAlertTriggered(updatedStep, newElapsed)
            updateState { it.copy(
                currentStep = updatedStep,
                isPreEndAlertTriggered = isPreEndAlert
            ) }
        }
    }

    private fun isPreEndAlertTriggered(step: DevelopmentStep, elapsed: Long): Boolean {
        return when (step) {
            is DevelopmentStep.BathStep -> step.isPreEndAlertTriggered(elapsed)
            is DevelopmentStep.PauseStep -> false
        }
    }

    /** Passer manuellement à l'étape suivante (mode MANUAL) */
    fun nextStep() {
        require(_stateFlow.value.state == DevelopmentSessionState.ACTIVE ||
                _stateFlow.value.state == DevelopmentSessionState.PAUSED) {
            "Cannot call nextStep from state ${_stateFlow.value.state}"
        }
        val currentIndex = currentStepIndex
        if (currentIndex >= 0 && currentIndex < totalSteps - 1) {
            val nextStep = profile.steps[currentIndex + 1].copyWithElapsed(0L)
            updateState { it.copy(
                currentStepIndex = currentIndex + 1,
                currentStep = nextStep,
                remainingSteps = totalSteps - (currentIndex + 1),
                progress = ((currentIndex + 1) * 100) / totalSteps,
                isPreEndAlertTriggered = false
            ) }
        }
    }

    /** Annuler et retourner à l'état CONFIGURED */
    fun cancel() {
        updateState { initialState() }
    }

    private fun updateState(mutator: (DevelopmentSessionStateSnapshot) -> DevelopmentSessionStateSnapshot) {
        _stateFlow.value = mutator(_stateFlow.value)
    }
}
