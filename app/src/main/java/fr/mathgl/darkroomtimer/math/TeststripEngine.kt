package fr.mathgl.darkroomtimer.math

import kotlin.math.pow
import kotlin.math.roundToLong

enum class TeststripMode { INCREMENTAL, SEPARATE }
enum class IncrementType { F_STOP, SECONDS }

/**
 * Calculates exposure times for photographic teststrip patches.
 *
 * @param baseTimeMs Base exposure time in milliseconds (must be in [100, 999000])
 * @param numerator Numerator of the stop increment fraction (used if [incrementType] is F_STOP)
 * @param denominator Denominator of the stop increment fraction (must not be zero, used if [incrementType] is F_STOP)
 * @param patchCount Number of patches to calculate (must be in [3, 12])
 * @param mode Whether to return absolute times (SEPARATE) or differential times (INCREMENTAL)
 * @param incrementType The type of increment to use (F_STOP or SECONDS)
 * @param incrementMs The increment in milliseconds (used if [incrementType] is SECONDS)
 */
class TeststripEngine(
    baseTimeMs: Long,
    numerator: Int,
    denominator: Int,
    patchCount: Int,
    mode: TeststripMode,
    incrementType: IncrementType,
    incrementMs: Long = 0L
) {
    var baseTimeMs: Long = baseTimeMs
        private set
    var numerator: Int = numerator
        private set
    var denominator: Int = denominator
        private set
    var patchCount: Int = patchCount
        private set
    var mode: TeststripMode = mode
        private set
    var incrementType: IncrementType = incrementType
        private set
    var incrementMs: Long = incrementMs
        private set

    init {
        require(baseTimeMs in 100L..999_000L) {
            "baseTimeMs must be in [100, 999000], was $baseTimeMs"
        }
        require(patchCount in 3..12) {
            "patchCount must be in [3, 12], was $patchCount"
        }
        require(denominator != 0) {
            "denominator cannot be zero"
        }
    }

    fun updateBaseTime(newTime: Long) {
        require(newTime in 100L..999_000L) { "baseTimeMs must be in [100, 999000], was $newTime" }
        baseTimeMs = newTime
    }

    fun updatePatchCount(count: Int) {
        require(count in 3..12) { "patchCount must be in [3, 12], was $count" }
        patchCount = count
    }

    fun updateStopFraction(newNum: Int, newDen: Int) {
        require(newDen != 0) { "denominator cannot be zero" }
        numerator = newNum
        denominator = newDen
    }

    fun updateMode(newMode: TeststripMode) {
        mode = newMode
    }

    fun updateIncrementType(newType: IncrementType) {
        incrementType = newType
    }

    fun updateIncrementMs(ms: Long) {
        require(ms >= 100L) { "incrementMs must be at least 100ms, was $ms" }
        incrementMs = ms
    }

    /**
     * Calculates the absolute exposure time for a given patch index.
     */
    private fun calculateAbsoluteTime(index: Int): Long {
        return when (incrementType) {
            IncrementType.F_STOP -> {
                val stops = (numerator.toDouble() / denominator) * index
                (baseTimeMs * 2.0.pow(stops)).roundToLong()
            }
            IncrementType.SECONDS -> {
                baseTimeMs + (incrementMs * index)
            }
        }
    }

    /**
     * Returns the exposure duration for the relay for a specific patch index,
     * based on the selected [mode].
     */
    fun getRelayDuration(index: Int): Long {
        return when (mode) {
            TeststripMode.SEPARATE -> calculateAbsoluteTime(index)
            TeststripMode.INCREMENTAL -> {
                if (index == 0) {
                    calculateAbsoluteTime(0)
                } else {
                    calculateAbsoluteTime(index) - calculateAbsoluteTime(index - 1)
                }
            }
        }
    }

    /**
     * Returns the simplified fraction (numerator, denominator) by dividing both by their GCD.
     */
    val simplifiedFraction: Pair<Int, Int>
        get() = FStopMath.simplify(numerator, denominator)

    /**
     * Returns the cumulative exposure time in milliseconds for each patch.
     */
    val patchTimesMs: List<Long>
        get() = (0 until patchCount).map { calculateAbsoluteTime(it) }

    /**
     * Returns the differential (incremental) exposure time for each patch.
     */
    val differentialTimesMs: List<Long>
        get() = (0 until patchCount).map { getRelayDurationForIncremental(it) }

    private fun getRelayDurationForIncremental(index: Int): Long {
        return if (index == 0) {
            calculateAbsoluteTime(0)
        } else {
            calculateAbsoluteTime(index) - calculateAbsoluteTime(index - 1)
        }
    }

    /**
     * Returns the differential time for a specific patch index.
     * Note: this is only logically consistent if mode is INCREMENTAL.
     */
    fun differentialTimeForPatch(patchIndex: Int): Long {
        return getRelayDurationForIncremental(patchIndex)
    }


    /**
     * Formats a time in milliseconds to MM:SS.t format (minutes:seconds.tenths).
     * Negative values are clamped to zero.
     */
    fun adjustIncrement(delta: Int) {
        when (incrementType) {
            IncrementType.F_STOP -> {
                numerator += delta
                if (numerator < 1) numerator = 1
            }
            IncrementType.SECONDS -> {
                incrementMs = (incrementMs + delta * SECONDS_INCREMENT_STEP_MS).coerceAtLeast(100L)
            }
        }
    }

    companion object {
        private const val SECONDS_INCREMENT_STEP_MS = 1000L

        fun formatStopTime(ms: Long): String {
            val clamped = maxOf(0L, ms)
            val totalTenths = clamped / 100
            val tenths = totalTenths % 10
            val totalSeconds = totalTenths / 10
            val seconds = totalSeconds % 60
            val minutes = totalSeconds / 60
            return "%02d:%02d.%d".format(minutes, seconds, tenths)
        }
    }
}
