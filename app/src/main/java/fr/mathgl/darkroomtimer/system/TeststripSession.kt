package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.math.TeststripEngine
import fr.mathgl.darkroomtimer.system.TeststripState.*

enum class TeststripState { INIT, EXPOSING, BETWEEN_PATCHES, PAUSED }

class TeststripSession(
    private val engine: TeststripEngine,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    var state: TeststripState = INIT
        private set

    private var currentPatchIndexValue: Int = -1
        set(value) {
            field = value
            if (value >= 0) {
                exposureStartAt = clock()
                pauseStartAt = null
            }
        }

    val currentPatchIndex: Int get() = currentPatchIndexValue

    private var exposureStartAt: Long = 0L
    private var pauseStartAt: Long? = null
    private var elapsedBeforePause: Long = 0L

    private val exposedPatches = mutableSetOf<Int>()

    val isExposing: Boolean get() = state == EXPOSING
    val isPaused: Boolean get() = state == PAUSED
    val isSessionComplete: Boolean get() = state == BETWEEN_PATCHES && exposedPatches.size == engine.patchCount

    val currentExposureTimeMs: Long
        get() = if (currentPatchIndex < 0) 0L else engine.getRelayDuration(currentPatchIndex)

    val remainingTimeMs: Long
        get() {
            if (currentPatchIndex < 0) return currentExposureTimeMs
            val elapsed = when (state) {
                EXPOSING -> clock() - exposureStartAt
                PAUSED -> elapsedBeforePause
                else -> 0L
            }
            return currentExposureTimeMs - elapsed
        }

    fun start() {
        check(state == INIT || state == BETWEEN_PATCHES || state == PAUSED) {
            "start() called from state $state"
        }
        if (state == PAUSED) {
            exposureStartAt = clock()
            elapsedBeforePause = 0L
            pauseStartAt = null
            state = EXPOSING
            return
        }
        currentPatchIndexValue = when (state) {
            INIT -> 0
            BETWEEN_PATCHES -> currentPatchIndexValue
            else -> throw IllegalStateException("Invalid state: $state")
        }
        state = EXPOSING
        exposureStartAt = clock()
        elapsedBeforePause = 0L
        pauseStartAt = null
    }

    fun pause() {
        check(state == EXPOSING) { "pause() called from state $state" }
        pauseStartAt = clock()
        elapsedBeforePause = pauseStartAt!! - exposureStartAt
        state = PAUSED
    }

    fun resume() {
        check(state == PAUSED) { "resume() called from state $state" }
        exposureStartAt = clock() - elapsedBeforePause
        pauseStartAt = null
        state = EXPOSING
    }

    fun finishExposure() {
        check(state == EXPOSING) { "finishExposure() called from state $state" }
        exposedPatches.add(currentPatchIndex)
        state = BETWEEN_PATCHES
    }

    fun nextPatch() {
        check(state == BETWEEN_PATCHES) { "nextPatch() called from state $state" }
        val nextIndex = if (currentPatchIndexValue == engine.patchCount - 1) 0 else currentPatchIndexValue + 1
        if (nextIndex == 0 && currentPatchIndexValue == engine.patchCount - 1) {
            exposedPatches.clear()
        }
        currentPatchIndexValue = nextIndex
        exposedPatches.remove(currentPatchIndexValue)
        state = EXPOSING
        exposureStartAt = clock()
        elapsedBeforePause = 0L
        pauseStartAt = null
    }

    fun restartCurrentPatch() {
        check(state == BETWEEN_PATCHES) { "restartCurrentPatch() called from state $state" }
        exposedPatches.remove(currentPatchIndexValue)
        state = EXPOSING
        exposureStartAt = clock()
        elapsedBeforePause = 0L
        pauseStartAt = null
    }

    fun abandon() {
        state = INIT
        currentPatchIndexValue = -1
        exposedPatches.clear()
    }

    fun isPatchExposed(patchIndex: Int): Boolean = exposedPatches.contains(patchIndex)

    fun formatTime(ms: Long): String = TeststripEngine.formatStopTime(ms)
}
