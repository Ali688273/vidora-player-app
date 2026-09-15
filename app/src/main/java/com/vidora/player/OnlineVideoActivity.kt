package com.vidora.player

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class OnlineVideoActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL =
            "com.vidora.player.EXTRA_ONLINE_URL"
    }

    private lateinit var playerView: PlayerView
    private lateinit var urlInput: EditText
    private lateinit var playButton: Button
    private lateinit var retryButton: Button
    private lateinit var errorText: TextView

    private var player: ExoPlayer? = null
    private var currentUrl: String = ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_online_video
        )

        playerView =
            findViewById(
                R.id.onlinePlayerView
            )

        urlInput =
            findViewById(
                R.id.onlineUrlInput
            )

        playButton =
            findViewById(
                R.id.onlinePlayButton
            )

        retryButton =
            findViewById(
                R.id.onlineRetryButton
            )

        errorText =
            findViewById(
                R.id.onlineErrorText
            )

        val initialUrl =
            intent.getStringExtra(
                EXTRA_URL
            ).orEmpty()

        if (initialUrl.isNotBlank()) {
            urlInput.setText(
                initialUrl
            )

            playUrl(
                initialUrl
            )
        }

        playButton.setOnClickListener {

            val url =
                urlInput.text
                    .toString()
                    .trim()

            if (url.isBlank()) {
                showError(
                    "لطفاً لینک ویدئو را وارد کنید."
                )
                return@setOnClickListener
            }

            playUrl(
                url
            )
        }

        retryButton.setOnClickListener {

            if (currentUrl.isNotBlank()) {
                playUrl(
                    currentUrl
                )
            }
        }
    }

    private fun playUrl(
        url: String
    ) {

        val uri =
            try {
                Uri.parse(url)
            } catch (_: Exception) {
                null
            }

        if (
            uri == null ||
            uri.scheme !in listOf(
                "http",
                "https"
            )
        ) {
            showError(
                "لینک واردشده معتبر نیست."
            )
            return
        }

        currentUrl = url

        hideError()

        releasePlayer()

        val builder =
            ExoPlayer.Builder(this)

        player =
            builder.build()

        playerView.player =
            player

        val mediaItem =
            buildMediaItem(
                uri
            )

        player?.setMediaItem(
            mediaItem
        )

        player?.addListener(
            object : Player.Listener {

                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    showError(
                        errorMessage(
                            error
                        )
                    )
                }

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    if (
                        playbackState ==
                        Player.STATE_READY
                    ) {
                        hideError()
                    }
                }
            }
        )

        player?.prepare()
        player?.playWhenReady = true
    }

    private fun buildMediaItem(
        uri: Uri
    ): MediaItem {

        val lower =
            uri.toString()
                .lowercase()

        return when {

            lower.contains(
                ".m3u8"
            ) -> {

                MediaItem.Builder()
                    .setUri(uri)
                    .setMimeType(
                        "application/x-mpegURL"
                    )
                    .build()
            }

            lower.contains(
                ".mpd"
            ) -> {

                MediaItem.Builder()
                    .setUri(uri)
                    .setMimeType(
                        "application/dash+xml"
                    )
                    .build()
            }

            else -> {

                MediaItem.Builder()
                    .setUri(uri)
                    .build()
            }
        }
    }

    private fun errorMessage(
        error: PlaybackException
    ): String {

        return when (
            error.errorCode
        ) {

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                "اتصال اینترنت برقرار نیست."

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "زمان اتصال به سرور تمام شد."

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "سرور پاسخ نامعتبر داد."

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                "فرمت ویدئو قابل شناسایی نیست."

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                "این ویدئو روی دستگاه قابل پخش نیست."

            else ->
                "پخش ویدئوی آنلاین با خطا مواجه شد."
        }
    }

    private fun showError(
        message: String
    ) {

        errorText.text =
            message

        errorText.visibility =
            View.VISIBLE

        retryButton.visibility =
            View.VISIBLE
    }

    private fun hideError() {

        errorText.visibility =
            View.GONE

        retryButton.visibility =
            View.GONE
    }

    private fun releasePlayer() {

        playerView.player =
            null

        player?.release()

        player = null
    }

    override fun onStop() {

        super.onStop()

        releasePlayer()
    }

    override fun onDestroy() {

        releasePlayer()

        super.onDestroy()
    }
}
