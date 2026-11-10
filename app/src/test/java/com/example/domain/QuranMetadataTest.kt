package com.example.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuranMetadataTest {

    private val metadata: QuranMetadata by lazy {
        QuranMetadata.parse(assetFile().readText(Charsets.UTF_8))
    }

    private fun assetFile(): File {
        // Unit tests normally run from the app module directory; the second path covers a run from the project root.
        return listOf(
            File("src/main/assets/quran_metadata.json"),
            File("app/src/main/assets/quran_metadata.json")
        ).firstOrNull { it.exists() } ?: throw IllegalStateException("quran_metadata.json not found")
    }

    @Test
    fun `page count and page anchors match the Madinah mushaf`() {
        assertEquals(604, metadata.pageCount)
        assertEquals(QuranPosition(1, 1), metadata.pageStart(1))
        assertEquals(QuranPosition(2, 1), metadata.pageStart(2))
        assertEquals(QuranPosition(112, 1), metadata.pageStart(604))
    }

    @Test
    fun `juz and hizb anchors are exact`() {
        assertEquals(QuranPosition(1, 1), metadata.juzStart(1))
        assertEquals(QuranPosition(2, 142), metadata.juzStart(2))
        assertEquals(QuranPosition(78, 1), metadata.juzStart(30))
        assertEquals(QuranPosition(1, 1), metadata.hizbStart(1))
        assertEquals(QuranPosition(87, 1), metadata.hizbStart(60))
    }

    @Test
    fun `every juz starts at the odd hizb`() {
        for (juz in 1..30) {
            assertEquals("juz $juz", metadata.hizbStart(2 * juz - 1), metadata.juzStart(juz))
        }
    }

    @Test
    fun `page and juz lookups round-trip from their start positions`() {
        for (page in 1..604) {
            val start = metadata.pageStart(page)
            assertEquals("page $page", page, metadata.pageOf(start.sura, start.aya))
        }
        for (juz in 1..30) {
            val start = metadata.juzStart(juz)
            assertEquals("juz $juz", juz, metadata.juzOf(start.sura, start.aya))
        }
    }

    @Test
    fun `ayahs inside a range belong to the range that starts before them`() {
        assertEquals(1, metadata.pageOf(1, 7))
        assertEquals(2, metadata.pageOf(2, 1))
        assertEquals(1, metadata.juzOf(2, 141))
        assertEquals(2, metadata.juzOf(2, 142))
        assertEquals(30, metadata.juzOf(114, 6))
    }

    @Test
    fun `out of range requests are clamped to the mushaf bounds`() {
        assertEquals(metadata.pageStart(1), metadata.pageStart(0))
        assertEquals(metadata.pageStart(604), metadata.pageStart(700))
        assertEquals(metadata.juzStart(1), metadata.juzStart(-3))
        assertEquals(metadata.hizbStart(60), metadata.hizbStart(99))
    }

    @Test
    fun `malformed metadata is rejected`() {
        try {
            QuranMetadata.parse("{}")
            fail("empty metadata must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().isNotEmpty())
        }
    }
}
