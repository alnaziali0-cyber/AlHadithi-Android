package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات لكتالوج القرّاء ومناطق المحافظات.
 *
 * الهدف منع regressions في الروابط التي بنينا عليها: كل قارئ يجب أن يملك
 * مصدراً صوتياً واحداً على الأقل، وكل منطقة يجب أن تقع داخل العراق.
 */
class QuranCatalogAndDistrictsTest {

    @Test
    fun `every reciter exposes at least one audio source`() {
        assertTrue(SurahCatalog.reciters.isNotEmpty())
        SurahCatalog.reciters.forEach { reciter ->
            assertTrue(
                "Reciter ${reciter.id} must expose a verified audio source",
                reciter.hasSurahAudio || reciter.hasAyahAudio ||
                    reciter.audioUnit == "external" || reciter.audioUnit == "juz"
            )
        }
    }

    @Test
    fun `surah templates produce a valid url for every surah`() {
        SurahCatalog.reciters.filter { it.hasSurahAudio }.forEach { reciter ->
            val url = reciter.buildSurahStreamUrl(1)
            assertNotNull(url)
            assertTrue("Template must not keep the placeholder: $url", !url!!.contains("{n}"))
            assertTrue(url.startsWith("https://"))
            assertTrue(url.endsWith("/1.mp3"))
            // حدود السورة
            assertEquals(null, reciter.buildSurahStreamUrl(0))
            assertEquals(null, reciter.buildSurahStreamUrl(115))
        }
    }

    @Test
    fun `ayah urls are built with a six digit key`() {
        SurahCatalog.reciters.filter { it.hasAyahAudio }.forEach { reciter ->
            val url = reciter.buildAyahUrl(2, 255)
            assertNotNull(url)
            assertTrue("Unexpected ayah url: $url", url!!.endsWith("002255.mp3"))
            assertTrue(url.startsWith("https://"))
            val first = reciter.buildAyahUrl(1, 1)
            assertTrue(first!!.endsWith("001001.mp3"))
        }
    }

    @Test
    fun `mp3quran fallback is only offered for the five verified readers`() {
        val fallbackCapable = SurahCatalog.reciters.filter { it.buildMp3QuranFallbackUrl(1) != null }
        val ids = fallbackCapable.map { it.id }.toSet()
        assertEquals(
            setOf("afasy", "abdulbasit", "abdulbasitmujawwad", "husary", "minshawi"),
            ids
        )
    }

    @Test
    fun `all eighteen governorates are present with sane coordinates`() {
        assertEquals(18, IraqGovernorates.all.size)
        IraqGovernorates.all.forEach { gov ->
            assertTrue("Latitude out of range for ${gov.arabicName}", gov.latitude in 29.0..38.0)
            assertTrue("Longitude out of range for ${gov.arabicName}", gov.longitude in 38.0..49.5)
        }
    }

    @Test
    fun `every governorate exposes its districts`() {
        assertEquals(18, IraqDistricts.byGovernorate.size)
        assertTrue(IraqDistricts.byGovernorate.values.sumOf { it.size } >= 100)
        IraqDistricts.byGovernorate.forEach { (govId, districts) ->
            assertTrue("Governorate $govId has no districts", districts.isNotEmpty())
            districts.forEach { district ->
                assertTrue("District ${district.arabicName} latitude", district.latitude in 29.0..38.0)
                assertTrue("District ${district.arabicName} longitude", district.longitude in 38.0..49.5)
            }
        }
    }

    @Test
    fun `district ids are unique and resolvable`() {
        val all = IraqDistricts.byGovernorate.values.flatten()
        assertEquals(all.size, all.map { it.id }.toSet().size)
        all.forEach { district ->
            assertNotNull("District ${district.id} must resolve", IraqDistricts.byId(district.id))
        }
        assertEquals(null, IraqDistricts.byId("not-a-district"))
    }

    @Test
    fun `the nearest district helper returns a district inside iraq`() {
        val nearest = IraqDistricts.nearestTo(33.3152, 44.3661)
        assertNotNull(nearest)
        assertTrue(nearest!!.arabicName.isNotBlank())
        // المسافة إلى أقرب منطقة يجب أن تكون قصيرة (أقل من ٣٠ كم عن وسط بغداد).
        val km = QiblaGeoMath.distanceBetweenKm(33.3152, 44.3661, nearest.latitude, nearest.longitude)
        assertTrue("Nearest district is $km km away", km < 30.0)
    }
}
