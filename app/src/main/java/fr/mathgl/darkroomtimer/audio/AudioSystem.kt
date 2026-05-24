package fr.mathgl.darkroomtimer.audio

/**
 * Orchestrate audio feedback for the DarkroomTimer application.
 *
 * This class provides a high-level API for playing sound effects during
 * timer events (start exposure, stop exposure, test strip operations)
 * and manages the metronome during exposures.
 *
 * @param audioEngine The underlying audio engine for playing sounds
 * @param audioSettingsProvider User preferences for audio behavior
 * @param audioVolume Current volume level for audio output
 */
class AudioSystem(
    private val audioEngine: AudioEngine,
    private val audioSettingsProvider: AudioSettingsProvider,
    private var audioVolume: AudioVolume
) {
    private var metronomeController: MetronomeController? = null
    private var isRunning = true

    /**
     * Whether the metronome is currently running.
     */
    val isMetronomeRunning: Boolean
        get() = metronomeController?.isRunning == true

    /**
     * Start an exposure. Plays a start beep if enabled and starts the metronome.
     */
    fun startExposure() {
        if (!isRunning) return

        // Play start beep if enabled
        if (audioSettingsProvider.isStartBeepEnabled) {
            audioEngine.playTone(START_BEEP_FREQUENCY_HZ, START_BEEP_DURATION_MS, audioVolume.toFloat())
        }

        // Start metronome if enabled
        if (audioSettingsProvider.isMetronomeEnabled) {
            startMetronome()
        }
    }

    /**
     * Stop an exposure. Stops the metronome and plays stop feedback.
     */
    fun stopExposure() {
        if (!isRunning) return

        // Stop metronome
        stopMetronome()

        // Play stop exposure feedback (3 beeps at 880Hz)
        audioEngine.playBeepSequence(
            frequencyHz = STOP_EXPOSURE_FREQUENCY_HZ,
            beepCount = 3,
            beepDurationMs = DEFAULT_BEEP_DURATION_MS,
            silenceBetweenMs = DEFAULT_BEEP_GAP_MS,
            volume = audioVolume.toFloat()
        )
    }

    /**
     * Stop a test strip patch. Plays feedback sound.
     */
    fun stopTeststripPatch() {
        if (!isRunning) return

        // Play test strip patch feedback (2 beeps at 660Hz)
        audioEngine.playBeepSequence(
            frequencyHz = STOP_TESTSTRIP_PATCH_FREQUENCY_HZ,
            beepCount = 2,
            beepDurationMs = DEFAULT_BEEP_DURATION_MS,
            silenceBetweenMs = DEFAULT_BEEP_GAP_MS,
            volume = audioVolume.toFloat()
        )
    }

    /**
     * Stop a test strip session. Plays completion feedback.
     */
    fun stopTeststripSession() {
        if (!isRunning) return

        // Play test strip session completion feedback (4 ascending beeps)
        audioEngine.playBeepSequence(
            frequencyHz = 440,
            beepCount = 1,
            beepDurationMs = DEFAULT_BEEP_DURATION_MS,
            silenceBetweenMs = DEFAULT_BEEP_GAP_MS,
            volume = audioVolume.toFloat()
        )
        audioEngine.playBeepSequence(
            frequencyHz = 550,
            beepCount = 1,
            beepDurationMs = DEFAULT_BEEP_DURATION_MS,
            silenceBetweenMs = DEFAULT_BEEP_GAP_MS,
            volume = audioVolume.toFloat()
        )
        audioEngine.playBeepSequence(
            frequencyHz = 660,
            beepCount = 1,
            beepDurationMs = DEFAULT_BEEP_DURATION_MS,
            silenceBetweenMs = DEFAULT_BEEP_GAP_MS,
            volume = audioVolume.toFloat()
        )
        audioEngine.playBeepSequence(
            frequencyHz = 880,
            beepCount = 1,
            beepDurationMs = DEFAULT_BEEP_DURATION_MS,
            silenceBetweenMs = DEFAULT_BEEP_GAP_MS,
            volume = audioVolume.toFloat()
        )
    }

    /**
     * Pause the metronome temporarily.
     */
    fun pause() {
        if (!isRunning) return
        stopMetronome()
    }

    /**
     * Resume the metronome after a pause.
     */
    fun resume() {
        if (!isRunning) return
        if (audioSettingsProvider.isMetronomeEnabled) {
            startMetronome()
        }
    }

    /**
     * Completely stop all audio activity.
     */
    fun stop() {
        if (!isRunning) return
        stopMetronome()
        audioEngine.stop()
    }

    /**
     * Set the volume level for audio output.
     *
     * @param volume The new volume level
     */
    fun setVolume(volume: AudioVolume) {
        audioVolume = volume
    }

    /**
     * Set the metronome cadence (time between clicks).
     *
     * @param newCadenceMs The new cadence in milliseconds
     */
    fun setMetronomeCadence(newCadenceMs: Int) {
        audioSettingsProvider.metronomeCadenceMs = newCadenceMs.coerceIn(MIN_CADENCE_MS, MAX_CADENCE_MS)
        metronomeController?.setCadence(audioSettingsProvider.metronomeCadenceMs)
    }

    /**
     * Release all resources. Call this when the AudioSystem is no longer needed.
     */
    fun release() {
        stopMetronome()
        audioEngine.release()
        isRunning = false
    }

    private fun startMetronome() {
        if (metronomeController == null) {
            metronomeController = MetronomeController(
                audioEngine = audioEngine,
                cadenceMs = audioSettingsProvider.metronomeCadenceMs,
                frequencyHz = audioSettingsProvider.metronomeFrequencyHz,
                durationMs = audioSettingsProvider.metronomeDurationMs
            )
        }
        metronomeController?.start()
    }

    private fun stopMetronome() {
        metronomeController?.stop()
        metronomeController = null
    }

    companion object {
        // Start exposure beep: 440Hz (A4 note)
        private const val START_BEEP_FREQUENCY_HZ = 440
        private const val START_BEEP_DURATION_MS = 150

        // Stop exposure feedback: 3 beeps at 880Hz
        private const val STOP_EXPOSURE_FREQUENCY_HZ = 880

        // Stop test strip patch feedback: 2 beeps at 660Hz
        private const val STOP_TESTSTRIP_PATCH_FREQUENCY_HZ = 660

        // Default beep parameters
        private const val DEFAULT_BEEP_DURATION_MS = 100
        private const val DEFAULT_BEEP_GAP_MS = 100

        // Metronome cadence limits
        private const val MIN_CADENCE_MS = 500
        private const val MAX_CADENCE_MS = 5000
    }
}
