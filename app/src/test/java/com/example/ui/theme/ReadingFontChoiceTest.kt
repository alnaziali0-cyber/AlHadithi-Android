package com.example.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingFontChoiceTest {
    @Test
    fun `new installations default to the uploaded Quranic font`() {
        assertEquals(ReadingFontChoice.MUHAMMADI, ReadingFontChoice.fromKey(null))
        assertEquals(ReadingFontChoice.MUHAMMADI, ReadingFontChoice.fromKey("unknown"))
    }

    @Test
    fun `all uploaded Quran fonts are separately selectable`() {
        assertEquals(ReadingFontChoice.MUHAMMADI, ReadingFontChoice.fromKey("MUHAMMADI"))
        assertEquals(ReadingFontChoice.ME_QURAN, ReadingFontChoice.fromKey("ME_QURAN"))
        assertEquals(ReadingFontChoice.AMIRI_COLORED, ReadingFontChoice.fromKey("AMIRI_COLORED"))
        assertEquals(ReadingFontChoice.entries.size, ReadingFontChoice.entries.map { it.key }.distinct().size)
        assertTrue(ReadingFontChoice.MUHAMMADI.arabicLabel.contains("افتراضي"))
    }
}
