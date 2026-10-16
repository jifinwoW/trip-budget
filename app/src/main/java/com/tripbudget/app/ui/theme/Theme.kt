package com.tripbudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tripbudget.app.data.ThemeMode

val LocalIsDarkTheme = staticCompositionLocalOf { false }

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
    CompositionLocalProvider(LocalIsDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = TripBudgetTypography,
            shapes = TripBudgetShapes,
            content = content,
        )
    }
}

/** Semantic aliases for coastal UI — light pastels adapt in dark mode. */
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

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    val ink: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val muted: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val card: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surface

    val canvas: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background

    val softFill: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) NightElevated else Color(0xFFF3F8F7)

    val softFillAlt: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF243835) else Color(0xFFEAF5F4)

    val progressTrack: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF2A3F3B) else Color(0xFFDCEAE6)

    val progressFill: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF5ED4C2) else Color(0xFF0D8B8A)

    val accent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val danger: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val dangerSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF4A221C) else Color(0xFFFCE8E4)

    val settledSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF1A3D32) else Color(0xFFD8F3E8)

    val settledFg: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF6FDBB0) else Color(0xFF2F8F6B)

    val peachSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF3D2A1F) else Peach

    val sandSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF3A3224) else Sand

    val tealSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF1A3D38) else Teal100

    val tealSoftFg: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF7EE0D0) else Teal700

    val chipNeutral: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF2A3835) else Color(0xFFE8EEF0)

    data class SoftTile(
        val background: Color,
        val iconBg: Color,
        val iconTint: Color,
        val amount: Color,
    )

    @Composable
    @ReadOnlyComposable
    fun tileMint(): SoftTile = if (isDark) {
        SoftTile(Color(0xFF1A3A36), Color(0xFF244E48), Color(0xFF5ED4C2), Color(0xFFE6F2EF))
    } else {
        SoftTile(Color(0xFFD8F3EE), Color(0xFFB5E6DC), Color(0xFF0D8B8A), Charcoal900)
    }

    @Composable
    @ReadOnlyComposable
    fun tileRose(): SoftTile = if (isDark) {
        SoftTile(Color(0xFF3A2428), Color(0xFF4A3034), Color(0xFFFF8FA0), Color(0xFFE6F2EF))
    } else {
        SoftTile(Color(0xFFF6DCE0), Color(0xFFF0C4CA), Color(0xFFC45B66), Charcoal900)
    }

    @Composable
    @ReadOnlyComposable
    fun tileBlue(): SoftTile = if (isDark) {
        SoftTile(Color(0xFF1E3040), Color(0xFF2A4054), Color(0xFF7EB8E0), Color(0xFFE6F2EF))
    } else {
        SoftTile(Color(0xFFD7EAF7), Color(0xFFBDDCF0), Color(0xFF2F7DB8), Charcoal900)
    }

    @Composable
    @ReadOnlyComposable
    fun tileSand(): SoftTile = if (isDark) {
        SoftTile(Color(0xFF3A3224), Color(0xFF4A4030), Color(0xFFE0B86A), Color(0xFFE6F2EF))
    } else {
        SoftTile(Color(0xFFF3E8D4), Color(0xFFE6D4B0), Color(0xFFB07A2E), Charcoal900)
    }

    @Composable
    @ReadOnlyComposable
    fun tilePeach(): SoftTile = if (isDark) {
        SoftTile(Color(0xFF3D2A1F), Color(0xFF4E382C), Color(0xFFFFA070), Color(0xFFE6F2EF))
    } else {
        SoftTile(Color(0xFFF8E0D0), Color(0xFFF0C8B4), Color(0xFFD0653A), Charcoal900)
    }

    @Composable
    @ReadOnlyComposable
    fun tileWarm(): SoftTile = if (isDark) {
        SoftTile(Color(0xFF3A2C26), Color(0xFF4A3A32), Color(0xFFE09A7A), Color(0xFFE6F2EF))
    } else {
        SoftTile(Color(0xFFF6E4DC), Color(0xFFEED0C4), Color(0xFFC47A5A), Charcoal900)
    }
}
