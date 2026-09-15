package com.vidora.player

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class OnlineVideoActivity : Activity() {

    private var player: ExoPlayer? = null

    private lateinit var playerView: PlayerView
    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        title =
            "پخش آنلاین"

        buildUi()

        val initialUrl =
            intent.getStringExtra(
                "url"
            )
                ?: intent.getStringExtra(
                    "video_url"
                )
                ?: ""

        if (
            initialUrl.isNotBlank()
        ) {

            urlInput.setText(
                initialUrl
            )

            playUrl(
                initialUrl
            )
        }
    }

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
                )
            }

        urlInput =
            EditText(this).apply {

                hint =
                    "لینک ویدیو را وارد کنید"

                textSize = 16f

                singleLine = true

                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12)
                )
            }

        root.addView(
            urlInput,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val playButton =
            Button(this).apply {

                text =
                    "پخش ویدیو"

                setOnClickListener {

                    playUrl(
                        urlInput.text
                            .toString()
                    )
                }
            }

        root.addView(
            playButton
        )

        statusText =
            TextView(this).apply {

                text =
                    "آماده پخش"

                textSize = 14f

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8)
                )
            }

        root.addView(
            statusText
        )

        playerView =
            PlayerView(this).apply {

                useController = true

                controllerAutoShow = true

                controllerHideOnTouch = true

                setShowBuffering(
                    PlayerView.SHOW_BUFFERING_ALWAYS
                )
            }

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val closeButton =
            Button(this).apply {

                text =
                    "بستن"

                setOnClickListener {
                    finish()
                }
            }

        root.addView(
            closeButton
        )

        setContentView(
            root
        )
    }

    private fun playUrl(
        rawUrl: String
    ) {

        val url =
            rawUrl.trim()

        if (
            !VideoUrlValidator.isValid(
                url
            )
        ) {

            statusText.text =
                "لینک واردشده معتبر نیست."

            Toast.makeText(
                this,
                "لطفاً یک لینک معتبر http یا https وارد کنید.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        releasePlayer()

        statusText.text =
            "در حال اتصال..."

        val mediaItem =
            try {

                OnlinePlaybackResolver
                    .createMediaItem(
                        url
                    )

            } catch (
                _: Exception
            ) {

                statusText.text =
                    "لینک قابل پخش نیست."

                return
            }

        val newPlayer =
            ExoPlayer.Builder(
                this
            )
                .build()

        player =
            newPlayer

        playerView.player =
            newPlayer

        newPlayer.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    when (
                        playbackState
                    ) {

                        Player.STATE_BUFFERING -> {
                            statusText.text =
                                "در حال بارگذاری..."
                        }

                        Player.STATE_READY -> {
                            statusText.text =
                                "در حال پخش"
                        }

                        Player.STATE_ENDED -> {
                            statusText.text =
                                "پخش به پایان رسید"
                        }

                        else -> Unit
                    }
                }

                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    statusText.text =
                        NetworkPlaybackError
                            .message(
                                error
                            )

                    Toast.makeText(
                        this@OnlineVideoActivity,
                        NetworkPlaybackError
                            .message(
                                error
                            ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        newPlayer.setMediaItem(
            mediaItem
        )

        newPlayer.prepare()

        newPlayer.playWhenReady =
            true
    }

    override fun onPause() {

        super.onPause()

        player?.pause()
    }

    override fun onDestroy() {

        releasePlayer()

        super.onDestroy()
    }

    private fun releasePlayer() {

        playerView.player =
            null

        player?.release()

        player =
            null
    }
}
