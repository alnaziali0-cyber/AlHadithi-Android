package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranContinuousTextTest {

    @Test
    fun buildsAllVersesAsOneInlineTextFlow() {
        val result = buildQuranContinuousText(
            ayahs = listOf(1 to "بِسْمِ اللَّهِ", 2 to "الْحَمْدُ لِلَّهِ"),
            markerColor = Color.Blue
        )

        assertEquals("بِسْمِ اللَّهِ  ﴿1﴾  الْحَمْدُ لِلَّهِ  ﴿2﴾", result.annotatedText.text)
        assertEquals(0, result.ayahStartOffsets[1])
        assertEquals("بِسْمِ اللَّهِ  ﴿1﴾  ".length, result.ayahStartOffsets[2])
        assertTrue(!result.annotatedText.text.contains('\n'))
        assertEquals(2, result.annotatedText.getStringAnnotations(
            tag = QURAN_AYAH_ANNOTATION_TAG,
            start = 0,
            end = result.annotatedText.length
        ).size)
    }

    @Test
    fun emptySurahProducesEmptyTextAndNoOffsets() {
        val result = buildQuranContinuousText(emptyList(), Color.Blue)
        assertTrue(result.annotatedText.text.isEmpty())
        assertTrue(result.ayahStartOffsets.isEmpty())
    }
}
