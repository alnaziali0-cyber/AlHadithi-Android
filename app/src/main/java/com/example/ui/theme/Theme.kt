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

private val AlHadithiLightColorScheme = lightColorScheme(
    primary = TurquoisePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = TurquoiseSoftContainer,
    onPrimaryContainer = OnTurquoiseContainer,
    secondary = TurquoiseSecondaryLight,
    onSecondary = Color.White,
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
    outlineVariant = Color(0x1F0F766E)
)

private val AlHadithiDarkColorScheme = darkColorScheme(
    primary = TurquoisePrimaryDark,
    onPrimary = Color(0xFF042F2B),
    primaryContainer = Color(0xFF0E3E39),
    onPrimaryContainer = Color(0xFFC7F9F1),
    secondary = TurquoiseSecondaryDark,
    onSecondary = Color(0xFF042F2B),
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
    outlineVariant = Color(0x225EEAD4)
)

val AlHadithiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val LocalReadingFontScale = staticCompositionLocalOf { 1.10f }
val LocalIsDarkThemeActive = staticCompositionLocalOf { false }

@Composable
fun AlHadithiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontChoice: AppFontChoice = AppFontChoice.NASKH,
    readingFontScale: Float = 1.10f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AlHadithiDarkColorScheme else AlHadithiLightColorScheme
    val typography = buildAlHadithiTypography(fontChoice = fontChoice, scale = 1.0f)

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalReadingFontScale provides readingFontScale,
        LocalIsDarkThemeActive provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AlHadithiShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AlHadithiTheme(darkTheme = darkTheme, content = content)
}
