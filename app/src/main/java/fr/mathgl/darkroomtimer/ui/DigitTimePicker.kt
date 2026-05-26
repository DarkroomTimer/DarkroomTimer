// app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import kotlinx.coroutines.CancellationException
import kotlin.math.abs

enum class DigitTimeFormat {
    MINUTES_SECONDS_TENTHS,  // MM:SS.T — countdown, teststrip
    HOURS_MINUTES_SECONDS    // HH:MM:SS — développement
}

internal fun msToDigits(ms: Long, format: DigitTimeFormat): IntArray = when (format) {
    DigitTimeFormat.MINUTES_SECONDS_TENTHS -> {
        val t = (ms / 100 % 10).toInt()
        val totalS = ms / 1000
        val s = (totalS % 60).toInt()
        val m = (totalS / 60).toInt()
        intArrayOf(m / 10, m % 10, s / 10, s % 10, t)
    }
    DigitTimeFormat.HOURS_MINUTES_SECONDS -> {
        val totalS = ms / 1000
        val s = (totalS % 60).toInt()
        val totalM = totalS / 60
        val m = (totalM % 60).toInt()
        val h = (totalM / 60).toInt()
        intArrayOf(h / 10, h % 10, m / 10, m % 10, s / 10, s % 10)
    }
}

internal fun applyDelta(ms: Long, digitIndex: Int, delta: Int, format: DigitTimeFormat): Long {
    val (min, max) = when (format) {
        DigitTimeFormat.MINUTES_SECONDS_TENTHS -> 100L to 999_000L
        DigitTimeFormat.HOURS_MINUTES_SECONDS  -> 1_000L to 359_999_000L
    }
    val weight = when (format) {
        DigitTimeFormat.MINUTES_SECONDS_TENTHS ->
            longArrayOf(600_000L, 60_000L, 10_000L, 1_000L, 100L)[digitIndex]
        DigitTimeFormat.HOURS_MINUTES_SECONDS  ->
            longArrayOf(36_000_000L, 3_600_000L, 600_000L, 60_000L, 10_000L, 1_000L)[digitIndex]
    }
    return (ms + delta * weight).coerceIn(min, max)
}

@Composable
fun DigitTimePicker(
    valueMs: Long,
    onValueChange: (Long) -> Unit,
    enabled: Boolean = true,
    format: DigitTimeFormat = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
    digitHeight: Dp = 80.dp
) {
    val digits = msToDigits(valueMs, format)
    Row(verticalAlignment = Alignment.CenterVertically) {
        digits.forEachIndexed { i, digit ->
            SingleDigitPicker(
                digit = digit,
                enabled = enabled,
                onIncrement = { onValueChange(applyDelta(valueMs, i, +1, format)) },
                onDecrement = { onValueChange(applyDelta(valueMs, i, -1, format)) },
                digitHeight = digitHeight
            )
            val sep: Char? = when (format) {
                DigitTimeFormat.MINUTES_SECONDS_TENTHS -> when (i) { 1 -> ':'; 3 -> '.'; else -> null }
                DigitTimeFormat.HOURS_MINUTES_SECONDS  -> when (i) { 1 -> ':'; 3 -> ':'; else -> null }
            }
            if (sep != null) {
                Text(
                    text = sep.toString(),
                    fontSize = (digitHeight.value * 0.6f).sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DarkroomRedBright.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SingleDigitPicker(
    digit: Int,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    digitHeight: Dp,
    segOnColor: Color = DarkroomRedBright,
    segOffColor: Color = DarkroomRedFaint
) {
    val digitWidth = digitHeight / 2
    val arrowZoneHeight = digitHeight * 0.35f
    var activeZone by remember { mutableStateOf(0) } // 0=none, 1=increment, -1=decrement
    val currentOnIncrement by rememberUpdatedState(onIncrement)
    val currentOnDecrement by rememberUpdatedState(onDecrement)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(digitWidth)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val swipeThresholdPx = 40.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    activeZone = if (down.position.y < size.height / 2f) 1 else -1
                    var swipeAcc = 0f
                    var isDragging = false
                    val slop = viewConfiguration.touchSlop
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                if (!isDragging) {
                                    if (down.position.y < size.height / 2f) currentOnIncrement()
                                    else currentOnDecrement()
                                }
                                activeZone = 0
                                break
                            }
                            val dy = change.position.y - change.previousPosition.y
                            swipeAcc += dy
                            if (!isDragging && abs(swipeAcc) > slop) isDragging = true
                            if (isDragging) {
                                while (swipeAcc <= -swipeThresholdPx) {
                                    currentOnIncrement()
                                    swipeAcc += swipeThresholdPx
                                }
                                while (swipeAcc >= swipeThresholdPx) {
                                    currentOnDecrement()
                                    swipeAcc -= swipeThresholdPx
                                }
                            }
                            change.consume()
                        }
                    } catch (e: CancellationException) {
                        activeZone = 0
                        throw e
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier.width(digitWidth).height(arrowZoneHeight),
            contentAlignment = Alignment.Center
        ) {
            if (activeZone > 0) {
                Text("▲", color = segOnColor.copy(alpha = 0.55f), fontSize = (arrowZoneHeight.value * 0.55f).sp)
            }
        }

        Box(modifier = Modifier.width(digitWidth).height(digitHeight)) {
            SegmentDisplay(
                digit = digit,
                segOnColor = segOnColor,
                segOffColor = segOffColor,
                modifier = Modifier.fillMaxSize()
            )
            if (activeZone != 0) {
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 2.dp),
                    color = segOnColor.copy(alpha = 0.15f),
                    thickness = 1.dp
                )
            }
        }

        Box(
            modifier = Modifier.width(digitWidth).height(arrowZoneHeight),
            contentAlignment = Alignment.Center
        ) {
            if (activeZone < 0) {
                Text("▼", color = segOnColor.copy(alpha = 0.4f), fontSize = (arrowZoneHeight.value * 0.55f).sp)
            }
        }
    }
}
