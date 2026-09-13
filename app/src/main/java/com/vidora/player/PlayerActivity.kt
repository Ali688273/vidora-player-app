package com.vidora.player

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var enhanceButton: Button

    private var player: ExoPlayer? = null
    private var enhancementEnabled = false

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

        enhanceButton =
            findViewById(R.id.enhanceButton)

        enhanceButton.setOnClickListener {
            toggleImageEnhancement()
        }

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

    private fun toggleImageEnhancement() {

        val currentPlayer =
            player ?: return

        enhancementEnabled =
            !enhancementEnabled

        if (enhancementEnabled) {

            val contrastEffect =
                Contrast(0.12f)

            val colorEffect =
                HslAdjustment.Builder()
                    .adjustSaturation(8f)
                    .adjustLightness(2f)
                    .build()

            currentPlayer.setVideoEffects(
                listOf(
                    contrastEffect,
                    colorEffect
                )
            )

            enhanceButton.text =
                getString(
                    R.string.enhance_on
                )

        } else {

            currentPlayer.setVideoEffects(
                emptyList()
            )

            enhanceButton.text =
                getString(
                    R.string.enhance_off
                )
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
