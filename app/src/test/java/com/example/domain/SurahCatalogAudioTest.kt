package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurahCatalogAudioTest {
    @Test
    fun `all published reciters generate secure urls for every surah`() {
        // The catalogue keeps growing with every documented reciter; what must always hold is
        // that the ids are unique and that every stream URL is a secure HTTPS link.
        assertTrue(SurahCatalog.reciters.size >= 13)
        assertEquals(SurahCatalog.reciters.size, SurahCatalog.reciters.map { it.id }.distinct().size)

        SurahCatalog.reciters.forEach { reciter ->
            for (surahNumber in 1..114) {
                val url = reciter.buildSurahStreamUrl(surahNumber)
                assertTrue(url.startsWith("https://cdn.islamic.network/quran/audio-surah/"))
                assertTrue(url.endsWith("/${reciter.surahServerFolder}/$surahNumber.mp3"))
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `surah zero is rejected`() {
        SurahCatalog.reciters.first().buildSurahStreamUrl(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `surah 115 is rejected`() {
        SurahCatalog.reciters.first().buildSurahStreamUrl(115)
    }
}
