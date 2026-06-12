package fr.mathgl.darkroomtimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkroomColorScheme = darkColorScheme(
    primary                  = DarkroomRedBright,
    onPrimary                = DarkroomBlack,
    background               = DarkroomBlack,
    onBackground             = DarkroomRedBright,
    surface                  = DarkroomSurface,
    onSurface                = DarkroomRedBright,
    onSurfaceVariant         = DarkroomRedDim,
    secondary                = DarkroomRedMedium,
    onSecondary              = DarkroomBlack,
    tertiary                 = DarkroomRedDim,
    onTertiary               = DarkroomBlack,
    surfaceVariant           = DarkroomSurfaceElevated,
    surfaceContainer         = DarkroomSurface,
    surfaceContainerLow      = DarkroomBlack,
    surfaceContainerLowest   = DarkroomBlack,
    surfaceContainerHigh     = DarkroomSurfaceElevated,
    surfaceContainerHighest  = DarkroomSurfaceElevated,
    surfaceBright            = DarkroomSurfaceElevated,
    surfaceDim               = DarkroomBlack,
    inverseSurface           = DarkroomRedDim,
    inverseOnSurface         = DarkroomBlack,
    inversePrimary           = DarkroomRedDim,
    outline                  = DarkroomRedFaint,
    outlineVariant           = DarkroomRedFaint,
    scrim                    = DarkroomBlack,
    error                    = DarkroomRedBright,
    onError                  = DarkroomBlack,
)

@Composable
fun DarkroomTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkroomColorScheme,
        typography = Typography,
        content = content
    )
}