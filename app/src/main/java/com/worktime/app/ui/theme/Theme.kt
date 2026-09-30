package com.worktime.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.worktime.app.domain.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF3568B5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE7F7),
    onPrimaryContainer = Color(0xFF16335F),
    secondary = Color(0xFF5A5B60),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9ECF1),
    onSecondaryContainer = Color(0xFF1D2B45),
    tertiary = Color(0xFF3568B5),
    tertiaryContainer = Color(0xFFDDE7F7),
    onTertiaryContainer = Color(0xFF16335F),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1B1B1D),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1B1B1D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF3F3F4),
    surfaceContainerHigh = Color(0xFFEDEDEF),
    surfaceContainerHighest = Color(0xFFE7E7EA),
    surfaceVariant = Color(0xFFE4E4E8),
    onSurfaceVariant = Color(0xFF5A5B60),
    outline = Color(0xFF9A9BA1),
    outlineVariant = Color(0xFFE6E6EA),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8CCFF),
    onPrimary = Color(0xFF0A315E),
    primaryContainer = Color(0xFF2C4670),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFC6C6CC),
    onSecondary = Color(0xFF2E2E33),
    secondaryContainer = Color(0xFF38383D),
    onSecondaryContainer = Color(0xFFE4E4E8),
    tertiary = Color(0xFFB8CCFF),
    tertiaryContainer = Color(0xFF2C4670),
    onTertiaryContainer = Color(0xFFDCE6FF),
    background = Color(0xFF111113),
    onBackground = Color(0xFFEDEDF0),
    surface = Color(0xFF111113),
    onSurface = Color(0xFFEDEDF0),
    surfaceContainerLowest = Color(0xFF0C0C0E),
    surfaceContainerLow = Color(0xFF18181B),
    surfaceContainer = Color(0xFF1E1E21),
    surfaceContainerHigh = Color(0xFF28282C),
    surfaceContainerHighest = Color(0xFF333337),
    surfaceVariant = Color(0xFF444448),
    onSurfaceVariant = Color(0xFFC9C9CE),
    outline = Color(0xFF9A9AA0),
    outlineVariant = Color(0xFF3F3F44),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Immutable
class WorkTimeSemanticColors(
    val positive: Color,
    val negative: Color,
)

private val LightSemanticColors = WorkTimeSemanticColors(
    positive = Color(0xFF1E7B45),
    negative = Color(0xFFBA1A1A),
)

private val DarkSemanticColors = WorkTimeSemanticColors(
    positive = Color(0xFF7FD6A0),
    negative = Color(0xFFFFB4AB),
)

val LocalWorkTimeColors = staticCompositionLocalOf { LightSemanticColors }

val MaterialTheme.semanticColors: WorkTimeSemanticColors
    @Composable get() = LocalWorkTimeColors.current

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Default Material typography with tabular numerals so money/time values never jitter. */
private val WorkTimeTypography: Typography = run {
    val base = Typography()
    base.copy(
        displayLarge = base.displayLarge.tabular(),
        displayMedium = base.displayMedium.tabular(),
        displaySmall = base.displaySmall.tabular(),
        headlineLarge = base.headlineLarge.tabular(),
        headlineMedium = base.headlineMedium.tabular(),
        headlineSmall = base.headlineSmall.tabular(),
        titleLarge = base.titleLarge.tabular(),
        titleMedium = base.titleMedium.tabular(),
        titleSmall = base.titleSmall.tabular(),
        bodyLarge = base.bodyLarge.tabular(),
        bodyMedium = base.bodyMedium.tabular(),
        bodySmall = base.bodySmall.tabular(),
        labelLarge = base.labelLarge.tabular(),
        labelMedium = base.labelMedium.tabular(),
        labelSmall = base.labelSmall.tabular(),
    )
}

private val WorkTimeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun WorkTimeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val lightSystemBars = colorScheme.background.luminance() > 0.5f
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightSystemBars
            controller.isAppearanceLightNavigationBars = lightSystemBars
        }
    }

    CompositionLocalProvider(
        LocalWorkTimeColors provides if (darkTheme) DarkSemanticColors else LightSemanticColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WorkTimeTypography,
            shapes = WorkTimeShapes,
            content = content,
        )
    }
}
