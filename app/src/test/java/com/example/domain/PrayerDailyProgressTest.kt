package com.example.domain

import com.example.data.local.PrayerCompletionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerDailyProgressTest {
    @Test
    fun `only completed prayers for the requested day and profile are counted`() {
        val today = "2026-11-08"
        val rows = listOf(
            PrayerCompletionEntity(dayKey = today, prayerKey = "fajr", isCompleted = true, updatedAtMillis = 1L),
            PrayerCompletionEntity(dayKey = today, prayerKey = "dhuhr", isCompleted = false, updatedAtMillis = 2L),
            PrayerCompletionEntity(dayKey = "2026-11-07", prayerKey = "asr", isCompleted = true, updatedAtMillis = 3L),
            PrayerCompletionEntity(profileKey = "another-profile", dayKey = today, prayerKey = "isha", isCompleted = true, updatedAtMillis = 4L)
        )
        val completed = PrayerDailyProgress.completedKeys(rows, today)
        assertEquals(setOf(PrayerKey.FAJR), completed)
        assertEquals(1, PrayerDailyProgress.completedCount(completed))
        assertEquals(0.2f, PrayerDailyProgress.completionRatio(completed), 0.0001f)
    }

    @Test
    fun `day key is calculated in Baghdad timezone`() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 7, 22, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("2026-11-08", PrayerDailyProgress.dayKey(calendar.timeInMillis))
    }

    @Test
    fun `the tracker always uses the five obligatory daily prayers`() {
        assertEquals(
            listOf("fajr", "dhuhr", "asr", "maghrib", "isha"),
            PrayerDailyProgress.orderedPrayers.map { it.key }
        )
        assertTrue(PrayerDailyProgress.completionRatio(emptySet()) == 0f)
    }
}
