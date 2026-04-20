package com.sirius.proxima.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext

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

private val ProximaLightColorScheme = lightColorScheme(
    primary = ColorPrimaryLight,
    onPrimary = ColorOnPrimaryLight,
    secondary = ColorSecondaryLight,
    onSecondary = ColorOnSecondaryLight,
    background = ColorBackgroundLight,
    onBackground = ColorOnBackgroundLight,
    surface = ColorSurfaceLight,
    onSurface = ColorOnSurfaceLight,
    surfaceVariant = ColorSurfaceVariantLight,
    onSurfaceVariant = ColorOnSurfaceVariantLight,
    outline = ColorOutlineLight,
    surfaceContainer = ColorCardLight,
    surfaceContainerHigh = ColorCardLight,
    surfaceContainerHighest = ColorSurfaceVariantLight,
    error = DangerRed,
    onError = ColorOnBackgroundLight,
)

@Composable
fun ProximaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useMaterial3: Boolean = false,
    useMaterialYou: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useMaterial3 && useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (useMaterial3) {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    } else {
        if (darkTheme) ProximaDarkColorScheme else ProximaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}