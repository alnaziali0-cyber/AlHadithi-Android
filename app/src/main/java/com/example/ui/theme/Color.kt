package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Spiritual Emerald & Turquoise Identity (Al-Hadithi)
val TurquoisePrimaryLight = Color(0xFF0F766E)
val TurquoiseSecondaryLight = Color(0xFF0D9488)
val EmeraldDeep = Color(0xFF115E59)
val TurquoiseSoftContainer = Color(0xFFDFF4F1)
val OnTurquoiseContainer = Color(0xFF063F3A)

// Muted Heritage Gold Accents
val HeritageGold = Color(0xFFB88E35)
val HeritageGoldSoft = Color(0xFFFBF4E2)
val OnHeritageGoldSoft = Color(0xFF543D0A)

// Calm Light Backgrounds & Translucent Glass Surfaces
val CalmAlabasterBg = Color(0xFFFFFFFF)
val PureGlassWhite = Color(0xFFFFFFFF)
val SoftSageSurfaceVariant = Color(0xFFE6F0EE)
val InkDarkText = Color(0xFF102422)
val MutedSlateText = Color(0xFF496561)
val SubtleGlassBorderLight = Color(0x330F766E)

// Harmonious Spiritual Dark Mode
val TurquoisePrimaryDark = Color(0xFF2DD4BF)
val TurquoiseSecondaryDark = Color(0xFF5EEAD4)
val NightSanctuaryBg = Color(0xFF091413)
val NightGlassSurface = Color(0xFF112321)
val NightSurfaceVariant = Color(0xFF19322F)
val NightGoldAccent = Color(0xFFE2BA64)
val NightLightText = Color(0xFFEAF5F3)
val NightMutedText = Color(0xFF9BB8B4)
val SubtleGlassBorderDark = Color(0x335EEAD4)

// Occasion Adaptive Accents (Subtle, respectful, never jarring)
val MourningBannerBgLight = Color(0xFFF6ECEF)
val MourningBannerTextLight = Color(0xFF5B1F2E)
val MourningBannerBgDark = Color(0xFF26151B)
val MourningBannerTextDark = Color(0xFFF4C6D2)

val JoyBannerBgLight = Color(0xFFECF8F4)
val JoyBannerTextLight = Color(0xFF0B5345)
val JoyBannerBgDark = Color(0xFF112B25)
val JoyBannerTextDark = Color(0xFF9FE8D6)

/**
 * Accent families offered in the appearance settings.
 *
 * Every palette keeps the calm background surfaces and the heritage gold tertiary so that the
 * identity of Al-Hadithi is preserved; only the primary/secondary family changes. Contrast
 * against the paired `on*` colour was chosen to stay at or above the WCAG AA ratio for body text.
 */
data class AccentPalette(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val lightOnSecondary: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    val darkOnSecondary: Color
)

val EmeraldAccentPalette = AccentPalette(
    lightPrimary = TurquoisePrimaryLight,
    lightOnPrimary = Color.White,
    lightPrimaryContainer = TurquoiseSoftContainer,
    lightOnPrimaryContainer = OnTurquoiseContainer,
    lightSecondary = TurquoiseSecondaryLight,
    lightOnSecondary = Color.White,
    darkPrimary = TurquoisePrimaryDark,
    darkOnPrimary = Color(0xFF042F2B),
    darkPrimaryContainer = Color(0xFF0E3E39),
    darkOnPrimaryContainer = Color(0xFFC7F9F1),
    darkSecondary = TurquoiseSecondaryDark,
    darkOnSecondary = Color(0xFF042F2B)
)

val TurquoiseAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF0E7490),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFCFF7FB),
    lightOnPrimaryContainer = Color(0xFF083344),
    lightSecondary = Color(0xFF0891B2),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF22D3EE),
    darkOnPrimary = Color(0xFF083344),
    darkPrimaryContainer = Color(0xFF164E63),
    darkOnPrimaryContainer = Color(0xFFCFFAFE),
    darkSecondary = Color(0xFF67E8F9),
    darkOnSecondary = Color(0xFF083344)
)

val GoldAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF8C6D1F),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = HeritageGoldSoft,
    lightOnPrimaryContainer = OnHeritageGoldSoft,
    lightSecondary = Color(0xFF7A5C17),
    lightOnSecondary = Color.White,
    darkPrimary = NightGoldAccent,
    darkOnPrimary = Color(0xFF3A2804),
    darkPrimaryContainer = Color(0xFF35280D),
    darkOnPrimaryContainer = Color(0xFFF9E2AC),
    darkSecondary = Color(0xFFD8B45C),
    darkOnSecondary = Color(0xFF3A2804)
)

val NightBlueAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF2C4A7C),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDCE6F5),
    lightOnPrimaryContainer = Color(0xFF1B2C4A),
    lightSecondary = Color(0xFF3D5F99),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF93C5FD),
    darkOnPrimary = Color(0xFF1E3A5F),
    darkPrimaryContainer = Color(0xFF1E3350),
    darkOnPrimaryContainer = Color(0xFFDBEAFE),
    darkSecondary = Color(0xFFA5C9F5),
    darkOnSecondary = Color(0xFF1E3A5F)
)

fun accentPaletteFor(choice: AccentChoice): AccentPalette = when (choice) {
    AccentChoice.EMERALD -> EmeraldAccentPalette
    AccentChoice.TURQUOISE -> TurquoiseAccentPalette
    AccentChoice.GOLD -> GoldAccentPalette
    AccentChoice.NIGHT_BLUE -> NightBlueAccentPalette
}

/**
 * Backdrops used by the continuous Quran reader. Each pair keeps the ayah text readable and
 * never relies on colour alone to convey meaning.
 */
data class ReaderBackdrop(
    val background: Color,
    val onBackground: Color,
    val markerColor: Color,
    val surfaceTint: Color
)

fun readerBackdropFor(
    style: QuranBackgroundStyle,
    isDark: Boolean
): ReaderBackdrop = when {
    isDark -> ReaderBackdrop(
        background = Color(0xFF0D1513),
        onBackground = Color(0xFFE7EFEC),
        markerColor = Color(0xFFE2BA64),
        surfaceTint = Color(0xFF16211F)
    )

    style == QuranBackgroundStyle.MUSHAF -> ReaderBackdrop(
        background = Color(0xFFF7F2E4),
        onBackground = Color(0xFF1B1A14),
        markerColor = Color(0xFF9A7A2B),
        surfaceTint = Color(0xFFEFE6D0)
    )

    style == QuranBackgroundStyle.SEPIA -> ReaderBackdrop(
        background = Color(0xFFF4EDE2),
        onBackground = Color(0xFF2A2118),
        markerColor = Color(0xFF8A6A2A),
        surfaceTint = Color(0xFFE9DFCE)
    )

    else -> ReaderBackdrop(
        background = Color(0xFFFCFDFC),
        onBackground = Color(0xFF12201E),
        markerColor = Color(0xFF0F766E),
        surfaceTint = Color(0xFFF1F6F5)
    )
}
