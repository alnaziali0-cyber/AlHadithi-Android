package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R

/** Plays the user's selected adhan recording after an exact prayer alarm fires. */
class AdhanPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: Any? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> stopSelf()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> runCatching { mediaPlayer?.pause() }
            AudioManager.AUDIOFOCUS_GAIN -> runCatching {
                mediaPlayer?.let { player -> if (!player.isPlaying) player.start() }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startForeground(SERVICE_NOTIFICATION_ID, buildOngoingNotification())
        startPlayback()
        return START_NOT_STICKY
    }

    private fun startPlayback() {
        val audioFile = AdhanAudioStorage.resolvePlaybackFile(this)
        if (audioFile == null) {
            stopSelf()
            return
        }

        stopPlayback()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (!requestAudioFocus(audioAttributes)) {
                stopSelf()
                return
            }
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(audioAttributes)
            player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            player.setDataSource(audioFile.absolutePath)
            player.setOnPreparedListener { prepared -> prepared.start() }
            player.setOnCompletionListener { stopSelf() }
            player.setOnErrorListener { _, _, _ ->
                stopSelf()
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun requestAudioFocus(attributes: AudioAttributes): Boolean {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        audioManager = manager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setAcceptsDelayedFocusGain(false)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            manager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest as? AudioFocusRequest
            if (request != null) manager.abandonAudioFocusRequest(request)
            audioFocusRequest = null
        } else {
            manager.abandonAudioFocus(audioFocusListener)
        }
        audioManager = null
    }

    private fun buildOngoingNotification(): Notification {
        val stopIntent = Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alhadithi)
            .setContentTitle("أذان الحديثي")
            .setContentText("يتم تشغيل تسجيل الأذان")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف الأذان", stopPendingIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "تشغيل الأذان", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "إشعار مستمر أثناء تشغيل تسجيل الأذان"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            )
        }
    }

    private fun stopPlayback() {
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) player.stop()
            } catch (_: IllegalStateException) {
                // The player may already have completed or entered an error state.
            }
            try {
                player.reset()
            } catch (_: IllegalStateException) {
                // Reset is best-effort during teardown.
            }
            try {
                player.release()
            } catch (_: RuntimeException) {
                // Release is best-effort; the service must always be able to stop.
            }
        }
        abandonAudioFocus()
    }

    override fun onDestroy() {
        stopPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "alhadithi_adhan_playback"
        private const val SERVICE_NOTIFICATION_ID = 5102
        private const val STOP_REQUEST_CODE = 5103
        private const val ACTION_PLAY = "com.example.alhadithi.action.PLAY_ADHAN"
        private const val ACTION_STOP = "com.example.alhadithi.action.STOP_ADHAN"

        fun play(context: Context) {
            val intent = Intent(context, AdhanPlaybackService::class.java).setAction(ACTION_PLAY)
            ContextCompat.startForegroundService(context, intent)
        }

        fun hasPlayableAudio(context: Context): Boolean = AdhanAudioStorage.hasPlayableAudio(context)

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPlaybackService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
