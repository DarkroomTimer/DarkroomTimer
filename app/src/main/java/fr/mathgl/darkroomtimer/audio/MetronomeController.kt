package fr.mathgl.darkroomtimer.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Contrôle le métronome périodique pendant les expositions.
 * Utilise un scope de coroutine pour le timing précis.
 */
class MetronomeController(
    private val audioEngine: AudioEngine,
    private var cadenceMs: Int = 1000,
    private val frequencyHz: Int = 250,
    private val durationMs: Int = 25
) {
    private var scope: CoroutineScope? = null
    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun start() {
        if (isRunning) return
        scope = CoroutineScope(Dispatchers.Default)
        job = scope?.launch {
            while (isActive) {
                audioEngine.playTone(frequencyHz, durationMs, 1f)
                delay(cadenceMs.toLong())
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
    }

    fun setCadence(newCadenceMs: Int) {
        cadenceMs = newCadenceMs.coerceIn(MIN_CADENCE_MS, MAX_CADENCE_MS)
    }

    companion object {
        private const val MIN_CADENCE_MS = 500
        private const val MAX_CADENCE_MS = 5000
    }
}
