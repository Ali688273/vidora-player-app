package com.vidora.player

import android.content.Intent

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.cast.CastPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

        val localPlayer =
            ExoPlayer.Builder(this)
                .setAudioAttributes(
                    audioAttributes,
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

        val castPlayer =
            CastPlayer.Builder(this)
                .setLocalPlayer(localPlayer)
                .build()

        mediaSession =
            MediaSession.Builder(
                this,
                castPlayer
            )
                .setId("VidoraPlayerSession")
                .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {
        /*
         * سرویس عمداً در زمان حذف برنامه
         * از Recent Apps متوقف نمی‌شود.
         *
         * بنابراین اگر پخش در حال انجام باشد،
         * امکان ادامه پخش وجود دارد.
         */
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {

        mediaSession?.run {

            player.release()

            release()
        }

        mediaSession = null

        super.onDestroy()
    }
}
