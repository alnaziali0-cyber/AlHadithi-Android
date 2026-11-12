package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The verification that guards the prayer timetable.
 *
 * The app must never claim an official match it cannot prove, so the verifier reports three
 * distinct states and the dashboard turns them into different messages.
 */
class PrayerTimetableVerifierTest {

    private fun schedule(
        sourceType: String = "ALADHAN_API",
        sourceLabel: String = "AlAdhan API · method=99 · غير مؤكد التطابق مع حقيبة المؤمن",
        fajr: Int = 5 * 60 + 4,
        sunrise: Int = 6 * 60 + 40,
        dhuhr: Int = 12 * 60 + 5,
        asr: Int = 15 * 60 + 20,
        maghrib: Int = 18 * 60 + 10,
        isha: Int = 19 * 60 + 35,
        sunset: Int = 18 * 60 + 9
    ) = DailyPrayerSchedule(
        gregorianDateLabel = "الأحد، 15 شباط 2026 م",
        hijriDate = HijriDateValue(1447, 8, 26),
        fajrMinutes = fajr,
        sunriseMinutes = sunrise,
        dhuhrMinutes = dhuhr,
        asrMinutes = asr,
        maghribMinutes = maghrib,
        ishaMinutes = isha,
        midnightMinutes = 0,
        sourceLabel = sourceLabel,
        sourceType = sourceType,
        sunsetMinutes = sunset
    )

    @Test
    fun `a coherent timetable from the reference method is reported as matched`() {
        val result = PrayerTimetableVerifier.verify(schedule())
        assertEquals(VerificationStatus.MATCHED, result.status)
        assertTrue(result.offendingPrayers.isEmpty())
    }

    @Test
    fun `an unavailable timetable is not verified`() {
        val result = PrayerTimetableVerifier.verify(schedule(sourceType = "UNAVAILABLE"))
        assertEquals(VerificationStatus.UNVERIFIED, result.status)
    }

    @Test
    fun `a timetable with an impossible order is reported as a mismatch`() {
        val result = PrayerTimetableVerifier.verify(
            schedule(asr = 11 * 60, maghrib = 10 * 60)
        )
        assertEquals(VerificationStatus.MISMATCH, result.status)
        assertTrue(result.offendingPrayers.isNotEmpty())
    }

    @Test
    fun `a timetable calculated with other parameters is reported as a mismatch`() {
        val result = PrayerTimetableVerifier.verify(
            schedule(sourceLabel = "حساب فلكي جعفري محلي")
        )
        assertEquals(VerificationStatus.MISMATCH, result.status)
    }

    @Test
    fun `maghrib may never precede the sunset`() {
        val result = PrayerTimetableVerifier.verify(
            schedule(sunset = 18 * 60 + 30, maghrib = 18 * 60 + 10)
        )
        assertEquals(VerificationStatus.MISMATCH, result.status)
        assertTrue(result.offendingPrayers.contains("المغرب قبل الغروب"))
    }

    @Test
    fun `the drift comparison ignores small differences and reports large ones`() {
        val baseline = schedule()
        val close = schedule(fajr = 5 * 60 + 5)
        assertTrue(PrayerTimetableVerifier.compareDrift(baseline, close).isEmpty())

        val far = schedule(fajr = 5 * 60 + 30, isha = 20 * 60)
        val drift = PrayerTimetableVerifier.compareDrift(baseline, far)
        assertTrue(drift.contains("الفجر"))
        assertTrue(drift.contains("العشاء"))
    }
}
