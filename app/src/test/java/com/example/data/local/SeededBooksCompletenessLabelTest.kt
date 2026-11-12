package com.example.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeededBooksCompletenessLabelTest {
    @Test
    fun shortSeededExcerptsAreNotLabeledAsCompleteBooks() {
        val books = SeedDataPart2.authenticBooksAndEncyclopedia()
        assertFalse(books.any { it.title.contains("الكاملة") })
        assertTrue(books.any { it.title.startsWith("مختارات من نهج البلاغة") })
        assertTrue(books.any { it.title.startsWith("مختارات من الصحيفة السجادية") })
        assertTrue(books.any { it.title.startsWith("مقتطفات من اعتقادات الإمامية") })
    }
}
