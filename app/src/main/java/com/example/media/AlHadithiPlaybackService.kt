package com.example.media

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Hosts the Quran recitation in a media session so the audio keeps playing when the user leaves
 * the app and can be controlled from the system notification and the lock screen.
 *
 * The player itself stays owned by [AudioPlayerController] so the whole app keeps using a single
 * instance; this service only publishes it to the platform.
 */
class AlHadithiPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = AudioPlayerController.playerInstance ?: return
        mediaSession = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * The user swiped the task away: the recitation must continue, so the service is kept alive.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val SESSION_ID = "alhadithi_quran_session"

        /** Builds the intent that starts the playback service. */
        fun intent(context: android.content.Context): Intent =
            Intent(context, AlHadithiPlaybackService::class.java)
    }
}
