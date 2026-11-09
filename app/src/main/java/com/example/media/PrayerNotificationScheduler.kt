package com.example.media

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.domain.DailyPrayerSchedule
import com.example.domain.PrayerAlertProfile
import com.example.domain.PrayerAlertSettings
import com.example.domain.PrayerAndHijriCalculator
import com.example.domain.PrayerCalcMethod
import com.example.domain.PrayerKey
import com.example.domain.PrayerMinuteOffsets
import com.example.domain.QuietHours
import com.example.data.local.AlHadithiDatabase
import com.example.data.remote.PrayerTimesService
import com.example.domain.IraqGovernorates
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Schedules reminders only from records returned by PrayerTimesService.
 * The exact same saved schedule is consumed by the UI and alarm path; reboot recovery
 * restores these records without invoking a second calculation algorithm.
 */
object PrayerNotificationScheduler {
    const val ACTION_PRAYER = "com.example.alhadithi.action.PRAYER_ALARM"
    const val ACTION_REFRESH = "com.example.alhadithi.action.REFRESH_PRAYER_ALARMS"
    private const val PREFS = "alhadithi_prayer_alarm_config"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_GOV = "governorate_id"
    private const val KEY_LAT = "latitude"
    private const val KEY_LON = "longitude"
    private const val KEY_METHOD = "method"
    private const val KEY_FAJR = "offset_fajr"
    private const val KEY_SUNRISE = "offset_sunrise"
    private const val KEY_DHUHR = "offset_dhuhr"
    private const val KEY_ASR = "offset_asr"
    private const val KEY_MAGHRIB = "offset_maghrib"
    private const val KEY_ISHA = "offset_isha"
    private const val KEY_AREA = "area_name"
    private const val KEY_PENDING_URIS = "pending_uris"
    private const val KEY_API_SCHEDULES = "api_month_schedule_entries"
    private const val KEY_QUIET_HOURS = "quiet_hours"
    private const val KEY_ALERT_PREFIX = "alert_"
    private const val REFRESH_URI = "alhadithi://prayer/refresh"
    private const val REFRESH_HOUR = 0
    private const val REFRESH_MINUTE = 5
    private const val DAYS_AHEAD = 35

