package com.example.media

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.QuietHours
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class PrayerAlarmReceiver : BroadcastReceiver() {
    /**
     * Daily content alarms may have been queued before the user switched them off. The switch and the
     * selected prayers are checked again when the alarm fires, so such alarms stay silent.
     */
    private fun isDailyContentAllowed(context: Context, intent: Intent): Boolean {
        if (!DailyContentStore.isEnabled(context)) return false
        val prayerKey = intent.data?.lastPathSegment ?: return false
        return prayerKey in DailyContentStore.enabledPrayers(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PrayerNotificationScheduler.ACTION_CONTENT) {
            if (isDailyContentAllowed(context, intent)) postDailyContentNotification(context, intent)
            return
        }
        if (intent.action != PrayerNotificationScheduler.ACTION_PRAYER) return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val timeText = intent.getStringExtra(EXTRA_TIME_TEXT).orEmpty()
        val areaName = intent.getStringExtra(EXTRA_AREA_NAME).orEmpty()
        val soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val vibrateEnabled = intent.getBooleanExtra(EXTRA_VIBRATE_ENABLED, true)
        val quietHours = QuietHours.decode(intent.getStringExtra(EXTRA_QUIET_HOURS))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val hasCustomAdhan = AdhanAudioStorage.hasAudio(context)
        // Starting a media foreground service from an alarm is permitted for exact alarms.
        // If the user has not granted exact-alarm access, keep the audible system notification
        // rather than posting a silent notification and attempting a restricted background start.
        val canStartAdhanService = hasCustomAdhan && PrayerNotificationScheduler.canScheduleExactAlarms(context)
        ensureChannels(manager)

        // The quiet-hours window silences the reminder without removing the notification.
        val quietNow = quietHours.isActiveAt(currentMinutesOfDay())
        val audible = soundEnabled && !quietNow
        val vibrating = vibrateEnabled && !quietNow

        // The recording starts first: the notification is silent only when the recording really plays.
        val adhanStarted = canStartAdhanService && audible && startAdhanRecording(context)
        val selectedChannel = when {
            adhanStarted -> CHANNEL_ID_WITH_ADHAN
            audible -> CHANNEL_ID_DEFAULT
            else -> CHANNEL_ID_SILENT
        }
        val builder = NotificationCompat.Builder(context, selectedChannel)
            .setSmallIcon(R.drawable.ic_notification_alhadithi)
            .setContentTitle("حان الآن وقت صلاة $prayerName")
            .setContentText(listOf("الحديثي", areaName, timeText).filter { it.isNotBlank() }.joinToString(" · "))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (audible) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
        if (!vibrating) {
            builder.setVibrate(longArrayOf(0L))
        }
        val notification = builder.build()

        val notificationId = intent.dataString?.hashCode() ?: prayerName.hashCode()
        if (canPostNotifications(context)) manager.notify(notificationId, notification)
    }

    /** Starts the saved recording; returns false when the system refuses the background start. */
    private fun startAdhanRecording(context: Context): Boolean = try {
        AdhanPlaybackService.play(context)
        true
    } catch (_: RuntimeException) {
        // Some Android builds reject a background media foreground-service start. The prayer
        // notification then keeps the system sound instead of going silent.
        false
    }

    /** Notification permission gates only the notification; the recording may still play without it. */
    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * ينشر إشعار حديث اليوم أو دعاء اليوم بعد كل صلاة.
     * المحتوى يأتي من خطة موثّقة (تخريج الحديث أو مرجع الآية) ويصلح في
     * NotificationChannel المستقل "حديث اليوم ودعاء اليوم".
     */
    private fun postDailyContentNotification(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val title = intent.getStringExtra(EXTRA_CONTENT_TITLE)?.trim().orEmpty()
        val body = intent.getStringExtra(EXTRA_CONTENT_BODY)?.trim().orEmpty()
        val takhrij = intent.getStringExtra(EXTRA_CONTENT_TAKHRIJ)?.trim().orEmpty()
        if (title.isBlank() || body.isBlank()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        NotificationChannels.ensureAll(context)

        val fullText = if (takhrij.isBlank()) body else "$body\n\n$takhrij"
        val contentIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("alhadithi_destination", if (title.contains("دعاء")) "duas" else "hadith")
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            (intent.dataString ?: title).hashCode(),
            contentIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_CONTENT)
            .setSmallIcon(R.drawable.ic_notification_alhadithi)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((intent.dataString ?: title).hashCode(), notification)
    }

    /** Minutes of the day in the app's reference time zone, used for the quiet-hours window. */
    private fun currentMinutesOfDay(): Int {
        val cal = GregorianCalendar(TimeZone.getTimeZone("Asia/Baghdad"))
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun ensureChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        if (manager.getNotificationChannel(CHANNEL_ID_DEFAULT) == null) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_DEFAULT,
                    "تنبيهات مواقيت الصلاة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيه عند دخول وقت الصلاة عند عدم اختيار تسجيل أذان مخصص"
                    enableVibration(true)
                    setSound(sound, attributes)
                }
            )
        }

        if (manager.getNotificationChannel(CHANNEL_ID_WITH_ADHAN) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_WITH_ADHAN,
                    "تنبيهات الصلاة مع الأذان المختار",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعار صامت لأن تسجيل الأذان المختار يعمل بشكل منفصل"
                    enableVibration(true)
                    setSound(null, null)
                }
            )
        }

        if (manager.getNotificationChannel(CHANNEL_ID_SILENT) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_SILENT,
                    "تنبيهات صامتة (فترة منع الإزعاج)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "إشعار بلا صوت وبلا اهتزاز خلال فترة منع الإزعاج"
                    enableVibration(false)
                    setSound(null, null)
                }
            )
        }
    }

    companion object {
        const val EXTRA_CONTENT_TITLE = "alhadithi_content_title"
        const val EXTRA_CONTENT_BODY = "alhadithi_content_body"
        const val EXTRA_CONTENT_TAKHRIJ = "alhadithi_content_takhrij"

        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_TIME_TEXT = "prayer_time_text"
        const val EXTRA_AREA_NAME = "prayer_area_name"
        const val EXTRA_SOUND_ENABLED = "prayer_sound_enabled"
        const val EXTRA_VIBRATE_ENABLED = "prayer_vibrate_enabled"
        const val EXTRA_QUIET_HOURS = "prayer_quiet_hours"
        const val CHANNEL_ID_DEFAULT = "alhadithi_prayer_times_v3"
        const val CHANNEL_ID_WITH_ADHAN = "alhadithi_prayer_times_v3_custom_audio"
        const val CHANNEL_ID_SILENT = "alhadithi_prayer_times_v3_silent"
    }
}
