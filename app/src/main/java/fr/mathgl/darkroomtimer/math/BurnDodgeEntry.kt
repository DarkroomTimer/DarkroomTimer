package fr.mathgl.darkroomtimer.math

import kotlin.math.pow

data class BurnDodgeEntry(
    val id: Int,                      // Unique ID within session
    val label: String,                // Zone description (e.g., "ciel")
    val type: BurnDodgeType,
    val numerator: Int,               // Stop fraction numerator
    val denominator: Int,             // Stop fraction denominator (12, 6, 4, 3, 2, 1)
    val contrastGrade: ContrastGrade
) {
    /**
     * Calculate adjustment time based on a base exposure time.
     * For BURN: adds time (positive adjustment)
     * For DODGE: subtracts time (negative adjustment)
     */
    fun adjustmentTimeMs(baseTimeMs: Long): Long {
        val stops = numerator.toDouble() / denominator
        return when (type) {
            BurnDodgeType.BURN -> {
                // adjustment = base * (2^stops - 1)
                (baseTimeMs * (2.0.pow(stops) - 1)).toLong()
            }
            BurnDodgeType.DODGE -> {
                // adjustment = base * (1 - 2^(-stops))
                (baseTimeMs * (1 - 2.0.pow(-stops))).toLong()
            }
        }
    }

    /** Human-readable fraction label like "+1/3 stop" or "-1/2 stop" */
    val fractionLabel: String
        get() {
            val sign = if (type == BurnDodgeType.BURN) "+" else "-"
            return "$sign${numerator}/${denominator} stop"
        }
}
