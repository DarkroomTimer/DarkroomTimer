package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PatchItem(
    patchNumber: Int,
    timeMs: Long,
    differentialMs: Long,
    isExposed: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isCurrent -> Color(0xFFCC2200)
        isExposed -> Color(0xFF44AA44)
        else -> Color(0xFF444444)
    }
    val backgroundColor = when {
        isCurrent -> Color(0x11CC2200)
        isExposed -> Color(0x1144AA44)
        else -> Color.Transparent
    }
    val textColor = if (isCurrent) Color.White else Color.White

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Patch $patchNumber",
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${timeMs / 1000.0}s",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "(${differentialMs / 1000.0}s)",
                fontSize = 11.sp,
                color = Color(0xFFAAAAAA)
            )
            if (isExposed) {
                Text(
                    text = "✓",
                    fontSize = 18.sp,
                    color = Color(0xFF44AA44)
                )
            }
        }
    }
}
