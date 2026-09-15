package com.vidora.player

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@UnstableApi
class OnlineVideoActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    private lateinit var urlInput: EditText
    private lateinit var playerView: PlayerView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
    }

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )

                setBackgroundColor(
                    getColor(
                        R.color.vidora_background
                    )
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "پخش ویدئوی آنلاین"

                textSize =
                    24f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    12,
                    0,
                    24
                )
            }

        root.addView(title)

        urlInput =
            EditText(this).apply {

                hint =
                    "آدرس ویدئو را وارد کنید"

                textSize =
                    16f

                singleLine = true

                isSingleLine = true

                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_URI

                setPadding(
                    18,
                    14,
                    18,
                    14
                )

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setHintTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )
            }

        root.addView(
            urlInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val playButton =
            Button(this).apply {

                text =
                    "▶ پخش ویدئو"

                isAllCaps =
                    false

                textSize =
                    16f

                setOnClickListener {

                    playOnlineVideo(
                        urlInput.text
                            .toString()
                            .trim()
                    )
                }
            }

        root.addView(
            playButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
        )

        statusText =
            TextView(this).apply {

                text =
                    "آدرس ویدئو را وارد کنید."

                textSize =
                    14f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    16,
                    0,
                    16
                )
            }

        root.addView(statusText)

        playerView =
            PlayerView(this).apply {

                useController =
                    true

                controllerAutoShow =
                    true

                controllerHideOnTouch =
                    true

                controllerShowTimeoutMs =
                    3500
            }

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun playOnlineVideo(
        url: String
    ) {

        if (url.isBlank()) {

            statusText.text =
                "لطفاً آدرس ویدئو را وارد کنید."

            return
        }

        val uri =
            try {
                Uri.parse(url)
            } catch (_: Exception) {

                statusText.text =
                    "آدرس ویدئو معتبر نیست."

                return
            }

        if (
            uri.scheme != "http" &&
            uri.scheme != "https"
        ) {

            statusText.text =
                "فقط آدرس‌های HTTP و HTTPS پشتیبانی می‌شوند."

            return
        }

        releasePlayer()

        statusText.text =
            "در حال اتصال به ویدئو..."

        val mediaItem =
            createMediaItem(
                uri
            )

        player =
            ExoPlayer.Builder(
                this
            )
                .build()
                .also { exoPlayer ->

                    playerView.player =
                        exoPlayer

                    exoPlayer.setMediaItem(
                        mediaItem
                    )

                    exoPlayer.prepare()

                    exoPlayer.playWhenReady =
                        true

                    exoPlayer.addListener(
                        object :
                            androidx.media3.common.Player.Listener {

                            override fun onPlaybackStateChanged(
                                playbackState: Int
                            ) {

                                when (
                                    playbackState
                                ) {

                                    androidx.media3.common.Player.STATE_BUFFERING -> {

                                        statusText.text =
                                            "در حال بارگذاری ویدئو..."
                                    }

                                    androidx.media3.common.Player.STATE_READY -> {

                                        statusText.text =
                                            "ویدئو آماده پخش است."
                                    }

                                    androidx.media3.common.Player.STATE_ENDED -> {

                                        statusText.text =
                                            "پخش ویدئو تمام شد."
                                    }
                                }
                            }

                            override fun onPlayerError(
                                error:
                                    androidx.media3.common.PlaybackException
                            ) {

                                statusText.text =
                                    NetworkPlaybackError.message(
                                        error
                                    )
                            }
                        }
                    )
                }
    }

    private fun createMediaItem(
        uri: Uri
    ): MediaItem {

        val url =
            uri.toString()
                .lowercase()

        val builder =
            MediaItem.Builder()
                .setUri(uri)

        when {

            url.contains(".m3u8") ||
                url.contains("m3u8?") -> {

                builder.setMimeType(
                    MimeTypes.APPLICATION_M3U8
                )
            }

            url.contains(".mpd") ||
                url.contains("mpd?") -> {

                builder.setMimeType(
                    MimeTypes.APPLICATION_MPD
                )
            }

            url.endsWith(".mp4") ||
                url.contains(".mp4?") -> {

                builder.setMimeType(
                    MimeTypes.VIDEO_MP4
                )
            }

            url.endsWith(".webm") ||
                url.contains(".webm?") -> {

                builder.setMimeType(
                    MimeTypes.VIDEO_WEBM
                )
            }

            url.endsWith(".mkv") ||
                url.contains(".mkv?") -> {

                builder.setMimeType(
                    MimeTypes.VIDEO_MATROSKA
                )
            }
        }

        return builder.build()
    }

    private fun releasePlayer() {

        playerView.player =
            null

        player?.release()

        player =
            null
    }

    override fun onStop() {

        super.onStop()

        player?.pause()
    }

    override fun onDestroy() {

        releasePlayer()

        super.onDestroy()
    }
}
