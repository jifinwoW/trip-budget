package com.tripbudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tripbudget.app.data.ThemeMode

// SoftCanvas is defined in Color.kt (same package).

private val LightColors = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal700,
    secondary = Teal700,
    onSecondary = Color.White,
    secondaryContainer = AquaMistDeep,
    onSecondaryContainer = Charcoal900,
    tertiary = Coral600,
    onTertiary = Color.White,
    tertiaryContainer = Coral100,
    onTertiaryContainer = Color(0xFF5C180C),
    background = SoftCanvas,
    onBackground = Charcoal900,
    surface = PaperWhite,
    onSurface = Charcoal900,
    surfaceVariant = AquaMistDeep,
    onSurfaceVariant = Charcoal600,
    error = Coral600,
    onError = Color.White,
    outline = HairlineSoft,
    outlineVariant = Color(0xFFD5E5E1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5ED4C2),
    onPrimary = NightBg,
    primaryContainer = Color(0xFF0F5C52),
    onPrimaryContainer = Teal100,
    secondary = Color(0xFF9BC9C0),
    onSecondary = NightBg,
    secondaryContainer = NightElevated,
    onSecondaryContainer = AquaMistDeep,
    tertiary = Color(0xFFFF8F75),
    onTertiary = Color(0xFF4A1208),
    tertiaryContainer = Color(0xFF7A2A18),
    onTertiaryContainer = Coral100,
    background = NightBg,
    onBackground = Color(0xFFE6F2EF),
    surface = NightSurface,
    onSurface = Color(0xFFE6F2EF),
    surfaceVariant = NightElevated,
    onSurfaceVariant = Color(0xFFB0C8C2),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF690005),
    outline = Color(0xFF6F8A84),
    outlineVariant = Color(0xFF3A4F4A),
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

/** Semantic aliases for coastal UI components. */
object CoastalTokens {
    val positive: Color get() = Teal600
    val negative: Color get() = Coral600
    val mintHero: Color get() = MintHero
    val paleBlue: Color get() = PaleBlue
    val sand: Color get() = Sand
    val softRose: Color get() = SoftRose
    val peach: Color get() = Peach
    val aquaMist: Color get() = AquaMist
    val softCanvas: Color get() = SoftCanvas
    val settled: Color get() = Color(0xFF2F8F6B)
    val settledBg: Color get() = Color(0xFFD8F3E8)
}
