package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private fun lightSchemeFor(choice: AccentChoice): androidx.compose.material3.ColorScheme {
    val accent = accentPaletteFor(choice)
    return lightColorScheme(
        primary = accent.lightPrimary,
        onPrimary = accent.lightOnPrimary,
        primaryContainer = accent.lightPrimaryContainer,
        onPrimaryContainer = accent.lightOnPrimaryContainer,
        secondary = accent.lightSecondary,
        onSecondary = accent.lightOnSecondary,
        secondaryContainer = SoftSageSurfaceVariant,
        onSecondaryContainer = OnTurquoiseContainer,
        tertiary = HeritageGold,
        onTertiary = Color.White,
        tertiaryContainer = HeritageGoldSoft,
        onTertiaryContainer = OnHeritageGoldSoft,
        background = CalmAlabasterBg,
        onBackground = InkDarkText,
        surface = PureGlassWhite,
        onSurface = InkDarkText,
        surfaceVariant = SoftSageSurfaceVariant,
        onSurfaceVariant = MutedSlateText,
        outline = SubtleGlassBorderLight,
        outlineVariant = Color(0xFFC8D4D0)
    )
}

private fun darkSchemeFor(choice: AccentChoice): androidx.compose.material3.ColorScheme {
    val accent = accentPaletteFor(choice)
    return darkColorScheme(
        primary = accent.darkPrimary,
        onPrimary = accent.darkOnPrimary,
        primaryContainer = accent.darkPrimaryContainer,
        onPrimaryContainer = accent.darkOnPrimaryContainer,
        secondary = accent.darkSecondary,
        onSecondary = accent.darkOnSecondary,
        secondaryContainer = NightSurfaceVariant,
        onSecondaryContainer = NightLightText,
        tertiary = NightGoldAccent,
        onTertiary = Color(0xFF3A2804),
        tertiaryContainer = Color(0xFF35280D),
        onTertiaryContainer = Color(0xFFF9E2AC),
        background = NightSanctuaryBg,
        onBackground = NightLightText,
        surface = NightGlassSurface,
        onSurface = NightLightText,
        surfaceVariant = NightSurfaceVariant,
        onSurfaceVariant = NightMutedText,
        outline = SubtleGlassBorderDark,
        outlineVariant = Color(0xFF3A5550)
    )
}

private val CompactShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val ComfortableShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val AlHadithiShapes = ComfortableShapes

val LocalReadingFontScale = staticCompositionLocalOf { 1.10f }
val LocalIsDarkThemeActive = staticCompositionLocalOf { false }
val LocalCardDensity = staticCompositionLocalOf { CardDensity.COMFORTABLE }
val LocalLineSpacing = staticCompositionLocalOf { LineSpacingOption.NORMAL }
val LocalReadingFontChoice = staticCompositionLocalOf { ReadingFontChoice.MUHAMMADI }

/**
 * Spacing tokens driven by the user's density preference so that a compact layout really is
 * denser everywhere instead of only in a few screens.
 */
object DensityTokens {
    fun cardPadding(density: CardDensity) = when (density) {
        CardDensity.COMFORTABLE -> 16.dp
        CardDensity.COMPACT -> 11.dp
    }

    fun cardSpacing(density: CardDensity) = when (density) {
        CardDensity.COMFORTABLE -> 14.dp
        CardDensity.COMPACT -> 8.dp
    }

    fun listSpacing(density: CardDensity) = when (density) {
        CardDensity.COMFORTABLE -> 14.dp
        CardDensity.COMPACT -> 8.dp
    }
}

/**
 * The single entry point of the app design system.
 *
 * @param themeMode light / dark / follow the system.
 * @param accentChoice the primary colour family; the heritage gold tertiary stays untouched.
 * @param fontChoice the UI font family.
 * @param uiFontScale multiplier applied to every typography size.
 * @param readingFontScale multiplier applied to long-form reading texts.
 */
@Composable
fun AlHadithiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentChoice: AccentChoice = AccentChoice.TURQUOISE,
    fontChoice: AppFontChoice = AppFontChoice.NASKH,
    readingFontChoice: ReadingFontChoice = ReadingFontChoice.MUHAMMADI,
    uiFontScale: Float = 1.0f,
    readingFontScale: Float = 1.10f,
    cardDensity: CardDensity = CardDensity.COMFORTABLE,
    lineSpacing: LineSpacingOption = LineSpacingOption.NORMAL,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }
    val colorScheme = if (isDark) darkSchemeFor(accentChoice) else lightSchemeFor(accentChoice)
    val typography = buildAlHadithiTypography(fontChoice = fontChoice, scale = uiFontScale)
    val shapes = when (cardDensity) {
        CardDensity.COMFORTABLE -> ComfortableShapes
        CardDensity.COMPACT -> CompactShapes
    }

    CompositionLocalProvider(
        // The whole product is Arabic-first, so the layout direction is pinned to RTL.
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalReadingFontScale provides readingFontScale,
        LocalIsDarkThemeActive provides isDark,
        LocalCardDensity provides cardDensity,
        LocalLineSpacing provides lineSpacing,
        LocalReadingFontChoice provides readingFontChoice
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentChoice: AccentChoice = AccentChoice.TURQUOISE,
    fontChoice: AppFontChoice = AppFontChoice.NASKH,
    readingFontChoice: ReadingFontChoice = ReadingFontChoice.MUHAMMADI,
    uiFontScale: Float = 1.0f,
    readingFontScale: Float = 1.10f,
    cardDensity: CardDensity = CardDensity.COMFORTABLE,
    lineSpacing: LineSpacingOption = LineSpacingOption.NORMAL,
    content: @Composable () -> Unit
) {
    AlHadithiTheme(
        themeMode = themeMode,
        darkTheme = darkTheme,
        accentChoice = accentChoice,
        fontChoice = fontChoice,
        readingFontChoice = readingFontChoice,
        uiFontScale = uiFontScale,
        readingFontScale = readingFontScale,
        cardDensity = cardDensity,
        lineSpacing = lineSpacing,
        content = content
    )
}
