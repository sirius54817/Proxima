package com.sirius.proxima.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ProximaDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    surfaceContainer = Card,
    surfaceContainerHigh = Card,
    surfaceContainerHighest = SurfaceVariant,
    error = DangerRed,
    onError = OnBackground,
)

@Composable
fun ProximaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProximaDarkColorScheme,
        typography = Typography,
        content = content
    )
}