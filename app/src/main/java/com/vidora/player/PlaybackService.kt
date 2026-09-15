package com.vidora.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? =
        null

    private var localPlayer: ExoPlayer? =
        null

    private lateinit var audioSyncProcessor:
        AudioSyncProcessor

    private val syncReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action !=
                    AudioSyncManager.actionChanged()
                ) {
                    return
                }

                val uriString =
                    intent.getStringExtra(
                        "uri"
                    ) ?: return

                val offset =
                    intent.getLongExtra(
                        "offset",
                        0L
                    )

                val currentUri =
                    localPlayer
                        ?.currentMediaItem
                        ?.localConfiguration
                        ?.uri
                        ?.toString()

                if (
                    currentUri ==
                    uriString
                ) {
                    audioSyncProcessor
                        .setOffset(
                            offset
                        )
                }
            }
        }

    private val playerListener =
        object : Player.Listener {

            override fun onMediaItemTransition(
                mediaItem: androidx.media3.common.MediaItem?,
                reason: Int
            ) {

                val uri =
                    mediaItem
                        ?.localConfiguration
                        ?.uri
                        ?: return

                val offset =
                    AudioSyncManager.getOffset(
                        this@PlaybackService,
                        uri
                    )

                audioSyncProcessor
                    .setOffset(
                        offset
                    )
            }
        }

    override fun onCreate() {

        super.onCreate()

        audioSyncProcessor =
            AudioSyncProcessor()

        val audioSink =
            DefaultAudioSink.Builder(this)
                .setAudioProcessors(
                    arrayOf(
                        audioSyncProcessor
                    )
                )
                .build()

        val renderersFactory =
            DefaultRenderersFactory(this)
                .setAudioSink(
                    audioSink
                )

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    C.USAGE_MEDIA
                )
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MOVIE
                )
                .build()

        localPlayer =
            ExoPlayer.Builder(
                this,
                renderersFactory
            )
                .setAudioAttributes(
                    audioAttributes,
                    true
                )
                .setHandleAudioBecomingNoisy(
                    true
                )
                .build()

        localPlayer?.addListener(
            playerListener
        )

        val castPlayer =
            CastPlayer.Builder(this)
                .setLocalPlayer(
                    localPlayer!!
                )
                .build()

        mediaSession =
            MediaSession.Builder(
                this,
                castPlayer
            )
                .setId(
                    "VidoraPlayerSession"
                )
                .build()

        ContextCompat.registerReceiver(
            this,
            syncReceiver,
            IntentFilter(
                AudioSyncManager.actionChanged()
            ),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onGetSession(
        controllerInfo:
            MediaSession.ControllerInfo
    ): MediaSession? {

        return mediaSession
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {
        super.onTaskRemoved(
            rootIntent
        )
    }

    override fun onDestroy() {

        try {
            unregisterReceiver(
                syncReceiver
            )
        } catch (_: Exception) {
        }

        localPlayer?.removeListener(
            playerListener
        )

        mediaSession?.run {
            player.release()
            release()
        }

        localPlayer = null
        mediaSession = null

        super.onDestroy()
    }
}
