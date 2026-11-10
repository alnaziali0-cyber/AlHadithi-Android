package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Verifies the local astronomical prayer-time calculation used for the offline fallback and for
 * the 35-day alarm window, plus the Hijri conversion and its manual correction.
 */
class PrayerTimesCalculationTest {

    private val baghdad = TimeZone.getTimeZone("Asia/Baghdad")

    private fun calendar(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance(baghdad).apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun `the six prayer times stay inside one day`() {
        val schedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI
        )

        listOf(
            schedule.fajrMinutes,
            schedule.sunriseMinutes,
            schedule.dhuhrMinutes,
            schedule.asrMinutes,
            schedule.maghribMinutes,
            schedule.ishaMinutes,
            schedule.midnightMinutes
        ).forEach { minutes ->
            assertTrue("minute out of range: $minutes", minutes in 0..1439)
        }
    }

    @Test
    fun `the prayer times keep their chronological order`() {
        val schedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI
        )

        assertTrue(schedule.fajrMinutes < schedule.sunriseMinutes)
        assertTrue(schedule.sunriseMinutes < schedule.dhuhrMinutes)
        assertTrue(schedule.dhuhrMinutes < schedule.asrMinutes)
        assertTrue(schedule.asrMinutes < schedule.maghribMinutes)
        assertTrue(schedule.maghribMinutes < schedule.ishaMinutes)
    }

    @Test
    fun `a minute offset shifts the corresponding prayer only`() {
        val base = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI
        )
        val shifted = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI,
            offsets = PrayerMinuteOffsets(fajr = -9)
        )

        assertEquals((base.fajrMinutes - 9 + 1440) % 1440, shifted.fajrMinutes)
        assertEquals(base.dhuhrMinutes, shifted.dhuhrMinutes)
    }

    @Test
    fun `the single method uses the documented Jafari parameters`() {
        val method = PrayerCalcMethod.JAFARI
        assertEquals(16.0, method.fajrAngle, 0.0)
        assertEquals(4.0, method.maghribAngle ?: -1.0, 0.0)
        assertEquals(14.0, method.ishaAngle, 0.0)
        assertTrue(method.jafariMidnight)
    }

    @Test
    fun `the month list has one entry per day and matches the single day calculation`() {
        val month = PrayerAndHijriCalculator.calculateMonthSchedules(
            latitude = 33.3152,
            longitude = 44.3661,
            year = 2026,
            month1Based = 3
        )
        assertEquals(31, month.size)
        val single = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI
        )
        assertEquals(single.fajrMinutes, month[14].fajrMinutes)
        assertEquals(single.ishaMinutes, month[14].ishaMinutes)
    }

    @Test
    fun `the countdown points at the next prayer and rolls over to fajr`() {
        val schedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI
        )
        val cal = Calendar.getInstance(baghdad).apply {
            set(2026, 2, 15, 13, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midday = PrayerAndHijriCalculator.computeNextPrayerCountdown(
            schedule,
            cal.timeInMillis
        )
        assertEquals("العصر", midday.prayerArabicName)

        val lateNight = Calendar.getInstance(baghdad).apply {
            set(2026, 2, 15, 23, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rollover = PrayerAndHijriCalculator.computeNextPrayerCountdown(
            schedule,
            lateNight.timeInMillis
        )
        assertEquals("الفجر", rollover.prayerArabicName)
        assertTrue(rollover.remainingSeconds > 0)
    }

    @Test
    fun `the manual hijri correction moves the date by the requested number of days`() {
        val plain = PrayerAndHijriCalculator.gregorianToHijri(2026, 3, 15, 0)
        val corrected = PrayerAndHijriCalculator.gregorianToHijri(2026, 3, 15, 1)
        assertTrue(corrected.day != plain.day || corrected.month != plain.month)
    }

    @Test
    fun `the hijri conversion is stable for the same input`() {
        val first = PrayerAndHijriCalculator.gregorianToHijri(2026, 3, 15, 0)
        val second = PrayerAndHijriCalculator.gregorianToHijri(2026, 3, 15, 0)
        assertEquals(first, second)
    }

    @Test
    fun `the time format helpers produce both a 12 and a 24 hour representation`() {
        val schedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15)
        )
        assertTrue(schedule.formatted12h(0).contains("12:00"))
        assertEquals("00:00", schedule.formatted24h(0))
        assertEquals("13:05", schedule.formatted24h(785))
    }

    @Test
    fun `a time of day outside the range is normalised`() {
        val schedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15)
        )
        assertEquals(schedule.formatted24h(100), schedule.formatted24h(1540))
    }
}
