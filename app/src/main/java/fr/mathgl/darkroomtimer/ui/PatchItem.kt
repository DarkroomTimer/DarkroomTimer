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
import fr.mathgl.darkroomtimer.ui.theme.DarkroomBlack
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedMedium
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface

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
        isCurrent -> DarkroomRedBright
        isExposed -> DarkroomRedMedium
        else      -> DarkroomRedFaint
    }
    val backgroundColor = when {
        isCurrent -> DarkroomRedDim
        isExposed -> DarkroomSurface
        else      -> DarkroomBlack
    }
    val textColor = when {
        isCurrent -> DarkroomRedBright
        isExposed -> DarkroomRedMedium
        else      -> DarkroomRedFaint
    }

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
                color = textColor
            )
            if (isExposed) {
                Text(
                    text = "✓",
                    fontSize = 18.sp,
                    color = DarkroomRedMedium
                )
            }
        }
    }
}
