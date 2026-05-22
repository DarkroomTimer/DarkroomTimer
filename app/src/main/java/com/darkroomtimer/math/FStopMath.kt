package com.darkroomtimer.math

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

object FStopMath {

    fun adjustTime(baseMs: Long, numerator: Int, denominator: Int, step: Int): Long {
        if (denominator == 0) return baseMs

        val stops = (numerator.toDouble() / denominator) * step
        val result = (baseMs * 2.0.pow(stops)).toLong()

        return if (result < 0) 0 else result
    }

    fun simplify(numerator: Int, denominator: Int): Pair<Int, Int> {
        if (denominator == 0) return Pair(numerator, denominator)

        val common = gcd(abs(numerator), abs(denominator))
        var n = numerator / common
        var d = denominator / common

        if (d < 0) {
            n = -n
            d = -d
        }

        return Pair(n, d)
    }

    fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }

    fun formatStop(numerator: Int, denominator: Int): String {
        val (n, d) = simplify(numerator, denominator)
        if (n == 0 || d == 0) return "0"

        val absN = abs(n)
        val whole = absN / d
        val remainder = absN % d
        val sign = if (n < 0) "-" else ""

        return when {
            whole == 0 -> "$sign$remainder/$d"
            remainder == 0 -> "$sign$whole"
            else -> "$sign$whole $remainder/$d"
        }
    }
}
