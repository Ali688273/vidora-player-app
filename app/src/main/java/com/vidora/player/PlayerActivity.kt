package com.vidora.player

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Rational
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
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
    private lateinit var subtitleButton: Button
    private lateinit var sleepTimerButton: Button
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

    private val sleepHandler =
        Handler(Looper.getMainLooper())

    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerEndTime = 0L

    private val subtitlePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) return@registerForActivityResult

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            loadVideoWithSubtitle(uri)
        }

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

        subtitleButton =
            findViewById(R.id.subtitleButton)

        sleepTimerButton =
            findViewById(R.id.sleepTimerButton)

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

        subtitleButton.setOnClickListener {
            if (!isLocked) {
                subtitlePicker.launch(
                    arrayOf(
                        "text/*",
                        "application/x-subrip",
                        "text/vtt"
                    )
                )
            }
        }

        sleepTimerButton.setOnClickListener {
            if (!isLocked) {
                showSleepTimerDialog()
            }
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

    private fun showSleepTimerDialog() {

        val input =
            EditText(this)

        input.inputType =
            InputType.TYPE_CLASS_NUMBER

        input.hint =
            getString(R.string.sleep_timer_hint)

        input.setSingleLine(true)

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        val padding =
            (24 * resources.displayMetrics.density)
                .toInt()

        container.setPadding(
            padding,
            0,
            padding,
            0
        )

        container.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    R.string.sleep_timer_title
                )
                .setView(container)
                .setPositiveButton(
                    R.string.sleep_timer_start,
                    null
                )
                .setNegativeButton(
                    R.string.cancel,
                    null
                )
                .setNeutralButton(
                    R.string.sleep_timer_cancel,
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val minutes =
                    input.text
                        .toString()
                        .trim()
                        .toLongOrNull()

                if (
                    minutes == null ||
                    minutes <= 0L
                ) {

                    input.error =
                        getString(
                            R.string.sleep_timer_invalid
                        )

                    return@setOnClickListener
                }

                startSleepTimer(minutes)

                dialog.dismiss()
            }

            dialog.getButton(
                AlertDialog.BUTTON_NEUTRAL
            ).setOnClickListener {

                cancelSleepTimer()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun startSleepTimer(
        minutes: Long
    ) {

        cancelSleepTimer()

        val delayMillis =
            minutes
                .coerceAtMost(
                    Long.MAX_VALUE / 60000L
                )
                .times(60000L)

        sleepTimerEndTime =
            System.currentTimeMillis() +
                delayMillis

        sleepTimerRunnable =
            Runnable {

                player?.pause()

                sleepTimerEndTime = 0L

                sleepTimerButton.text =
                    getString(
                        R.string.sleep_timer
                    )

                sleepTimerRunnable = null
            }

        sleepTimerButton.text =
            getString(
                R.string.sleep_timer_active,
                minutes
            )

        sleepHandler.postDelayed(
            sleepTimerRunnable!!,
            delayMillis
        )
    }

    private fun cancelSleepTimer() {

        sleepTimerRunnable?.let {
            sleepHandler.removeCallbacks(it)
        }

        sleepTimerRunnable = null
        sleepTimerEndTime = 0L

        if (::sleepTimerButton.isInitialized) {
            sleepTimerButton.text =
                getString(
                    R.string.sleep_timer
                )
        }
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

                       
