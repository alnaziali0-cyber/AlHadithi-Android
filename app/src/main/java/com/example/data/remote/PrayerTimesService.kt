package com.example.data.remote

import android.content.Context
import com.example.domain.DailyPrayerSchedule
import com.example.domain.GovernorateInfo
import com.example.domain.PrayerAndHijriCalculator
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * One source of truth for the prayer screen, home, countdown, offline cache and prayer alarms.
 * This service intentionally contains no calculation engine or legacy timetable fallback.
 */
class PrayerTimesService(context: Context) {

    private val repository = AlAdhanPrayerTimesRepository(context.applicationContext)

    companion object {
        /** A fail-closed empty state. It never fabricates or calculates prayer times. */
        fun unavailableSchedule(
            governorate: GovernorateInfo,
            hijriOffsetDays: Int = 0,
            nowMillis: Long = System.currentTimeMillis()
        ): DailyPrayerSchedule {
            val zone = TimeZone.getTimeZone("Asia/Baghdad")
            val date = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
            val dateKey = String.format(
                Locale.US, "%04d%02d%02d",
                date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH)
            )
            return DailyPrayerSchedule(
                gregorianDateLabel = PrayerAndHijriCalculator.formatGregorianArabic(date),
                hijriDate = PrayerAndHijriCalculator.gregorianToHijri(
                    date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
                    date.get(Calendar.DAY_OF_MONTH), hijriOffsetDays
                ),
                fajrMinutes = -1,
                sunriseMinutes = -1,
                dhuhrMinutes = -1,
                asrMinutes = -1,
                maghribMinutes = -1,
                ishaMinutes = -1,
                midnightMinutes = -1,
                sourceLabel = "لا تتوفر بيانات AlAdhan محفوظة أو متاحة لمحافظة ${governorate.arabicName} لهذا التاريخ",
                dateKey = dateKey,
                governorateId = governorate.id,
                sourceType = "UNAVAILABLE"
            )
        }
    }

    /** Single gateway for prayer screen, home, countdown and scheduled alarms. */
    suspend fun loadWindow(
        governorateId: String,
        hijriOffsetDays: Int = 0,
        days: Int = 2,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyPrayerSchedule> = repository.loadWindow(
        governorateId = governorateId,
        hijriOffsetDays = hijriOffsetDays,
        days = days,
        nowMillis = nowMillis
    )
}
