package com.example.moodwave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

val PatternPurple = Color(0xFF5546B9)
private val LocalDark = androidx.compose.runtime.compositionLocalOf { false }
object AppThemeState {
    val isDarkMode: Boolean @Composable get() = LocalDark.current
}

val PastelBg @Composable get() = if (AppThemeState.isDarkMode) Color(0xFF121218) else Color(0xFFFAF7F2)
val PastelCard @Composable get() = if (AppThemeState.isDarkMode) Color(0xFF1E1E28) else Color(0xFFFFFFFF)
val PastelText @Composable get() = if (AppThemeState.isDarkMode) Color(0xFFF3F4F6) else Color(0xFF303C38)
val PastelMuted @Composable get() = if (AppThemeState.isDarkMode) Color(0xFF9CA3AF) else Color(0xFF69716D)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    secondary = PastelMint,
    tertiary = PastelPeach,
    background = Color(0xFF121218),
    surface = Color(0xFF1E1E28),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6)
)

private val LightColorScheme = lightColorScheme(
    primary = PatternPurple,
    onPrimary = Color.White,
    secondary = PastelMint,
    tertiary = PastelPeach,
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF303C38),
    onSurface = Color(0xFF303C38)
)

@Composable
fun MoodWaveTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    androidx.compose.runtime.CompositionLocalProvider(LocalDark provides darkTheme) { MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    ) }
}
