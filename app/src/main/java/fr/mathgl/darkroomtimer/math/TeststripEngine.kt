package fr.mathgl.darkroomtimer.math

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Calculates exposure times for photographic teststrip patches using f-stop math.
 *
 * @param baseTimeMs Base exposure time in milliseconds (must be in [100, 999000])
 * @param numerator Numerator of the stop increment fraction
 * @param denominator Denominator of the stop increment fraction (must not be zero)
 * @param patchCount Number of patches to calculate (must be in [3, 7])
 */
class TeststripEngine(
    var baseTimeMs: Long,
    var numerator: Int,
    var denominator: Int,
    val patchCount: Int
) {
    init {
        require(baseTimeMs in 100L..999_000L) {
            "baseTimeMs must be in [100, 999000], was $baseTimeMs"
        }
        require(patchCount in 3..7) {
            "patchCount must be in [3, 7], was $patchCount"
        }
        require(denominator != 0) {
            "denominator cannot be zero"
        }
    }

    /**
     * Returns the simplified fraction (numerator, denominator) by dividing both by their GCD.
     */
    val simplifiedFraction: Pair<Int, Int>
        get() = simplify(numerator, denominator)

    /**
     * Returns the cumulative exposure time in milliseconds for each patch.
     * Patch times are calculated as: baseTimeMs * 2^(stops * patchIndex)
     */
    val patchTimesMs: List<Long>
        get() = (0 until patchCount).map { n ->
            val stops = (numerator.toDouble() / denominator) * n
            (baseTimeMs * 2.0.pow(stops)).roundToLong()
        }

    /**
     * Returns the differential (incremental) exposure time for each patch.
     * For patch 0: baseTimeMs
     * For patch i>0: patchTimesMs[i] - patchTimesMs[i-1]
     */
    val differentialTimesMs: List<Long>
        get() {
            val times = patchTimesMs
            return buildList(patchCount) {
                add(times[0])
                for (i in 1 until patchCount) {
                    add(times[i] - times[i - 1])
                }
            }
        }

    /**
     * Returns the differential time for a specific patch index.
     * For patch 0: baseTimeMs
     * For patch i>0: patchTimesMs[i] - patchTimesMs[i-1]
     */
    fun differentialTimeForPatch(patchIndex: Int): Long {
        val times = patchTimesMs
        return if (patchIndex == 0) times[0] else times[patchIndex] - times[patchIndex - 1]
    }

    /**
     * Formats a time in milliseconds to MM:SS.t format (minutes:seconds.tenths).
     * Negative values are clamped to zero.
     */
    companion object {
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

    private fun simplify(numerator: Int, denominator: Int): Pair<Int, Int> {
        if (denominator == 0) return Pair(numerator, denominator)
        val common = gcd(kotlin.math.abs(numerator), denominator)
        var n = numerator / common
        var d = denominator / common
        if (d < 0) { n = -n; d = -d }
        return Pair(n, d)
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }
}
