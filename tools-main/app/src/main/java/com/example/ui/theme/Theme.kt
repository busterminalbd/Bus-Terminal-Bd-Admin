package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun createDarkColorScheme(palette: AppThemePalette) = darkColorScheme(
    primary = palette.darkPrimaryAccent,
    onPrimary = Color.Black,
    primaryContainer = DarkElevatedSurface,
    onPrimaryContainer = DarkTextPrimary,
    secondary = palette.accent,
    onSecondary = Color.Black,
    tertiary = palette.accent,
    onTertiary = Color.Black,
    background = DarkMainBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderColor,
    outlineVariant = DarkBorderColor,
    error = DarkErrorRed,
    onError = Color.White
)

private fun createLightColorScheme(palette: AppThemePalette) = lightColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.container,
    onPrimaryContainer = palette.onContainer,
    secondary = palette.darkPrimary,
    onSecondary = Color.White,
    tertiary = palette.accent,
    onTertiary = Color.White,
    background = MainBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = palette.container.copy(alpha = 0.45f),
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "system",
    themeColor: String = "emerald",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = getThemePalette(themeColor)
    val colorScheme = if (darkTheme) createDarkColorScheme(palette) else createLightColorScheme(palette)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

