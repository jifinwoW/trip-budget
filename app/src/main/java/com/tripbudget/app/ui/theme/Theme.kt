package com.tripbudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tripbudget.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Sea600,
    onPrimary = Color.White,
    primaryContainer = Sea100,
    onPrimaryContainer = Spruce900,
    secondary = Spruce800,
    onSecondary = Color.White,
    secondaryContainer = Mist100,
    onSecondaryContainer = Spruce900,
    tertiary = Oxide600,
    onTertiary = Color.White,
    tertiaryContainer = Oxide100,
    onTertiaryContainer = Color(0xFF5C180C),
    background = Mist50,
    onBackground = Ink900,
    surface = Paper,
    onSurface = Ink900,
    surfaceVariant = Mist100,
    onSurfaceVariant = Ink600,
    error = Oxide600,
    onError = Color.White,
    outline = Hairline,
    outlineVariant = Color(0xFFD0D8D4),
)

private val DarkColors = darkColorScheme(
    primary = Sea400,
    onPrimary = Spruce950,
    primaryContainer = Spruce700,
    onPrimaryContainer = Sea100,
    secondary = Color(0xFF9BB5AB),
    onSecondary = Spruce950,
    secondaryContainer = NightElevated,
    onSecondaryContainer = Mist100,
    tertiary = Color(0xFFFF8F75),
    onTertiary = Color(0xFF4A1208),
    tertiaryContainer = Color(0xFF7A2A18),
    onTertiaryContainer = Oxide100,
    background = NightBg,
    onBackground = Color(0xFFE6EEEA),
    surface = NightSurface,
    onSurface = Color(0xFFE6EEEA),
    surfaceVariant = NightElevated,
    onSurfaceVariant = Color(0xFFB0C0B8),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF690005),
    outline = Color(0xFF6F8178),
    outlineVariant = Color(0xFF3A4A44),
)

@Composable
fun TripBudgetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = TripBudgetTypography,
        shapes = TripBudgetShapes,
        content = content,
    )
}

/** Semantic aliases used by ledger components. */
object LedgerTokens {
    val positive: Color get() = Sea600
    val negative: Color get() = Oxide600
    val positiveDark: Color get() = Sea400
}
