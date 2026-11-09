package com.example.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Restores local alarms on boot/time changes and refreshes the month cache once per day. */
class PrayerScheduleRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PrayerNotificationScheduler.ACTION_REFRESH -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        PrayerNotificationScheduler.refreshFromNetwork(context.applicationContext)
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        // Preserve a valid offline schedule if Firestore or the device network fails.
                        PrayerNotificationScheduler.restoreAndReschedule(context.applicationContext)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" ->
                PrayerNotificationScheduler.restoreAndReschedule(context.applicationContext)
        }
    }
}
