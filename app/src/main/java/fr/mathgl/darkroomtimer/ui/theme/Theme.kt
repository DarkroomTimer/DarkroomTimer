package fr.mathgl.darkroomtimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkroomColorScheme = darkColorScheme(
    primary        = DarkroomRedBright,
    onPrimary      = DarkroomBlack,
    background     = DarkroomBlack,
    onBackground   = DarkroomRedBright,
    surface        = DarkroomSurface,
    onSurface      = DarkroomRedBright,
    secondary      = DarkroomRedMedium,
    onSecondary    = DarkroomBlack,
    tertiary       = DarkroomRedDim,
    onTertiary     = DarkroomBlack,
    surfaceVariant = DarkroomSurfaceElevated,
    outline        = DarkroomRedFaint,
    error          = DarkroomRedBright,
    onError        = DarkroomBlack,
)

@Composable
fun DarkroomTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkroomColorScheme,
        typography = Typography,
        content = content
    )
}