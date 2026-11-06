package com.example

import com.example.domain.IraqGovernorates
import com.example.domain.PrayerAndHijriCalculator
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerMinuteOffsets
import com.example.domain.SurahCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun iraqGovernorates_containsAll18AndAccurateBaghdadQibla() {
        assertEquals(18, IraqGovernorates.all.size)
        val baghdad = IraqGovernorates.byId("baghdad")
        assertEquals("بغداد", baghdad.arabicName)
        // Baghdad Qibla bearing is ~200° from True North, distance ~1395-1398 km
        assertTrue(baghdad.qiblaBearingDegrees in 198..202)
        assertTrue(baghdad.distanceToKaabaKm in 1390..1405)

        val nearestKarbala = IraqGovernorates.nearestTo(32.615, 44.025)
        assertEquals("karbala", nearestKarbala.id)
    }

    @Test
    fun jafariPrayerSchedule_ordersPrayersChronologicallyAndRespectsOffsets() {
        val baghdad = IraqGovernorates.byId("baghdad")
        val cal = Calendar.getInstance(PrayerAndHijriCalculator.baghdadTimeZone()).apply {
            set(2026, Calendar.MARCH, 20, 12, 0, 0)
        }
        val baseSchedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = baghdad.latitude,
            longitude = baghdad.longitude,
            cal = cal,
            method = PrayerCalcMethod.JAFARI
        )

        assertTrue(baseSchedule.fajrMinutes < baseSchedule.sunriseMinutes)
        assertTrue(baseSchedule.sunriseMinutes < baseSchedule.dhuhrMinutes)
        assertTrue(baseSchedule.dhuhrMinutes < baseSchedule.asrMinutes)
        assertTrue(baseSchedule.asrMinutes < baseSchedule.maghribMinutes)
        assertTrue(baseSchedule.maghribMinutes < baseSchedule.ishaMinutes)

        val tunedSchedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = baghdad.latitude,
            longitude = baghdad.longitude,
            cal = cal,
            method = PrayerCalcMethod.JAFARI,
            offsets = PrayerMinuteOffsets(fajr = 2, maghrib = 3)
        )
        assertEquals(baseSchedule.fajrMinutes + 2, tunedSchedule.fajrMinutes)
        assertEquals(baseSchedule.maghribMinutes + 3, tunedSchedule.maghribMinutes)
    }

    @Test
    fun hijriConversion_respectsManualLunarOffset() {
        val baseHijri = PrayerAndHijriCalculator.gregorianToHijri(2026, 6, 15, hijriOffsetDays = 0)
        val plusOneHijri = PrayerAndHijriCalculator.gregorianToHijri(2026, 6, 15, hijriOffsetDays = 1)
        assertTrue(baseHijri.year >= 1447)
        assertTrue(baseHijri != plusOneHijri)
    }

    @Test
    fun quranReciters_haveUniqueIdsAndSecureSurahAudioUrls() {
        val reciters = SurahCatalog.reciters
        // The catalogue grows with every documented reciter, so the exact number is not the
        // invariant that matters here: unique ids and secure stream URLs are.
        assertTrue(
            "The catalogue should expose at least the 14 documented reciters",
            reciters.size >= 14
        )
        assertEquals(reciters.size, reciters.map { it.id }.distinct().size)
        assertEquals(192, SurahCatalog.reciterById("abdulbasit").surahBitrate)
        assertEquals(192, SurahCatalog.reciterById("sudais").surahBitrate)
        assertEquals(64, SurahCatalog.reciterById("minshawimujawwad").surahBitrate)

        reciters.forEach { reciter ->
            for (surahNumber in 1..114) {
                val url = reciter.buildSurahStreamUrl(surahNumber)
                assertTrue(url.startsWith("https://cdn.islamic.network/quran/audio-surah/${reciter.surahBitrate}/"))
                assertTrue(url.endsWith("/$surahNumber.mp3"))
                assertTrue(reciter.surahBitrate in setOf(32, 40, 48, 64, 128, 192))
                val fallback = reciter.buildMp3QuranFallbackUrl(surahNumber)
                assertTrue(fallback == null || fallback.startsWith("https://"))
            }
            assertTrue(reciter.buildMp3QuranFallbackUrl(0) == null)
            assertTrue(reciter.buildMp3QuranFallbackUrl(115) == null)
        }
    }

    @Test
    fun surahCatalog_contains114SurahsAndFourShiaWajibSajdahs() {
        assertEquals(114, SurahCatalog.allSurahs.size)
        val wajibSajdahNumbers = SurahCatalog.allSurahs
            .filter { it.isWajibSajdahShia }
            .map { it.number }
        assertEquals(listOf(32, 41, 53, 96), wajibSajdahNumbers)
    }
}
