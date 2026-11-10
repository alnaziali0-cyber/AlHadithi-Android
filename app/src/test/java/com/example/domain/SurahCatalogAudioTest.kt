package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات روابط التلاوة.
 *
 * بعد إعادة بناء الكتالوج على روابط مُتحقَّق منها فعلياً:
 *  • كل قارئ يملك مصدراً صوتياً واحداً على الأقل.
 *  • روابط السور تُبنى من قالب صالح، وروابط الآيات تُبنى بمفتاح من ستة أرقام.
 *  • لا يُطلب رابط سورة من قارئ لا تتوفر له تلاوة سورة كاملة.
 */
class SurahCatalogAudioTest {

    @Test
    fun `all published reciters expose a verified secure source`() {
        assertTrue(SurahCatalog.reciters.size >= 13)
        assertEquals(SurahCatalog.reciters.size, SurahCatalog.reciters.map { it.id }.distinct().size)

        SurahCatalog.reciters.forEach { reciter ->
            assertTrue(
                "Reciter ${reciter.id} must expose an audio source",
                reciter.hasSurahAudio || reciter.hasAyahAudio ||
                    reciter.audioUnit == "external" || reciter.audioUnit == "juz"
            )
            reciter.buildSurahStreamUrl(1)?.let { url ->
                assertTrue("Secure URL expected: $url", url.startsWith("https://"))
                assertTrue(url.endsWith("/1.mp3"))
            }
            reciter.buildAyahUrl(1, 1)?.let { url ->
                assertTrue("Secure URL expected: $url", url.startsWith("https://"))
                assertTrue(url.endsWith("001001.mp3"))
            }
        }
    }

    @Test
    fun `surah numbers outside the mushaf are rejected`() {
        val reciter = SurahCatalog.reciters.first { it.hasSurahAudio }
        assertNull(reciter.buildSurahStreamUrl(0))
        assertNull(reciter.buildSurahStreamUrl(115))
        assertNull(reciter.buildAyahUrl(0, 1))
        assertNull(reciter.buildAyahUrl(1, 0))
    }

    @Test
    fun `every surah url is unique per reciter`() {
        val reciter = SurahCatalog.reciters.first { it.hasSurahAudio }
        val urls = (1..114).map { reciter.buildSurahStreamUrl(it) }.toSet()
        assertEquals(114, urls.size)
        urls.forEach { url ->
            assertNotNull(url)
            assertTrue(url!!.startsWith("https://"))
        }
    }

    @Test
    fun `the mp3quran fallback stays limited to the five verified readers`() {
        val withFallback = SurahCatalog.reciters.filter { it.buildMp3QuranFallbackUrl(1) != null }
        assertEquals(5, withFallback.size)
        withFallback.forEach { reciter ->
            val fallback = reciter.buildMp3QuranFallbackUrl(1)
            assertNotNull(fallback)
            assertTrue(fallback!!.startsWith("https://server"))
            assertTrue(fallback.endsWith("/001.mp3"))
            assertNull(reciter.buildMp3QuranFallbackUrl(0))
            assertNull(reciter.buildMp3QuranFallbackUrl(115))
        }
    }
}
