package com.vidora.player

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {

    private lateinit var playerView: PlayerView

    private var player: ExoPlayer? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(
            R.layout.activity_player
        )

        playerView =
            findViewById(R.id.playerView)

        initializePlayer()
    }

    private fun initializePlayer() {

        val uriString =
            intent.getStringExtra(
                EXTRA_VIDEO_URI
            ) ?: return

        val videoUri =
            Uri.parse(uriString)

        player =
            ExoPlayer.Builder(this)
                .build()
                .also { exoPlayer ->

                    playerView.player =
                        exoPlayer

                    val mediaItem =
                        MediaItem.fromUri(
                            videoUri
                        )

                    exoPlayer.setMediaItem(
                        mediaItem
                    )

                    exoPlayer.prepare()

                    exoPlayer.playWhenReady =
                        true
                }
    }

    override fun onStop() {
        super.onStop()

        player?.pause()
    }

    override fun onDestroy() {

        playerView.player = null

        player?.release()

        player = null

        super.onDestroy()
    }

    companion object {

        const val EXTRA_VIDEO_URI =
            "com.vidora.player.EXTRA_VIDEO_URI"

        const val EXTRA_VIDEO_NAME =
            "com.vidora.player.EXTRA_VIDEO_NAME"
    }
}
