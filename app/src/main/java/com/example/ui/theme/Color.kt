package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Spiritual Emerald & Turquoise Identity (Al-Hadithi)
val TurquoisePrimaryLight = Color(0xFF087E91)
val TurquoiseSecondaryLight = Color(0xFF119FB1)
val EmeraldDeep = Color(0xFF155E70)
val TurquoiseSoftContainer = Color(0xFFDDF4F7)
val OnTurquoiseContainer = Color(0xFF063B4A)

// Muted Heritage Gold Accents
val HeritageGold = Color(0xFFB88E35)
val HeritageGoldSoft = Color(0xFFFBF4E2)
val OnHeritageGoldSoft = Color(0xFF543D0A)

// Calm Light Backgrounds & Translucent Glass Surfaces
val CalmAlabasterBg = Color(0xFFF4F7FA)
val PureGlassWhite = Color(0xFFFFFFFF)
val SoftSageSurfaceVariant = Color(0xFFEAF0F5)
val InkDarkText = Color(0xFF102033)
val MutedSlateText = Color(0xFF536579)
val SubtleGlassBorderLight = Color(0x33278CA1)

// Harmonious Spiritual Dark Mode
val TurquoisePrimaryDark = Color(0xFF2DD4BF)
val TurquoiseSecondaryDark = Color(0xFF5EEAD4)
val NightSanctuaryBg = Color(0xFF07111E)
val NightGlassSurface = Color(0xFF111E2D)
val NightSurfaceVariant = Color(0xFF1A2A3B)
val NightGoldAccent = Color(0xFFE4C477)
val NightLightText = Color(0xFFF0F5FB)
val NightMutedText = Color(0xFFAAB8C8)
val NightEmeraldAccent = Color(0xFF4FD7E5)
val NightEmeraldSoft = Color(0xFF143746)
val SubtleGlassBorderDark = Color(0x444FD7E5)

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
    lightPrimary = Color(0xFF26745D),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDCEFE7),
    lightOnPrimaryContainer = Color(0xFF123E31),
    lightSecondary = Color(0xFF3D9277),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF72D5B2),
    darkOnPrimary = Color(0xFF07382B),
    darkPrimaryContainer = Color(0xFF174638),
    darkOnPrimaryContainer = Color(0xFFD3F7E8),
    darkSecondary = Color(0xFF9BE6CD),
    darkOnSecondary = Color(0xFF103D30)
)

val TurquoiseAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF087E91),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDDF4F7),
    lightOnPrimaryContainer = Color(0xFF073B49),
    lightSecondary = Color(0xFF119FB1),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF4FD7E5),
    darkOnPrimary = Color(0xFF063640),
    darkPrimaryContainer = Color(0xFF124352),
    darkOnPrimaryContainer = Color(0xFFD7F8FA),
    darkSecondary = Color(0xFF8CEAF0),
    darkOnSecondary = Color(0xFF063640)
)

val LuminousCyanAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF006F8C),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFD8F5FC),
    lightOnPrimaryContainer = Color(0xFF063747),
    lightSecondary = Color(0xFF168EAD),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF68E4F4),
    darkOnPrimary = Color(0xFF063442),
    darkPrimaryContainer = Color(0xFF164A5B),
    darkOnPrimaryContainer = Color(0xFFE0FAFF),
    darkSecondary = Color(0xFF9CECF5),
    darkOnSecondary = Color(0xFF063442)
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
    AccentChoice.CYAN -> LuminousCyanAccentPalette
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
