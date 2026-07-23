package com.f1telemetry.viewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val F1Red = Color(0xFFE10600)
val F1Dark = Color(0xFF15151E)
val F1Surface = Color(0xFF1F1F2B)
val F1Card = Color(0xFF262633)
val AccentCyan = Color(0xFF00D2FF)
val AccentGreen = Color(0xFF39E75F)
val AccentYellow = Color(0xFFFFD400)
val AccentPurple = Color(0xFFB14EFF)
val TextDim = Color(0xFF9A9AAE)

private val DarkColors = darkColorScheme(
    primary = F1Red,
    onPrimary = Color.White,
    secondary = AccentCyan,
    background = F1Dark,
    onBackground = Color(0xFFECECF2),
    surface = F1Surface,
    onSurface = Color(0xFFECECF2),
    surfaceVariant = F1Card,
    onSurfaceVariant = TextDim,
    error = AccentYellow,
)

private val LightColors = lightColorScheme(
    primary = F1Red,
    secondary = Color(0xFF0077A3),
)

@Composable
fun F1Theme(useDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
