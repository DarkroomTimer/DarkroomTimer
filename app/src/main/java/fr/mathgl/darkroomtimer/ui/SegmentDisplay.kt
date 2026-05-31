package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint

// Bits: 0=a(top) 1=b(top-right) 2=c(bot-right) 3=d(bot) 4=e(bot-left) 5=f(top-left) 6=g(mid)
private val SEGMENT_PATTERNS = intArrayOf(
    0b0111111, // 0
    0b0000110, // 1
    0b1011011, // 2
    0b1001111, // 3
    0b1100110, // 4
    0b1101101, // 5
    0b1111101, // 6
    0b0000111, // 7
    0b1111111, // 8
    0b1101111, // 9
)

@Composable
fun SegmentDisplay(
    digit: Int,
    modifier: Modifier = Modifier,
    segOnColor: Color = DarkroomRedBright,
    segOffColor: Color = DarkroomRedFaint,
) {
    val pattern = SEGMENT_PATTERNS[digit.coerceIn(0, 9)]
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = w * 0.14f
        val g = w * 0.04f
        val cr = CornerRadius(t * 0.3f)

        fun seg(bitIndex: Int, left: Float, top: Float, width: Float, height: Float) {
            drawRoundRect(
                color = if ((pattern shr bitIndex) and 1 == 1) segOnColor else segOffColor,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = cr
            )
        }

        val hw = w - 2 * (t + g)  // horizontal segment width
        val vh = h / 2f - t - 2 * g  // vertical segment half-height

        seg(0, t + g,       0f,        hw, t)   // a top
        seg(1, w - t,       t + g,     t,  vh)  // b top-right
        seg(2, w - t,       h/2f + g,  t,  vh)  // c bot-right
        seg(3, t + g,       h - t,     hw, t)   // d bottom
        seg(4, 0f,          h/2f + g,  t,  vh)  // e bot-left
        seg(5, 0f,          t + g,     t,  vh)  // f top-left
        seg(6, t + g,       h/2f - t/2f, hw, t) // g middle
    }
}
