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
import com.example.data.remote.PrayerNetworkClient
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Schedules local prayer notifications. The current month is taken from AlAdhan when available;
 * dates outside that API month use the app's local Jafari/selected-method calculation.
 * Coordinates and settings are stored only in app-private SharedPreferences for reboot recovery.
 */
object PrayerNotificationScheduler {
    const val ACTION_PRAYER = "com.example.alhadithi.action.PRAYER_ALARM"
    /** إشعار حديث/دعاء اليوم بعد كل صلاة. */
    const val ACTION_CONTENT = "com.example.alhadithi.action.DAILY_CONTENT"
    const val ACTION_REFRESH = "com.example.alhadithi.action.REFRESH_PRAYER_ALARMS"
    private const val PREFS = "alhadithi_prayer_alarm_config"
    private const val KEY_ENABLED = "enabled"
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
        apiYear: Int,
        apiMonth: Int,
        apiMonthSchedules: List<DailyPrayerSchedule>,
        alertProfile: PrayerAlertProfile = PrayerAlertProfile()
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

        saveConfig(context, latitude, longitude, method, offsets, areaName, enabled = true, alertProfile)
        val apiTimesByDate = buildApiTimesMap(apiYear, apiMonth, apiMonthSchedules)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_API_SCHEDULES, apiTimesByDate.map { (date, times) ->
                listOf(date, times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha).joinToString(",")
            }.toSet())
            .apply()
        scheduleWindow(
            context = context,
            latitude = latitude,
            longitude = longitude,
            method = method,
            offsets = offsets,
            areaName = areaName,
            apiTimesByDate = apiTimesByDate,
            alertProfile = alertProfile
        )
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

    /** Refreshes the current month from the prayer API, then persists and schedules a 35-day window.
     * When the network is unavailable PrayerNetworkClient returns a complete local Jafari fallback.
     */
    suspend fun refreshFromNetwork(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            restoreAndReschedule(context)
            return
        }

        val latitude = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return
        val longitude = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: return
        val method = PrayerCalcMethod.fromCode(prefs.getInt(KEY_METHOD, PrayerCalcMethod.JAFARI.apiCode))
        val offsets = PrayerMinuteOffsets(
            fajr = prefs.getInt(KEY_FAJR, 0),
            sunrise = prefs.getInt(KEY_SUNRISE, 0),
            dhuhr = prefs.getInt(KEY_DHUHR, 0),
            asr = prefs.getInt(KEY_ASR, 0),
            maghrib = prefs.getInt(KEY_MAGHRIB, 0),
            isha = prefs.getInt(KEY_ISHA, 0)
        )
        val areaName = prefs.getString(KEY_AREA, "موقعك المحدد") ?: "موقعك المحدد"
        val now = Calendar.getInstance(baghdadTimeZone)
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        val monthly = PrayerNetworkClient.fetchMonthlySchedules(
            year = year,
            month1Based = month,
            latitude = latitude,
            longitude = longitude,
            method = method,
            offsets = offsets,
            hijriOffsetDays = 0
        )
        schedulePrayerAlarms(
            context = context,
            latitude = latitude,
            longitude = longitude,
            method = method,
            offsets = offsets,
            areaName = areaName,
            enabled = true,
            apiYear = year,
            apiMonth = month,
            apiMonthSchedules = monthly,
            alertProfile = readStoredAlertProfile(context)
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

        val latitude = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return
        val longitude = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: return
        val method = PrayerCalcMethod.fromCode(prefs.getInt(KEY_METHOD, PrayerCalcMethod.JAFARI.apiCode))
        val offsets = PrayerMinuteOffsets(
            fajr = prefs.getInt(KEY_FAJR, 0),
            sunrise = prefs.getInt(KEY_SUNRISE, 0),
            dhuhr = prefs.getInt(KEY_DHUHR, 0),
            asr = prefs.getInt(KEY_ASR, 0),
            maghrib = prefs.getInt(KEY_MAGHRIB, 0),
            isha = prefs.getInt(KEY_ISHA, 0)
        )
        val areaName = prefs.getString(KEY_AREA, "موقعك المحدد") ?: "موقعك المحدد"
        val apiTimesByDate = prefs.getStringSet(KEY_API_SCHEDULES, emptySet()).orEmpty()
            .mapNotNull(::parseApiScheduleEntry)
            .toMap()
        scheduleWindow(
            context = context,
            latitude = latitude,
            longitude = longitude,
            method = method,
            offsets = offsets,
            areaName = areaName,
            apiTimesByDate = apiTimesByDate,
            alertProfile = readStoredAlertProfile(context)
        )
    }

    private fun saveConfig(
        context: Context,
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        offsets: PrayerMinuteOffsets,
        areaName: String,
        enabled: Boolean,
        alertProfile: PrayerAlertProfile
    ) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
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
        latitude: Double,
        longitude: Double,
        method: PrayerCalcMethod,
        offsets: PrayerMinuteOffsets,
        areaName: String,
        apiTimesByDate: Map<String, PrayerTimesForDate>,
        alertProfile: PrayerAlertProfile
    ) {
        cancelPrayerAlarms(context)
        val now = System.currentTimeMillis()
        val scheduledUris = linkedSetOf<String>()
        val dayCalendar = Calendar.getInstance(baghdadTimeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        repeat(DAYS_AHEAD) { dayOffset ->
            val date = (dayCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val year = date.get(Calendar.YEAR)
            val month = date.get(Calendar.MONTH) + 1
            val day = date.get(Calendar.DAY_OF_MONTH)
            val dateKey = String.format(Locale.US, "%04d%02d%02d", year, month, day)
            val apiTimes = apiTimesByDate[dateKey]
            val prayers = if (apiTimes != null) {
                listOf(
                    PrayerAlarm(PrayerKey.FAJR, "الفجر", apiTimes.fajr),
                    PrayerAlarm(PrayerKey.DHUHR, "الظهر", apiTimes.dhuhr),
                    PrayerAlarm(PrayerKey.ASR, "العصر", apiTimes.asr),
                    PrayerAlarm(PrayerKey.MAGHRIB, "المغرب", apiTimes.maghrib),
                    PrayerAlarm(PrayerKey.ISHA, "العشاء", apiTimes.isha)
                )
            } else {
                val localSchedule = PrayerAndHijriCalculator.calculateLocalPrayerSchedule(
                    latitude = latitude,
                    longitude = longitude,
                    cal = date,
                    method = method,
                    offsets = offsets,
                    sourceLabel = "حساب محلي للموقع المحدد"
                )
                listOf(
                    PrayerAlarm(PrayerKey.FAJR, "الفجر", localSchedule.fajrMinutes),
                    PrayerAlarm(PrayerKey.DHUHR, "الظهر", localSchedule.dhuhrMinutes),
                    PrayerAlarm(PrayerKey.ASR, "العصر", localSchedule.asrMinutes),
                    PrayerAlarm(PrayerKey.MAGHRIB, "المغرب", localSchedule.maghribMinutes),
                    PrayerAlarm(PrayerKey.ISHA, "العشاء", localSchedule.ishaMinutes)
                )
            }

            prayers.forEach { prayer ->
                val alertSettings = alertProfile.settingsFor(prayer.key)
                // A prayer the user switched off individually must not produce any alarm.
                if (!alertSettings.enabled) return@forEach

                val prayerMoment = (date.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, prayer.minutesOfDay / 60)
                    set(Calendar.MINUTE, prayer.minutesOfDay % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // The reminder may fire before the prayer time, possibly on the previous day.
                val at = (prayerMoment.clone() as Calendar).apply {
                    add(Calendar.MINUTE, -alertSettings.leadMinutes)
                }.timeInMillis
                if (at <= now + 5_000L) return@forEach

                val uri = "alhadithi://prayer/$dateKey/${prayer.key}"
                val pendingIntent = prayerPendingIntent(
                    context = context,
                    uri = uri,
                    prayerName = prayer.arabicName,
                    timeText = String.format(Locale.US, "%02d:%02d", prayer.minutesOfDay / 60, prayer.minutesOfDay % 60),
                    areaName = areaName,
                    soundEnabled = alertSettings.soundEnabled,
                    vibrateEnabled = alertSettings.vibrateEnabled,
                    quietHours = alertProfile.quietHours
                )
                scheduleAlarm(context, at, pendingIntent)
                scheduledUris += uri

                // إشعار حديث/دعاء اليوم بعد كل صلاة (بدل بطاقة في الصفحة الرئيسية).
                scheduleContentAlarmIfEnabled(context, dateKey, prayer.key.key, prayerMoment.timeInMillis)
            }
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_PENDING_URIS, scheduledUris)
            .apply()
        scheduleNextRefresh(context)
    }


    /**
     * يجدول إشعار المحتوى (حديث أو دعاء اليوم) بعد دخول وقت الصلاة بدقيقتين،
     * إذا كان المستخدم قد فعّل هذا النوع من الإشعارات و اختار هذه الصلاة.
     */
    private fun scheduleContentAlarmIfEnabled(
        context: Context,
        dateKey: String,
        prayerKey: String,
        prayerTimeMillis: Long
    ) {
        if (!DailyContentStore.isEnabled(context)) return
        if (prayerKey !in DailyContentStore.enabledPrayers(context)) return
        val item = DailyContentStore.planItem(context, dateKey, prayerKey) ?: return
        if (item.body.isBlank()) return

        val at = prayerTimeMillis + CONTENT_DELAY_MINUTES * 60_000L
        if (at <= System.currentTimeMillis() + 5_000L) return

        val uri = "alhadithi://content/$dateKey/$prayerKey"
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerNotificationScheduler.ACTION_CONTENT
            data = Uri.parse(uri)
            putExtra(PrayerAlarmReceiver.EXTRA_CONTENT_TITLE, item.title)
            putExtra(PrayerAlarmReceiver.EXTRA_CONTENT_BODY, item.body)
            putExtra(PrayerAlarmReceiver.EXTRA_CONTENT_TAKHRIJ, item.takhrij)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            uri.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarm(context, at, pendingIntent)
    }

    private const val CONTENT_DELAY_MINUTES = 2

    private data class PrayerTimesForDate(
        val fajr: Int,
        val dhuhr: Int,
        val asr: Int,
        val maghrib: Int,
        val isha: Int
    )

    private fun buildApiTimesMap(
        year: Int,
        month: Int,
        schedules: List<DailyPrayerSchedule>
    ): Map<String, PrayerTimesForDate> {
        if (year < 1 || month !in 1..12 || schedules.isEmpty()) return emptyMap()
        return schedules.mapIndexed { index, schedule ->
            val dateKey = String.format(Locale.US, "%04d%02d%02d", year, month, index + 1)
            dateKey to PrayerTimesForDate(
                fajr = schedule.fajrMinutes,
                dhuhr = schedule.dhuhrMinutes,
                asr = schedule.asrMinutes,
                maghrib = schedule.maghribMinutes,
                isha = schedule.ishaMinutes
            )
        }.toMap()
    }

    private fun parseApiScheduleEntry(value: String): Pair<String, PrayerTimesForDate>? {
        val parts = value.split(',')
        if (parts.size != 6 || !parts[0].matches(Regex("\\d{8}"))) return null
        val values = parts.drop(1).map { it.toIntOrNull() ?: return null }
        if (values.any { it !in 0..1439 }) return null
        return parts[0] to PrayerTimesForDate(
            fajr = values[0],
            dhuhr = values[1],
            asr = values[2],
            maghrib = values[3],
            isha = values[4]
        )
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
