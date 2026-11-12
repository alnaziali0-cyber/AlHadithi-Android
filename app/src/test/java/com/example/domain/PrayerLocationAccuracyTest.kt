package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PrayerLocationAccuracyTest {

    @Test
    fun `fajr minute adjustment is applied exactly to calculated schedule`() {
        val date = Calendar.getInstance(PrayerAndHijriCalculator.baghdadTimeZone()).apply {
            set(2026, Calendar.NOVEMBER, 5, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val base = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 32.48,
            longitude = 44.43,
            cal = date,
            method = PrayerCalcMethod.JAFARI,
            offsets = PrayerMinuteOffsets()
        )
        val adjusted = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 32.48,
            longitude = 44.43,
            cal = date,
            method = PrayerCalcMethod.JAFARI,
            offsets = PrayerMinuteOffsets(fajr = -3)
        )

        assertEquals(base.fajrMinutes - 3, adjusted.fajrMinutes)
    }

    @Test
    fun `prayer times are calculated from supplied coordinates`() {
        val date = Calendar.getInstance(PrayerAndHijriCalculator.baghdadTimeZone()).apply {
            set(2026, Calendar.NOVEMBER, 5, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val hillah = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 32.48,
            longitude = 44.43,
            cal = date,
            method = PrayerCalcMethod.JAFARI
        )
        val baghdad = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = date,
            method = PrayerCalcMethod.JAFARI
        )

        assertNotEquals(hillah.fajrMinutes, baghdad.fajrMinutes)
        assertNotEquals(hillah.asrMinutes, baghdad.asrMinutes)
        assertNotEquals(hillah.maghribMinutes, baghdad.maghribMinutes)
    }

    @Test
    fun `jafari method uses documented code`() {
        assertEquals(0, PrayerCalcMethod.JAFARI.apiCode)
        assertTrue(PrayerCalcMethod.fromCode(0).jafariMidnight)
    }
}
