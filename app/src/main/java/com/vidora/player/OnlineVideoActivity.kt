package com.vidora.player

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@UnstableApi
class OnlineVideoActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    private lateinit var urlInput: EditText
    private lateinit var playerView: PlayerView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var playButton: Button
    private lateinit var retryButton: Button
    private lateinit var backButton: Button

    private var lastUrl: String = ""

    private var isPreparing = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        buildUi()

        val incomingUrl =
            intent.getStringExtra(
                EXTRA_URL
            )

        if (!incomingUrl.isNullOrBlank()) {

            urlInput.setText(
                incomingUrl
            )

            playOnlineVideo(
                incomingUrl.trim()
            )
        }
    }

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12)
                )

                setBackgroundColor(
                    getColor(
                        R.color.vidora_background
                    )
                )
            }

        val topBar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
            }

        backButton =
            Button(this).apply {

                text = "‹"

                textSize = 30f

                isAllCaps = false

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setBackgroundColor(
                    Color.TRANSPARENT
                )

                contentDescription =
                    "بازگشت"

                setOnClickListener {
                    finish()
                }
            }

        topBar.addView(
            backButton,
            LinearLayout.LayoutParams(
                dp(52),
                dp(52)
            )
        )

        val title =
            TextView(this).apply {

                text =
                    "پخش ویدئوی آنلاین"

                textSize = 22f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(8)
                )
            }

        topBar.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val spacer =
            View(this)

        topBar.addView(
            spacer,
            LinearLayout.LayoutParams(
                dp(52),
                dp(52)
            )
        )

        root.addView(
            topBar
        )

        urlInput =
            EditText(this).apply {

                hint =
                    "https://example.com/video.mp4"

                textSize = 16f

                isSingleLine = true

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_URI

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
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val buttonRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(10)
                    }
            }

        playButton =
            Button(this).apply {

                text =
                    "▶ پخش ویدئو"

                isAllCaps = false

                textSize = 15f

                setOnClickListener {

                    playOnlineVideo(
                        urlInput.text
                            .toString()
                            .trim()
                    )
                }
            }

        buttonRow.addView(
            playButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(6)
            }
        )

        retryButton =
            Button(this).apply {

                text =
                    "↻ تلاش مجدد"

                isAllCaps = false

                textSize = 15f

                visibility =
                    View.GONE

                setOnClickListener {

                    if (
                        lastUrl.isNotBlank()
                    ) {

                        playOnlineVideo(
                            lastUrl
                        )
                    }
                }
            }

        buttonRow.addView(
            retryButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(6)
            }
        )

        root.addView(
            buttonRow
        )

        statusText =
            TextView(this).apply {

                text =
                    "آدرس ویدئو را وارد کنید."

                textSize = 14f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    dp(8),
                    dp(10),
                    dp(8),
                    dp(10)
                )
            }

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        progressBar =
            ProgressBar(this).apply {

                visibility =
                    View.GONE

                isIndeterminate =
                    true
            }

        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        playerView =
            PlayerView(this).apply {

                useController = true

                controllerAutoShow = true

                controllerHideOnTouch = true

                controllerShowTimeoutMs =
                    3500

                setBackgroundColor(
                    Color.BLACK
                )
            }

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(8)
            }
        )

        setContentView(
            root
        )
    }

    private fun playOnlineVideo(
        url: String
    ) {

        val normalizedUrl =
            url.trim()

        if (
            normalizedUrl.isBlank()
        ) {

            showError(
                "لطفاً آدرس ویدئو را وارد کنید."
            )

            return
        }

        val uri =
            try {

                Uri.parse(
                    normalizedUrl
                )

            } catch (_: Exception) {

                showError(
                    "آدرس ویدئو معتبر نیست."
                )

                return
            }

        val scheme =
            uri.scheme
                ?.lowercase()

        if (
            scheme != "http" &&
            scheme != "https"
        ) {

            showError(
                "فقط آدرس‌های HTTP و HTTPS پشتیبانی می‌شوند."
            )

            return
        }

        if (
            uri.host.isNullOrBlank()
        ) {

            showError(
                "دامنه آدرس ویدئو معتبر نیست."
            )

            return
        }

        lastUrl =
            normalizedUrl

        releasePlayer()

        setLoadingState()

        val mediaItem =
            createMediaItem(
                uri
            )

        player =
            try {

                ExoPlayer.Builder(
                    this
                )
                    .build()

            } catch (_: Exception) {

                showError(
                    "ساخت پخش‌کننده انجام نشد."
                )

                return
            }

        playerView.player =
            player

        val currentPlayer =
            player
                ?: return

        currentPlayer.addListener(
            playbackListener
        )

        try {

            currentPlayer.setMediaItem(
                mediaItem
            )

            currentPlayer.prepare()

            currentPlayer.playWhenReady =
                true

        } catch (_: Exception) {

            showError(
                "شروع پخش ویدئو انجام نشد."
            )

            releasePlayer()
        }
    }

    private val playbackListener =
        object : Player.Listener {

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {

                when (
                    playbackState
                ) {

                    Player.STATE_IDLE -> {

                        if (!isPreparing) {

                            statusText.text =
                                "پخش‌کننده آماده نیست."
                        }
                    }

                    Player.STATE_BUFFERING -> {

                        isPreparing =
                            true

                        progressBar.visibility =
                            View.VISIBLE

                        retryButton.visibility =
                            View.GONE

                        statusText.text =
                            "در حال بارگذاری ویدئو..."
                    }

                    Player.STATE_READY -> {

                        isPreparing =
                            false

                        progressBar.visibility =
                            View.GONE

                        retryButton.visibility =
                            View.GONE

                        playButton.isEnabled =
                            true

                        statusText.text =
                            "ویدئو آماده پخش است."
                    }

                    Player.STATE_ENDED -> {

                        isPreparing =
                            false

                        progressBar.visibility =
                            View.GONE

                        playButton.isEnabled =
                            true

                        statusText.text =
                            "پخش ویدئو تمام شد."
                    }
                }
            }

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {

                if (isPlaying) {

                    progressBar.visibility =
                        View.GONE

                    statusText.text =
                        "در حال پخش"
                }
            }

            override fun onPlayerError(
                error:
                    androidx.media3.common.PlaybackException
            ) {

                isPreparing =
                    false

                progressBar.visibility =
                    View.GONE

                playButton.isEnabled =
                    true

                retryButton.visibility =
                    if (
                        lastUrl.isNotBlank()
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                statusText.text =
                    NetworkPlaybackError.message(
                        error
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

            url.contains(
                ".m3u8"
            ) ||
                url.contains(
                    "m3u8?"
                ) ||
                url.contains(
                    "m3u8&"
                ) -> {

                builder.setMimeType(
                    MimeTypes.APPLICATION_M3U8
                )
            }

            url.contains(
                ".mpd"
            ) ||
                url.contains(
                    "mpd?"
                ) ||
                url.contains(
                    "mpd&"
                ) -> {

                builder.setMimeType(
                    MimeTypes.APPLICATION_MPD
                )
            }

            url.endsWith(
                ".mp4"
            ) ||
                url.contains(
                    ".mp4?"
                ) ||
                url.contains(
                    ".mp4&"
                ) -> {

                builder.setMimeType(
                    MimeTypes.VIDEO_MP4
                )
            }

            url.endsWith(
                ".webm"
            ) ||
                url.contains(
                    ".webm?"
                ) ||
                url.contains(
                    ".webm&"
                ) -> {

                builder.setMimeType(
                    MimeTypes.VIDEO_WEBM
                )
            }

            url.endsWith(
                ".mkv"
            ) ||
                url.contains(
                    ".mkv?"
                ) ||
                url.contains(
                    ".mkv&"
                ) -> {

                builder.setMimeType(
                    MimeTypes.VIDEO_MATROSKA
                )
            }
        }

        return builder.build()
    }

    private fun setLoadingState() {

        isPreparing =
            true

        playButton.isEnabled =
            false

        retryButton.visibility =
            View.GONE

        progressBar.visibility =
            View.VISIBLE

        statusText.text =
            "در حال اتصال به ویدئو..."
    }

    private fun showError(
        message: String
    ) {

        isPreparing =
            false

        progressBar.visibility =
            View.GONE

        playButton.isEnabled =
            true

        retryButton.visibility =
            if (
                lastUrl.isNotBlank()
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        statusText.text =
            message
    }

    private fun releasePlayer() {

        player?.removeListener(
            playbackListener
        )

        playerView.player =
            null

        player?.release()

        player =
            null

        isPreparing =
            false
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onStop() {

        super.onStop()

        player?.pause()
    }

    override fun onDestroy() {

        releasePlayer()

        super.onDestroy()
    }

    companion object {

        const val EXTRA_URL =
            "com.vidora.player.EXTRA_ONLINE_URL"
    }
}
