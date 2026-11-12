package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Regression tests for legacy calculation helpers, Hijri conversion, and source-gated countdowns.
 * Active prayer schedules are delivered by the AlAdhan API/cache repository.
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
    fun `the Jafari method places maghrib after the Umm al-Qura method`() {
        val jafari = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI
        )
        val ummAlQura = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.UMM_AL_QURA
        )

        // Jafari uses a four degree depression so maghrib comes later than the sunset based rule.
        assertTrue(jafari.maghribMinutes > ummAlQura.maghribMinutes)
    }

    @Test
    fun `the countdown points at the next prayer and rolls over to fajr`() {
        val legacyCalculated = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
            latitude = 33.3152,
            longitude = 44.3661,
            cal = calendar(2026, 3, 15),
            method = PrayerCalcMethod.JAFARI
        )
        // The countdown accepts records delivered by the central AlAdhan API/cache service.
        val schedule = legacyCalculated.copy(
            dateKey = "20260315",
            governorateId = "baghdad",
            sourceType = "ALADHAN_API"
        )
        val nextDaySchedule = schedule.copy(
            dateKey = "20260316",
            fajrMinutes = 302,
            sourceRecordId = "baghdad-20260316"
        )
        val cal = Calendar.getInstance(baghdad).apply {
            set(2026, 2, 15, 13, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midday = PrayerAndHijriCalculator.computeNextPrayerCountdown(
            schedule = schedule,
            nowMillis = cal.timeInMillis
        )
        assertEquals("العصر", midday.prayerArabicName)

        val lateNight = Calendar.getInstance(baghdad).apply {
            set(2026, 2, 15, 23, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rollover = PrayerAndHijriCalculator.computeNextPrayerCountdown(
            schedule,
            nextDaySchedule,
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
