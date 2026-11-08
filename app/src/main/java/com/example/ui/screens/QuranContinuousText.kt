package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/** Tag used by the continuous Quran text to identify the verse under a tap. */
internal const val QURAN_AYAH_ANNOTATION_TAG = "alhadithi_quran_ayah"

internal data class QuranContinuousText(
    val annotatedText: AnnotatedString,
    val ayahStartOffsets: Map<Int, Int>
)

/**
 * Builds the complete surah as one annotated text flow. Ayahs are not wrapped in cards or
 * separated into rows; verse numbers remain inline and tappable for the reader's actions.
 */
internal fun buildQuranContinuousText(
    ayahs: List<Pair<Int, String>>,
    markerColor: Color
): QuranContinuousText {
    val offsets = linkedMapOf<Int, Int>()
    val annotated = buildAnnotatedString {
        ayahs.forEachIndexed { index, (ayahNumber, text) ->
            if (index > 0) append("  ")
            offsets[ayahNumber] = length
            pushStringAnnotation(
                tag = QURAN_AYAH_ANNOTATION_TAG,
                annotation = ayahNumber.toString()
            )
            append(text)
            withStyle(
                SpanStyle(
                    color = markerColor,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append("  ﴿$ayahNumber﴾")
            }
            pop()
        }
    }
    return QuranContinuousText(
        annotatedText = annotated,
        ayahStartOffsets = offsets.toMap()
    )
}
