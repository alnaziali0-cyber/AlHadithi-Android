package com.example.domain

import com.example.data.local.PrayerCompletionEntity
import com.example.data.local.LOCAL_PRAYER_PROFILE_KEY
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Pure helpers shared by the dashboard, persistence layer and tests. */
object PrayerDailyProgress {
    val orderedPrayers: List<PrayerKey> = listOf(
        PrayerKey.FAJR,
        PrayerKey.DHUHR,
        PrayerKey.ASR,
        PrayerKey.MAGHRIB,
        PrayerKey.ISHA
    )

    /** A stable Gregorian day key in Iraq's timezone, independent of the device's manual zone. */
    fun dayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Baghdad"))
            .apply { timeInMillis = nowMillis }
        return String.format(
            Locale.ROOT,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun completedKeys(
        records: Iterable<PrayerCompletionEntity>,
        dayKey: String,
        profileKey: String = LOCAL_PRAYER_PROFILE_KEY
    ): Set<PrayerKey> = records.asSequence()
        .filter { it.profileKey == profileKey && it.dayKey == dayKey && it.isCompleted }
        .mapNotNull { record -> PrayerKey.fromKey(record.prayerKey) }
        .toSet()

    fun completedCount(completed: Set<PrayerKey>): Int =
        orderedPrayers.count { it in completed }

    fun completionRatio(completed: Set<PrayerKey>): Float =
        completedCount(completed).toFloat() / orderedPrayers.size.toFloat()
}
