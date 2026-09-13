package com.worktime.app.modern.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.worktime.app.modern.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF315D7B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E9FF),
    onPrimaryContainer = Color(0xFF0B3148),
    secondary = Color(0xFF536875),
    secondaryContainer = Color(0xFFD7E5EE),
    tertiary = Color(0xFF6C5B75),
    surface = Color(0xFFF8F9FC),
    surfaceVariant = Color(0xFFE2E8ED),
    background = Color(0xFFF8F9FC),
    outline = Color(0xFF737A80),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCCF0),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF174B67),
    onPrimaryContainer = Color(0xFFCDE9FF),
    secondary = Color(0xFFBAC9D2),
    secondaryContainer = Color(0xFF3B4A53),
    tertiary = Color(0xFFD4BDE0),
    surface = Color(0xFF111417),
    surfaceVariant = Color(0xFF252A2E),
    background = Color(0xFF111417),
    outline = Color(0xFF8D9296),
)

@Composable
fun ModernWorkTimeTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
