package com.vidora.player

import android.content.Intent
import androidx.media3.cast.CastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    private var localPlayer: ExoPlayer? = null

    private var audioSyncProcessor:
        AudioSyncProcessor? = null

    override fun onCreate() {
        super.onCreate()

        val processor =
            AudioSyncProcessor()

        audioSyncProcessor =
            processor

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    C.USAGE_MEDIA
                )
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MOVIE
                )
                .build()

        val renderersFactory =
            object : DefaultRenderersFactory(this) {

                override fun buildAudioSink(
                    context: android.content.Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                    enableOffload: Boolean
                ): AudioSink? {

                    return DefaultAudioSink.Builder(
                        context
                    )
                        .setEnableFloatOutput(
                            enableFloatOutput
                        )
                        .setEnableAudioTrackPlaybackParams(
                            enableAudioTrackPlaybackParams
                        )
                        .setOffloadMode(
                            if (enableOffload) {
                                DefaultAudioSink
                                    .OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED
                            } else {
                                DefaultAudioSink
                                    .OFFLOAD_MODE_DISABLED
                            }
                        )
                        .setAudioProcessors(
                            arrayOf(processor)
                        )
                        .build()
                }
            }

        val player =
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

        localPlayer =
            player

        val castPlayer =
            CastPlayer.Builder(this)
                .setLocalPlayer(player)
                .build()

        castPlayer.addListener(
            object :
                androidx.media3.common.Player.Listener {

                override fun onMediaItemTransition(
                    mediaItem:
                        androidx.media3.common.MediaItem?,
                    reason: Int
                ) {

                    val uri =
                        mediaItem
                            ?.localConfiguration
                            ?.uri
                            ?: return

                    processor.setOffset(
                        AudioSyncManager.getOffset(
                            this@PlaybackService,
                            uri
                        )
                    )
                }
            }
        )

        mediaSession =
            MediaSession.Builder(
                this,
                castPlayer
            )
                .setId(
                    "VidoraPlayerSession"
                )
                .build()
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

        mediaSession?.run {

            player.release()

            release()
        }

        mediaSession = null

        localPlayer = null

        audioSyncProcessor = null

        super.onDestroy()
    }
}
