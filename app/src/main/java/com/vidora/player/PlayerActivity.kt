package com.vidora.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.abs

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var speedMinusButton: Button
    private lateinit var speedButton: Button
    private lateinit var speedPlusButton: Button
    private lateinit var aspectButton: Button
    private lateinit var lockButton: Button
    private lateinit var fullscreenButton: Button
    private lateinit var gestureInfo: TextView
    private lateinit var lockedOverlay: TextView

    private var player: ExoPlayer? = null

    private var isLocked = false
    private var currentSpeed = 1.0f
    private var aspectIndex = 0

    private val aspectModes = intArrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )

    private val aspectNames = arrayOf(
        "FIT",
        "FILL",
        "ZOOM"
    )

    private var downX = 0f
    private var downY = 0f
    private var startPosition = 0L
    private var startVolume = 0
    private var startBrightness = 0.5f

    private var gestureMode = GestureMode.NONE

    private enum class GestureMode {
        NONE,
        SEEK,
        VOLUME,
        BRIGHTNESS
    }

    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)

        speedMinusButton =
            findViewById(R.id.speedMinusButton)

        speedButton =
            findViewById(R.id.speedButton)

        speedPlusButton =
            findViewById(R.id.speedPlusButton)

        aspectButton =
            findViewById(R.id.aspectButton)

        lockButton =
            findViewById(R.id.lockButton)

        fullscreenButton =
            findViewById(R.id.fullscreenButton)

        gestureInfo =
            findViewById(R.id.gestureInfo)

        lockedOverlay =
            findViewById(R.id.lockedOverlay)

        audioManager =
            getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        setupButtons()
        setupGestures()
        initializePlayer()

        enterFullscreen()
    }

    private fun setupButtons() {

        speedMinusButton.setOnClickListener {
            if (!isLocked) {
                changeSpeed(-0.1f)
            }
        }

        speedPlusButton.setOnClickListener {
            if (!isLocked) {
                changeSpeed(0.1f)
            }
        }

        speedButton.setOnClickListener {
            if (!isLocked) {
                resetSpeed()
            }
        }

        aspectButton.setOnClickListener {
            if (isLocked) return@setOnClickListener

            aspectIndex++

            if (aspectIndex >= aspectModes.size) {
                aspectIndex = 0
            }

            playerView.resizeMode =
                aspectModes[aspectIndex]

            aspectButton.text =
                aspectNames[aspectIndex]
        }

        lockButton.setOnClickListener {
            toggleLock()
        }

        fullscreenButton.setOnClickListener {
            enterFullscreen()
        }

        updateSpeedText()
    }

    private fun changeSpeed(amount: Float) {

        currentSpeed =
            (currentSpeed + amount)
                .coerceIn(0.1f, 5.0f)

        currentSpeed =
            String.format(
                java.util.Locale.US,
                "%.1f",
                currentSpeed
            ).toFloat()

        player?.playbackParameters =
            PlaybackParameters(currentSpeed)

        updateSpeedText()
    }

    private fun resetSpeed() {

        currentSpeed = 1.0f

        player?.playbackParameters =
            PlaybackParameters(currentSpeed)

        updateSpeedText()
    }

    private fun updateSpeedText() {

        speedButton.text =
            String.format(
                java.util.Locale.US,
                "%.1f×",
                currentSpeed
            )
    }

    private fun setupGestures() {

        playerView.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {

                downX = event.x
                downY = event.y

                startPosition =
                    player?.currentPosition ?: 0L

                startVolume =
                    audioManager.getStreamVolume(
                        AudioManager.STREAM_MUSIC
                    )

                startBrightness =
                    window.attributes.screenBrightness
                        .takeIf { it >= 0f }
                        ?: 0.5f

                gestureMode =
                    GestureMode.NONE

                return@setOnTouchListener isLocked
            }

            if (isLocked) {
                return@setOnTouchListener true
            }

            when (event.action) {

                MotionEvent.ACTION_MOVE -> {

                    val deltaX =
                        event.x - downX

                    val deltaY =
                        event.y - downY

                    if (
                        gestureMode ==
                        GestureMode.NONE
                    ) {

                        if (
                            abs(deltaX) > 30 &&
                            abs(deltaX) > abs(deltaY)
                        ) {
                            gestureMode =
                                GestureMode.SEEK

                        } else if (
                            abs(deltaY) > 30 &&
                            abs(deltaY) > abs(deltaX)
                        ) {

                            gestureMode =
                                if (
                                    downX <
                                    playerView.width / 2f
                                ) {
                                    GestureMode.BRIGHTNESS
                                } else {
                                    GestureMode.VOLUME
                                }
                        }
                    }

                    when (gestureMode) {

                        GestureMode.SEEK ->
                            handleSeek(deltaX)

                        GestureMode.VOLUME ->
                            handleVolume(deltaY)

                        GestureMode.BRIGHTNESS ->
                            handleBrightness(deltaY)

                        GestureMode.NONE -> Unit
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    if (
                        gestureMode ==
                        GestureMode.NONE
                    ) {
                        false
                    } else {
                        hideGestureInfo()
                        true
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    hideGestureInfo()
                    true
                }

                else -> false
            }
        }
    }

    private fun handleSeek(deltaX: Float) {

        val currentPlayer =
            player ?: return

        val duration =
            currentPlayer.duration

        if (duration <= 0) return

        val seekAmount =
            (deltaX / playerView.width) * 60000L

        val newPosition =
            (startPosition + seekAmount.toLong())
                .coerceIn(0L, duration)

        currentPlayer.seekTo(newPosition)

        val seconds =
            seekAmount.toLong() / 1000

        val sign =
            if (seconds >= 0) "+" else ""

        showGestureInfo(
            "$sign${seconds}s"
        )
    }

    private fun handleVolume(deltaY: Float) {

        val maxVolume =
            audioManager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        val volumeChange =
            (
                -deltaY /
                    playerView.height *
                    maxVolume
            ).toInt()

        val newVolume =
            (startVolume + volumeChange)
                .coerceIn(0, maxVolume)

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            newVolume,
            0
        )

        val percent =
            if (maxVolume > 0) {
                newVolume * 100 / maxVolume
            } else {
                0
            }

        showGestureInfo(
            getString(
                R.string.volume_percent,
                percent
            )
        )
    }

    private fun handleBrightness(deltaY: Float) {

        val change =
            -deltaY / playerView.height

        val newBrightness =
            (startBrightness + change)
                .coerceIn(0.05f, 1.0f)

        val attributes =
            window.attributes

        attributes.screenBrightness =
            newBrightness

        window.attributes = attributes

        val percent =
            (newBrightness * 100).toInt()

        showGestureInfo(
            getString(
                R.string.brightness_percent,
                percent
            )
        )
    }

    private fun showGestureInfo(text: String) {

        gestureInfo.text = text
        gestureInfo.visibility =
            View.VISIBLE
    }

    private fun hideGestureInfo() {

        gestureInfo.visibility =
            View.GONE
    }

    private fun toggleLock() {

        isLocked = !isLocked

        if (isLocked) {

            lockedOverlay.visibility =
                View.VISIBLE

            lockButton.text =
                getString(R.string.unlock)

            speedMinusButton.visibility =
                View.GONE

            speedButton.visibility =
                View.GONE

            speedPlusButton.visibility =
                View.GONE

            aspectButton.visibility =
                View.GONE

            fullscreenButton.visibility =
                View.GONE

        } else {

            lockedOverlay.visibility =
                View.GONE

            lockButton.text =
                getString(R.string.lock)

            speedMinusButton.visibility =
                View.VISIBLE

            speedButton.visibility =
                View.VISIBLE

            speedPlusButton.visibility =
                View.VISIBLE

            aspectButton.visibility =
                View.VISIBLE

            fullscreenButton.visibility =
                View.VISIBLE
        }
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
                        MediaItem.fromUri(videoUri)

                    exoPlayer.setMediaItem(
                        mediaItem
                    )

                    val savedPosition =
                        getSavedPosition(
                            uriString
                        )

                    exoPlayer.prepare()

                    if (savedPosition > 0) {
                        exoPlayer.seekTo(
                            savedPosition
                        )
                    }

                    exoPlayer.playWhenReady =
                        true

                    exoPlayer.playbackParameters =
                        PlaybackParameters(
                            currentSpeed
                        )
                }
    }

    private fun getSavedPosition(
        uri: String
    ): Long {

        return getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).getLong(
            uri,
            0L
        )
    }

    private fun savePosition() {

        val uriString =
            intent.getStringExtra(
                EXTRA_VIDEO_URI
            ) ?: return

        val position =
            player?.currentPosition ?: return

        if (position <= 0) return

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .edit()
            .putLong(
                uriString,
                position
            )
            .apply()
    }

    private fun enterFullscreen() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onStop() {

        savePosition()

        player?.pause()

        super.onStop()
    }

    override fun onDestroy() {

        savePosition()

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

        private const val PREFS_NAME =
            "vidora_player_positions"
    }
}
