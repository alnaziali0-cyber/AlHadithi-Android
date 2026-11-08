package com.example.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.remote.PushDeviceRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives real push notifications (Firebase Cloud Messaging).
 *
 * The service handles the three notification families that must reach the device
 * even when the app is closed:
 *   • hadith / dua of the day  (kind = "hadith" | "dua")
 *   • Islamic occasions        (kind = "occasion")
 *   • application advertisements (kind = "ad")
 *
 * Every notification is rendered with the official Alhadithi monochrome icon so
 * the notification always shows the real app identity.
 */
class AlHadithiMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            val stored = PushDeviceRepository.registerToken(applicationContext, token)
            Log.i(TAG, "FCM token refresh stored=$stored")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val kind = data["kind"] ?: "content"
        val title = data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = data["body"] ?: message.notification?.body ?: return
        val destination = data["screen"] ?: "home"
        val notificationId = (kind + ":" + (data["scope"] ?: title)).hashCode()

        val channel = when (kind) {
            "prayer" -> NotificationChannels.CHANNEL_PRAYER
            "occasion" -> NotificationChannels.CHANNEL_OCCASIONS
            "ad" -> NotificationChannels.CHANNEL_ADS
            else -> NotificationChannels.CHANNEL_CONTENT
        }

        NotificationChannels.ensureAll(applicationContext)

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, destination)
            data["adId"]?.let { putExtra(EXTRA_AD_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification_alhadithi)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                when (kind) {
                    "prayer" -> NotificationCompat.PRIORITY_HIGH
                    "ad", "occasion" -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
            serviceScope.launch {
                PushDeviceRepository.logDelivery(
                    context = applicationContext,
                    kind = kind,
                    status = "sent",
                    title = title,
                    body = body
                )
            }
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS was not granted — record the fact instead of crashing.
            Log.w(TAG, "Notification permission missing")
            serviceScope.launch {
                PushDeviceRepository.logDelivery(
                    context = applicationContext,
                    kind = kind,
                    status = "permission_denied",
                    title = title,
                    body = body,
                    providerMessage = e.message
                )
            }
        }
    }

    companion object {
        private const val TAG = "AlhadithiFCM"
        const val EXTRA_DESTINATION = "alhadithi_destination"
        const val EXTRA_AD_ID = "alhadithi_ad_id"
    }
}
