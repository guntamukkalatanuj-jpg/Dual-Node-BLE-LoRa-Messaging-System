package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BluCyan,
    onPrimary = BluBlack,
    primaryContainer = BluSurfaceElevated,
    onPrimaryContainer = BluTextPrimary,
    secondary = BluElectricBright,
    onSecondary = Color.White,
    secondaryContainer = BluSurface,
    onSecondaryContainer = BluTextCyan,
    tertiary = BluCyanGlow,
    onTertiary = BluBlack,
    background = BluBlack,
    onBackground = BluTextPrimary,
    surface = BluDarkNavy,
    onSurface = BluTextPrimary,
    surfaceVariant = BluSurface,
    onSurfaceVariant = BluTextSecondary,
    outline = BluBorder,
    outlineVariant = BluBorderSubtle,
    error = BluError,
    onError = Color.White
)

@Composable
fun BluLinqTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BluLinqTheme(content = content)
}
