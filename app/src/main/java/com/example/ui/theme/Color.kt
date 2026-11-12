package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// KHAZAIN AL-HUDA refreshed identity — muted teal, ink and soft botanical neutrals.
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
val CalmAlabasterBg = Color(0xFFF7F8F5)
val PureGlassWhite = Color(0xFFFFFFFF)
val SoftSageSurfaceVariant = Color(0xFFF1F4F1)
val InkDarkText = Color(0xFF18221D)
val MutedSlateText = Color(0xFF68766F)
val SubtleGlassBorderLight = Color(0xFFDCE3DE)

// Deep mode remains available, but uses the same teal/navy family.
//
// The night palette is a "soft black": near-black but never a dead #000000 slab. Keeping a hint
// of green-teal in the dark surfaces removes the harsh contrast of a pure-black app and keeps the
// cards readable without heavy borders.
val TurquoisePrimaryDark = Color(0xFF4D9A8C)
val TurquoiseSecondaryDark = Color(0xFF8BC8B8)
val AppSoftBlackBackground = Color(0xFF111715)
val NightSanctuaryBg = AppSoftBlackBackground
val NightGlassSurface = Color(0xFF1A211E)
val NightSurfaceVariant = Color(0xFF212A26)
val NightGoldAccent = Color(0xFFC6A15B)
val NightLightText = Color(0xFFF4F5F2)
val NightMutedText = Color(0xFFA8B1AC)
val NightEmeraldAccent = Color(0xFF4D9A8C)
val NightEmeraldSoft = Color(0xFF233A32)
val SubtleGlassBorderDark = Color(0xFF2B3530)

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
 * Shared semantic status colours.
 *
 * They live here (instead of being re-declared inside screens) so the "completed", "warning" and
 * "error" states always read the same across the dashboard, the settings and the developer
 * panel, in both light and dark mode.
 */
val StatusSuccessLight = Color(0xFF1F7A4D)
val StatusSuccessDark = Color(0xFF5CC38C)
val StatusWarningLight = Color(0xFF9A6410)
val StatusWarningDark = Color(0xFFE0B055)
val StatusErrorLight = Color(0xFFB3261E)
val StatusErrorDark = Color(0xFFF2B8B5)

/** Container colours for the same semantic states (used for soft badges and banners). */
val StatusSuccessContainerLight = Color(0xFFDDF0E4)
val StatusSuccessContainerDark = Color(0xFF17301F)
val StatusWarningContainerLight = Color(0xFFFBEEDA)
val StatusWarningContainerDark = Color(0xFF332611)
val StatusErrorContainerLight = Color(0xFFFBE4E2)
val StatusErrorContainerDark = Color(0xFF3A1A19)

/**
 * Accent families offered in the appearance settings.
 *
 * Every palette keeps the calm background surfaces and the heritage gold tertiary so that the
 * identity of Khazain Al-Huda is preserved; only the primary/secondary family changes. Contrast
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
    darkPrimary = Color(0xFF5AA88D),
    darkOnPrimary = Color(0xFF0D1B15),
    darkPrimaryContainer = Color(0xFF243B32),
    darkOnPrimaryContainer = Color(0xFFDDF3E8),
    darkSecondary = Color(0xFF8BC8B8),
    darkOnSecondary = Color(0xFF13221C)
)

val TurquoiseAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF39766F),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDDE8E2),
    lightOnPrimaryContainer = Color(0xFF173D38),
    lightSecondary = Color(0xFF568A82),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF4D9A8C),
    darkOnPrimary = Color(0xFF081A15),
    darkPrimaryContainer = Color(0xFF223B34),
    darkOnPrimaryContainer = Color(0xFFDDF2EB),
    darkSecondary = Color(0xFF8BC8B8),
    darkOnSecondary = Color(0xFF10231D)
)

val LuminousCyanAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF006F8C),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFD8F5FC),
    lightOnPrimaryContainer = Color(0xFF063747),
    lightSecondary = Color(0xFF168EAD),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF519C9A),
    darkOnPrimary = Color(0xFF0B1D1C),
    darkPrimaryContainer = Color(0xFF213A39),
    darkOnPrimaryContainer = Color(0xFFD7F0EE),
    darkSecondary = Color(0xFF8BC8B8),
    darkOnSecondary = Color(0xFF12231F)
)

val GoldAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF8C6D1F),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = HeritageGoldSoft,
    lightOnPrimaryContainer = OnHeritageGoldSoft,
    lightSecondary = Color(0xFF7A5C17),
    lightOnSecondary = Color.White,
    darkPrimary = NightGoldAccent,
    darkOnPrimary = Color(0xFF211A0E),
    darkPrimaryContainer = Color(0xFF3C321F),
    darkOnPrimaryContainer = Color(0xFFF3E6C7),
    darkSecondary = Color(0xFFD8B875),
    darkOnSecondary = Color(0xFF211A0E)
)

val NightBlueAccentPalette = AccentPalette(
    lightPrimary = Color(0xFF2C4A7C),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDCE6F5),
    lightOnPrimaryContainer = Color(0xFF1B2C4A),
    lightSecondary = Color(0xFF3D5F99),
    lightOnSecondary = Color.White,
    darkPrimary = Color(0xFF879BB7),
    darkOnPrimary = Color(0xFF151E2A),
    darkPrimaryContainer = Color(0xFF2A3543),
    darkOnPrimaryContainer = Color(0xFFE1E8F1),
    darkSecondary = Color(0xFFB0BCD0),
    darkOnSecondary = Color(0xFF1A2430)
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
        background = NightSanctuaryBg,
        onBackground = NightLightText,
        markerColor = NightGoldAccent,
        surfaceTint = NightGlassSurface
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
