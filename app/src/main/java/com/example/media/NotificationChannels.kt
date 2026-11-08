package com.example.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.example.R

/**
 * Every notification channel used by Alhadithi.
 *
 * All channels use the official Alhadithi monochrome silhouette
 * (R.drawable.ic_notification_alhadithi) so the notification always carries the
 * real app identity on Android. Android tints the small icon itself, therefore the
 * drawable must stay a single-colour silhouette.
 */
object NotificationChannels {

    const val CHANNEL_PRAYER = "alhadithi_prayer"
    const val CHANNEL_CONTENT = "alhadithi_daily_content"
    const val CHANNEL_OCCASIONS = "alhadithi_occasions"
    const val CHANNEL_ADS = "alhadithi_ads"
    const val CHANNEL_ADHAN = "alhadithi_adhan"

    fun ensureAll(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PRAYER,
                "تنبيه الصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه عند دخول وقت كل صلاة"
                enableVibration(true)
                setShowBadge(true)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONTENT,
                "حديث اليوم ودعاء اليوم",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "حديث أو دعاء موثّق يُرسل بعد كل صلاة"
                enableVibration(false)
                setShowBadge(true)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_OCCASIONS,
                "المناسبات الإسلامية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير بالمناسبات الإسلامية الموثقة"
                setShowBadge(true)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ADS,
                "إعلانات التطبيق",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إعلانات إدارية من لوحة المطور"
                setShowBadge(true)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ADHAN,
                "تشغيل الأذان",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تشغيل تلاوة الأذان في الخلفية"
                setShowBadge(false)
            }
        )
    }
}
