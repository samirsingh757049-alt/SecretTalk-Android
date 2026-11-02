package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = IosBlueDark,
    onPrimaryContainer = Color.White,
    secondary = IosGreen,
    onSecondary = Color.White,
    tertiary = IosOrange,
    onTertiary = Color.White,
    error = IosRed,
    onError = Color.White,
    background = IosBackground,
    onBackground = IosTextPrimary,
    surface = IosSurface,
    onSurface = IosTextPrimary,
    surfaceVariant = IosSurfaceVariant,
    onSurfaceVariant = IosTextSecondary,
    outline = IosDivider
)

@Composable
fun SecretTalkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
