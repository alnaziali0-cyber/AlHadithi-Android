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

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrayerNotificationScheduler.ACTION_PRAYER) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val timeText = intent.getStringExtra(EXTRA_TIME_TEXT).orEmpty()
        val areaName = intent.getStringExtra(EXTRA_AREA_NAME).orEmpty()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val hasCustomAdhan = AdhanAudioStorage.hasAudio(context)
        // Starting a media foreground service from an alarm is permitted for exact alarms.
        // If the user has not granted exact-alarm access, keep the audible system notification
        // rather than posting a silent notification and attempting a restricted background start.
        val canStartAdhanService = hasCustomAdhan && PrayerNotificationScheduler.canScheduleExactAlarms(context)
        ensureChannels(manager)

        val selectedChannel = if (canStartAdhanService) CHANNEL_ID_WITH_ADHAN else CHANNEL_ID_DEFAULT
        val notification = NotificationCompat.Builder(context, selectedChannel)
            .setSmallIcon(R.drawable.ic_notification_adhan)
            .setContentTitle("حان الآن وقت صلاة $prayerName")
            .setContentText(listOf("الحديثي", areaName, timeText).filter { it.isNotBlank() }.joinToString(" · "))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        val notificationId = intent.dataString?.hashCode() ?: prayerName.hashCode()
        manager.notify(notificationId, notification)

        // The long-form audio is user-selected and stored privately; no copyrighted recording
        // is bundled in the APK. Exact-alarm delivery is an eligible foreground-service start.
        if (canStartAdhanService) {
            try {
                AdhanPlaybackService.play(context)
            } catch (_: RuntimeException) {
                // Some Android builds may reject a background media FGS start. Keep the
                // prayer notification visible instead of crashing the alarm receiver.
            }
        }
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
    }

    companion object {
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_TIME_TEXT = "prayer_time_text"
        const val EXTRA_AREA_NAME = "prayer_area_name"
        const val CHANNEL_ID_DEFAULT = "alhadithi_prayer_times_v3"
        const val CHANNEL_ID_WITH_ADHAN = "alhadithi_prayer_times_v3_custom_audio"
    }
}
