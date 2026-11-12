package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// AL-HADITHI refreshed identity — muted teal, ink and soft botanical neutrals.
val TurquoisePrimaryLight = Color(0xFF39766F)
val TurquoiseSecondaryLight = Color(0xFF568A82)
val EmeraldDeep = Color(0xFF24564F)
val TurquoiseSoftContainer = Color(0xFFDDE8E2)
val OnTurquoiseContainer = Color(0xFF173D38)

// The new wordmark uses deep navy and restrained gold; gold remains a supporting accent.
val HeritageGold = Color(0xFFB88A2E)
val HeritageGoldSoft = Color(0xFFF2E8CC)
val OnHeritageGoldSoft = Color(0xFF5B471B)
val BrandNavy = Color(0xFF102F55)

// Reference-image pastel tones, exposed as shared design tokens for section icons and chips.
val PastelGreenSurface = Color(0xFFDDE8D2)
val PastelBlueSurface = Color(0xFFDCE6F5)
val PastelPurpleSurface = Color(0xFFE7DFF2)
val PastelPeachSurface = Color(0xFFF7E3D5)
val PastelSandSurface = Color(0xFFF2E8CC)
val PastelSageSurface = Color(0xFFE4EFED)
val PastelInk = Color(0xFF172D2B)
val PastelMutedText = Color(0xFF637875)

// Calm light surfaces and borders — avoid heavy shadows or high-contrast separators.
val CalmAlabasterBg = Color(0xFFEAF3F0)
val PureGlassWhite = Color(0xFFFAFDFC)
val SoftSageSurfaceVariant = Color(0xFFE4EFED)
val InkDarkText = Color(0xFF172D2B)
val MutedSlateText = Color(0xFF637875)
val SubtleGlassBorderLight = Color(0x66D6E2DE)

// Deep mode remains available, but uses the same teal/navy family.
val TurquoisePrimaryDark = Color(0xFF8BC5B9)
val TurquoiseSecondaryDark = Color(0xFFA8D6CD)
val NightSanctuaryBg = Color(0xFF101A1B)
val NightGlassSurface = Color(0xFF182526)
val NightSurfaceVariant = Color(0xFF223334)
val NightGoldAccent = Color(0xFFE4C477)
val NightLightText = Color(0xFFEAF3F0)
val NightMutedText = Color(0xFFB1C3BE)
val NightEmeraldAccent = Color(0xFF8BC5B9)
val NightEmeraldSoft = Color(0xFF213D39)
val SubtleGlassBorderDark = Color(0x445D9B90)

// Occasion adaptive accents (subtle and readable in both themes).
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
    lightPrimary = Color(0xFF39766F),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDDE8E2),
    lightOnPrimaryContainer = Color(0xFF173D38),
    lightSecondary = Color(0xFF568A82),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF8BC5B9),
    darkOnPrimary = Color(0xFF173D38),
    darkPrimaryContainer = Color(0xFF213D39),
    darkOnPrimaryContainer = Color(0xFFEAF3F0),
    darkSecondary = Color(0xFFA8D6CD),
    darkOnSecondary = Color(0xFF173D38)
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
