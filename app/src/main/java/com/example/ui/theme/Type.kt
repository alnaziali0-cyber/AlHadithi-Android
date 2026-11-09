package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

enum class AppFontChoice(val key: String, val arabicLabel: String) {
    NASKH("NASKH", "نسخ واضح (افتراضي)"),
    TAJAWAL("TAJAWAL", "تجوّل"),
    AMIRI("AMIRI", "أميري تراثي"),
    KUFI("KUFI", "كوفي حديث");

    companion object {
        fun fromKey(key: String): AppFontChoice =
            entries.firstOrNull { it.key == key } ?: NASKH
    }
}

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.tajawal, FontWeight.Medium),
    Font(R.font.tajawal, FontWeight.SemiBold),
    Font(R.font.tajawal, FontWeight.Bold)
)

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

val NaskhFontFamily = FontFamily(
    Font(R.font.noto_naskh_arabic, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic_bold, FontWeight.Bold)
)

// User-supplied Quran typefaces; kept separate so each can be selected and verified on-device.
val MuhammadiQuranFontFamily = FontFamily(Font(R.font.muhammadi_quranic, FontWeight.Normal))
val MeQuranNaskhFontFamily = FontFamily(Font(R.font.me_quran_naskh, FontWeight.Normal))
val AmiriQuranColoredFontFamily = FontFamily(Font(R.font.amiri_quran_colored, FontWeight.Normal))

val KufiFontFamily = FontFamily(
    Font(R.font.noto_kufi_arabic, FontWeight.Normal),
    Font(R.font.noto_kufi_arabic, FontWeight.Medium),
    Font(R.font.noto_kufi_arabic, FontWeight.Bold)
)

fun resolveUiFontFamily(choice: AppFontChoice): FontFamily = when (choice) {
    AppFontChoice.NASKH -> NaskhFontFamily
    AppFontChoice.TAJAWAL -> TajawalFontFamily
    AppFontChoice.AMIRI -> AmiriFontFamily
    AppFontChoice.KUFI -> KufiFontFamily
}

/**
 * Font family used for long-form reading (Quran text, adhkar, duas). It is independent from the
 * UI font so a user can read the Quran in Amiri while keeping a crisp UI typeface.
 */
fun resolveReadingFontFamily(choice: ReadingFontChoice): FontFamily = when (choice) {
    ReadingFontChoice.MUHAMMADI -> MuhammadiQuranFontFamily
    ReadingFontChoice.ME_QURAN -> MeQuranNaskhFontFamily
    ReadingFontChoice.AMIRI_COLORED -> AmiriQuranColoredFontFamily
    ReadingFontChoice.AMIRI -> AmiriFontFamily
    ReadingFontChoice.NASKH -> NaskhFontFamily
    ReadingFontChoice.TAJAWAL -> TajawalFontFamily
}

fun buildAlHadithiTypography(
    fontChoice: AppFontChoice = AppFontChoice.NASKH,
    scale: Float = 1.0f
): Typography {
    val uiFamily = resolveUiFontFamily(fontChoice)
    val clamped = scale.coerceIn(0.85f, 1.45f)
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * clamped).sp,
            lineHeight = (38f * clamped).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (23f * clamped).sp,
            lineHeight = (32f * clamped).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19f * clamped).sp,
            lineHeight = (28f * clamped).sp
        ),
        titleLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (19f * clamped).sp,
            lineHeight = (27f * clamped).sp
        ),
        titleMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * clamped).sp,
            lineHeight = (24f * clamped).sp
        ),
        titleSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14f * clamped).sp,
            lineHeight = (20f * clamped).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16f * clamped).sp,
            lineHeight = (26f * clamped).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * clamped).sp,
            lineHeight = (22f * clamped).sp
        ),
        bodySmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12.5f * clamped).sp,
            lineHeight = (19f * clamped).sp
        ),
        labelLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * clamped).sp,
            lineHeight = (19f * clamped).sp
        ),
        labelMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * clamped).sp,
            lineHeight = (17f * clamped).sp
        ),
        labelSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * clamped).sp,
            lineHeight = (15f * clamped).sp
        )
    )
}

/**
 * Dedicated Quranic text style. The user-supplied Muhammadi Quranic family is the default;
 * alternatives remain available in settings for side-by-side device verification.
 */
fun quranAndHeritageStyle(
    scale: Float = 1.0f,
    fontChoice: ReadingFontChoice = ReadingFontChoice.MUHAMMADI,
    lineSpacing: LineSpacingOption = LineSpacingOption.NORMAL
): TextStyle {
    val s = scale.coerceIn(0.85f, 1.65f)
    val base = 23f * s
    val line = base * 1.95f * lineSpacing.multiplier
    return TextStyle(
        fontFamily = resolveReadingFontFamily(fontChoice),
        fontWeight = FontWeight.Normal,
        fontSize = base.sp,
        lineHeight = line.sp
    )
}

/**
 * Style for the non-Quran religious texts (adhkar, duas, ziyarat) which are less dense than the
 * Quran and read better with a slightly tighter leading.
 */
fun heritageReadingStyle(
    scale: Float = 1.0f,
    fontChoice: ReadingFontChoice = ReadingFontChoice.MUHAMMADI,
    lineSpacing: LineSpacingOption = LineSpacingOption.NORMAL
): TextStyle {
    val s = scale.coerceIn(0.85f, 1.65f)
    val base = 19f * s
    val line = base * 1.7f * lineSpacing.multiplier
    return TextStyle(
        fontFamily = resolveReadingFontFamily(fontChoice),
        fontWeight = FontWeight.Normal,
        fontSize = base.sp,
        lineHeight = line.sp
    )
}

val Typography = buildAlHadithiTypography()
