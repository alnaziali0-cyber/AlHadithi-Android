package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R

/**
 * Posts a harmless sample notification so the user can confirm that notifications are actually
 * delivered before enabling the real prayer reminders. Nothing is scheduled by this helper.
 */
object NotificationTestHelper {

    const val CHANNEL_ID = "alhadithi_test_channel"
    private const val NOTIFICATION_ID = 20250607

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** @return true when the sample notification was handed over to the system. */
    fun postTestNotification(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        if (!canPostNotifications(context)) return false
        ensureChannel(manager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alhadithi)
            .setContentTitle("تجربة إشعار — الحديثي")
            .setContentText("هكذا سيصلك تنبيه الصلاة. يمكنك تعطيله من إعدادات النظام في أي وقت.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "إشعارات التطبيق",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات التطبيق العامة واختبار الإشعارات"
            }
        )
    }
}
