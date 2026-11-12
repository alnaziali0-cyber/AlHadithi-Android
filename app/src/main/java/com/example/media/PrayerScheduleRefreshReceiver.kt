package com.example.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Restores prayer alarms from the central AlAdhan API/cache source on boot/time changes. */
class PrayerScheduleRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val supportedActions = setOf(
            PrayerNotificationScheduler.ACTION_REFRESH,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
        if (action !in supportedActions) return

        // Refresh exact dates from the central API/cache. Never restore old alarm times blindly.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrayerNotificationScheduler.refreshFromCentralSource(context.applicationContext)
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                PrayerNotificationScheduler.pauseWithoutData(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