    private val baghdadTimeZone: TimeZone
        get() = PrayerAndHijriCalculator.baghdadTimeZone()

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return manager.canScheduleExactAlarms()
    }

    fun schedulePrayerAlarms(
        context: Context,
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        offsets: PrayerMinuteOffsets,
        areaName: String,
        enabled: Boolean,
        schedules: List<DailyPrayerSchedule>,
        alertProfile: PrayerAlertProfile = PrayerAlertProfile(),
        governorateId: String = "baghdad"
    ) {
        if (!enabled) {
            disable(context)
            return
        }
        if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0
        ) {
            disable(context)
            return
        }

        saveConfig(context, governorateId, latitude, longitude, method, offsets, areaName, enabled = true, alertProfile)
        val timesByDate = buildScheduleMap(schedules)
        persistScheduleMap(context, timesByDate)
        scheduleWindow(context, areaName, timesByDate, alertProfile)
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_API_SCHEDULES)
            .apply()
        cancelPrayerAlarms(context)
        cancelRefreshAlarm(context)
    }

    /** Refreshes from the one central PrayerTimesService and then schedules those exact records. */
    suspend fun refreshFromNetwork(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            restoreAndReschedule(context)
            return
        }
        val latitude = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return
        val longitude = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: return
        val governorateId = prefs.getString(KEY_GOV, "baghdad") ?: "baghdad"
        val method = PrayerCalcMethod.fromCode(prefs.getInt(KEY_METHOD, PrayerCalcMethod.JAFARI.apiCode))
        val offsets = PrayerMinuteOffsets(
            fajr = prefs.getInt(KEY_FAJR, 0), sunrise = prefs.getInt(KEY_SUNRISE, 0),
            dhuhr = prefs.getInt(KEY_DHUHR, 0), asr = prefs.getInt(KEY_ASR, 0),
            maghrib = prefs.getInt(KEY_MAGHRIB, 0), isha = prefs.getInt(KEY_ISHA, 0)
        )
        val areaName = prefs.getString(KEY_AREA, "موقعك المحدد") ?: "موقعك المحدد"
        val dao = AlHadithiDatabase.getDatabase(context).dao()
        val schedules = PrayerTimesService(context, dao).loadWindow(
            governorateId = governorateId, latitude = latitude, longitude = longitude,
            method = method, offsets = offsets, days = DAYS_AHEAD
        )
        schedulePrayerAlarms(
            context = context, latitude = latitude, longitude = longitude, method = method,
            offsets = offsets, areaName = areaName, enabled = true, schedules = schedules,
            alertProfile = readStoredAlertProfile(context), governorateId = governorateId
        )
    }

    /** Called by BOOT_COMPLETED, time-change, and exact-alarm permission receivers. */
    fun restoreAndReschedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            cancelPrayerAlarms(context)
            cancelRefreshAlarm(context)
            return
        }

        if (prefs.getString(KEY_GOV, null).isNullOrBlank()) return
        if (prefs.getString(KEY_LAT, null)?.toDoubleOrNull() == null ||
            prefs.getString(KEY_LON, null)?.toDoubleOrNull() == null) return
        val areaName = prefs.getString(KEY_AREA, "موقعك المحدد") ?: "موقعك المحدد"
        val storedSchedules = decodeScheduleMap(prefs.getStringSet(KEY_API_SCHEDULES, emptySet()).orEmpty())
        scheduleWindow(context, areaName, storedSchedules, readStoredAlertProfile(context))
    }

    private fun saveConfig(
        context: Context,
        governorateId: String,
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        offsets: PrayerMinuteOffsets,
        areaName: String,
        enabled: Boolean,
        alertProfile: PrayerAlertProfile
    ) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_GOV, governorateId)
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_LAT, latitude.toString())
            .putString(KEY_LON, longitude.toString())
            .putInt(KEY_METHOD, method.apiCode)
            .putInt(KEY_FAJR, offsets.fajr)
            .putInt(KEY_SUNRISE, offsets.sunrise)
            .putInt(KEY_DHUHR, offsets.dhuhr)
            .putInt(KEY_ASR, offsets.asr)
            .putInt(KEY_MAGHRIB, offsets.maghrib)
            .putInt(KEY_ISHA, offsets.isha)
            .putString(KEY_AREA, areaName)
            .putString(KEY_QUIET_HOURS, alertProfile.quietHours.encode())
        PrayerKey.entries.forEach { prayer ->
            editor.putString(KEY_ALERT_PREFIX + prayer.key, alertProfile.settingsFor(prayer).encode())
        }
        editor.apply()
    }

    /** Reads the persisted per-prayer reminder configuration back after a reboot or time change. */
    private fun readStoredAlertProfile(context: Context): PrayerAlertProfile {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alerts = PrayerKey.entries.associateWith { prayer ->
            PrayerAlertSettings.decode(prefs.getString(KEY_ALERT_PREFIX + prayer.key, null))
        }
        return PrayerAlertProfile(
            alerts = alerts,
            quietHours = QuietHours.decode(prefs.getString(KEY_QUIET_HOURS, null))
        )
    }

    private fun scheduleWindow(
        context: Context,
        areaName: String,
        schedulesByDate: Map<String, PrayerTimesForDate>,
        alertProfile: PrayerAlertProfile
    ) {
        cancelPrayerAlarms(context)
        val now = System.currentTimeMillis()
        val scheduledUris = linkedSetOf<String>()
        schedulesByDate.toSortedMap().forEach { (dateKey, schedule) ->
            val date = calendarFromDateKey(dateKey) ?: return@forEach
            val prayers = listOf(
                PrayerAlarm(PrayerKey.FAJR, "الفجر", schedule.fajr),
                PrayerAlarm(PrayerKey.DHUHR, "الظهر", schedule.dhuhr),
                PrayerAlarm(PrayerKey.ASR, "العصر", schedule.asr),
                PrayerAlarm(PrayerKey.MAGHRIB, "المغرب", schedule.maghrib),
                PrayerAlarm(PrayerKey.ISHA, "العشاء", schedule.isha)
            )
            prayers.forEach prayerLoop@ { prayer ->
                val alertSettings = alertProfile.settingsFor(prayer.key)
                if (!alertSettings.enabled || prayer.minutesOfDay !in 0..1439) return@prayerLoop
                val prayerMoment = (date.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, prayer.minutesOfDay / 60)
                    set(Calendar.MINUTE, prayer.minutesOfDay % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val at = (prayerMoment.clone() as Calendar).apply {
                    add(Calendar.MINUTE, -alertSettings.leadMinutes)
                }.timeInMillis
                if (at <= now + 5_000L) return@prayerLoop
                val uri = "alhadithi://prayer/$dateKey/${prayer.key}"
                val pendingIntent = prayerPendingIntent(
                    context = context, uri = uri, prayerName = prayer.arabicName,
                    timeText = String.format(Locale.US, "%02d:%02d", prayer.minutesOfDay / 60, prayer.minutesOfDay % 60),
                    areaName = areaName, soundEnabled = alertSettings.soundEnabled,
                    vibrateEnabled = alertSettings.vibrateEnabled, quietHours = alertProfile.quietHours
                )
                scheduleAlarm(context, at, pendingIntent)
                scheduledUris += uri
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_PENDING_URIS, scheduledUris)
            .apply()
        scheduleNextRefresh(context)
    }

    private data class PrayerTimesForDate(
        val dateKey: String, val fajr: Int, val dhuhr: Int, val asr: Int, val maghrib: Int, val isha: Int
    )

    private fun buildScheduleMap(schedules: List<DailyPrayerSchedule>): Map<String, PrayerTimesForDate> =
        schedules.groupBy { it.dateKey }.mapNotNull { (dateKey, items) ->
            // Never choose one arbitrarily when two records claim the same local day.
            val schedule = items.singleOrNull() ?: return@mapNotNull null
            val validTimes = listOf(
                schedule.fajrMinutes, schedule.dhuhrMinutes, schedule.asrMinutes,
                schedule.maghribMinutes, schedule.ishaMinutes
            ).all { it in 0..1439 }
            val validOrder = schedule.fajrMinutes < schedule.dhuhrMinutes &&
                schedule.dhuhrMinutes < schedule.asrMinutes &&
                schedule.asrMinutes < schedule.maghribMinutes &&
                schedule.maghribMinutes < schedule.ishaMinutes
            if (!dateKey.matches(Regex("\\d{8}")) || !validTimes || !validOrder) return@mapNotNull null
            dateKey to PrayerTimesForDate(
                dateKey, schedule.fajrMinutes, schedule.dhuhrMinutes, schedule.asrMinutes,
                schedule.maghribMinutes, schedule.ishaMinutes
            )
        }.toMap()

    private fun decodeScheduleMap(entries: Set<String>): Map<String, PrayerTimesForDate> =
        entries.mapNotNull(::parseScheduleEntry)
            .groupBy { it.dateKey }
            .mapNotNull { (dateKey, items) -> items.singleOrNull()?.let { dateKey to it } }
            .toMap()

    private fun persistScheduleMap(context: Context, map: Map<String, PrayerTimesForDate>) {
        val encoded = map.values.map { item ->
            listOf(item.dateKey, item.fajr, item.dhuhr, item.asr, item.maghrib, item.isha).joinToString(",")
        }.toSet()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_API_SCHEDULES, encoded).apply()
    }

    private fun parseScheduleEntry(value: String): PrayerTimesForDate? {
        val parts = value.split(',')
        if (parts.size != 6 || !parts[0].matches(Regex("\\d{8}"))) return null
        val values = parts.drop(1).map { it.toIntOrNull() ?: return null }
        if (values.any { it !in 0..1439 }) return null
        if (!(values[0] < values[1] && values[1] < values[2] && values[2] < values[3] && values[3] < values[4])) return null
        return PrayerTimesForDate(parts[0], values[0], values[1], values[2], values[3], values[4])
    }

    private fun calendarFromDateKey(dateKey: String): Calendar? {
        if (!dateKey.matches(Regex("\\d{8}"))) return null
        val year = dateKey.substring(0, 4).toIntOrNull() ?: return null
        val month = dateKey.substring(4, 6).toIntOrNull() ?: return null
        val day = dateKey.substring(6, 8).toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return Calendar.getInstance(baghdadTimeZone).apply {
            clear(); set(year, month - 1, day, 12, 0, 0)
            if (get(Calendar.YEAR) != year || get(Calendar.MONTH) != month - 1 || get(Calendar.DAY_OF_MONTH) != day) return null
        }
    }

    private data class PrayerAlarm(
        val key: PrayerKey,
        val arabicName: String,
        val minutesOfDay: Int
    )

    private fun prayerPendingIntent(
        context: Context,
        uri: String,
        prayerName: String,
        timeText: String,
        areaName: String,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean,
        quietHours: QuietHours
    ): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER
            data = Uri.parse(uri)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_TIME_TEXT, timeText)
            putExtra(PrayerAlarmReceiver.EXTRA_AREA_NAME, areaName)
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
            putExtra(PrayerAlarmReceiver.EXTRA_VIBRATE_ENABLED, vibrateEnabled)
            putExtra(PrayerAlarmReceiver.EXTRA_QUIET_HOURS, quietHours.encode())
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleNextRefresh(context: Context) {
        val refreshAt = Calendar.getInstance(baghdadTimeZone).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, REFRESH_HOUR)
            set(Calendar.MINUTE, REFRESH_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val intent = Intent(context, PrayerScheduleRefreshReceiver::class.java).apply {
            action = ACTION_REFRESH
            data = Uri.parse(REFRESH_URI)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarm(context, refreshAt, pendingIntent)
    }

    private fun scheduleAlarm(context: Context, atMillis: Long, pendingIntent: PendingIntent) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExactAlarms(context) ->
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                    manager.setExact(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
                else -> manager.set(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Exact-alarm access can be revoked between the permission check and scheduling.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            }
        }
    }

    private fun cancelPrayerAlarms(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uris = prefs.getStringSet(KEY_PENDING_URIS, emptySet()).orEmpty().toSet()
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        uris.forEach { uri ->
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER
                data = Uri.parse(uri)
            }
            val existing = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) {
                manager?.cancel(existing)
                existing.cancel()
            }
        }
        prefs.edit().remove(KEY_PENDING_URIS).apply()
    }

    private fun cancelRefreshAlarm(context: Context) {
        val intent = Intent(context, PrayerScheduleRefreshReceiver::class.java).apply {
            action = ACTION_REFRESH
            data = Uri.parse(REFRESH_URI)
        }
        val existing = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(existing)
            existing.cancel()
        }
    }
}
