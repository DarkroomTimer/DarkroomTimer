package fr.mathgl.darkroomtimer.ui.exposure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint

@Composable
fun RelayButton(
    label: String,
    isOn: Boolean,
    hasOverride: Boolean,
    clickEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isOn) DarkroomRedBright else Color.Transparent
    val contentColor = if (isOn) Color.Black else DarkroomRedDim
    val borderColor = if (clickEnabled && isOn) DarkroomRedBright else DarkroomRedFaint
    OutlinedButton(
        onClick = onClick,
        enabled = clickEnabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = DarkroomRedFaint
        ),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 11.sp)
            if (hasOverride) {
                Text(text = "override", fontSize = 8.sp)
            }
        }
    }
}
