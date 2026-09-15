package com.vidora.player

import android.Manifest
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.util.Rational
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

import androidx.media3.cast.MediaRouteButtonViewProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder

import com.google.common.util.concurrent.ListenableFuture

import kotlin.math.abs

@OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity : FragmentActivity() {

    private lateinit var playerView: PlayerView

    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var repeatButton: Button
    private lateinit var audioButton: Button
    private lateinit var favoriteButton: Button
    private lateinit var speedMinusButton: Button
    private lateinit var speedButton: Button
    private lateinit var speedPlusButton: Button
    private lateinit var aspectButton: Button
    private lateinit var subtitleButton: Button
    private lateinit var sleepTimerButton: Button
    private lateinit var shareButton: Button
    private lateinit var deleteButton: Button
    private lateinit var lockButton: Button
    private lateinit var fullscreenButton: Button
    private lateinit var moreButton: Button
    private lateinit var gestureInfo: TextView
    private lateinit var lockedOverlay: TextView

    private var player: Player? = null

    private var controllerFuture:
        ListenableFuture<MediaController>? = null

    private var isLocked = false
    private var currentSpeed = 1.0f
    private var aspectIndex = 0
    private var repeatEnabled = false

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

    private var gestureMode =
        GestureMode.NONE

    private enum class GestureMode {
        NONE,
        SEEK,
        VOLUME,
        BRIGHTNESS
    }

    private lateinit var audioManager: AudioManager

    private val sleepHandler =
        Handler(Looper.getMainLooper())

    private var sleepTimerRunnable: Runnable? =
        null

    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    private val subtitlePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) {
                return@registerForActivityResult
            }

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            loadVideoWithSubtitle(uri)
        }

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

        previousButton =
            findViewById(R.id.previousButton)

        nextButton =
            findViewById(R.id.nextButton)

        repeatButton =
            findViewById(R.id.repeatButton)

        audioButton =
            findViewById(R.id.audioButton)

        favoriteButton =
            findViewById(R.id.favoriteButton)

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

        shareButton =
            findViewById(R.id.shareButton)

        deleteButton =
            findViewById(R.id.deleteButton)

        lockButton =
            findViewById(R.id.lockButton)

        fullscreenButton =
            findViewById(R.id.fullscreenButton)

        moreButton =
            findViewById(R.id.moreButton)

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

        setupCastButton()

        connectToPlaybackService()

        enterFullscreen()
    }

    private fun setupCastButton() {

        try {

            playerView.setMediaRouteButtonViewProvider(
                MediaRouteButtonViewProvider()
            )

        } catch (_: Exception) {

            showTemporaryMessage(
                "Cast در این دستگاه در دسترس نیست."
            )
        }
    }

    private fun connectToPlaybackService() {

        val sessionToken =
            SessionToken(
                this,
                ComponentName(
                    this,
                    PlaybackService::class.java
                )
            )

        controllerFuture =
            MediaController.Builder(
                this,
                sessionToken
            )
                .buildAsync()

        controllerFuture?.addListener(
            {
                try {

                    val controller =
                        controllerFuture?.get()
                            ?: return@addListener

                    player = controller

                    playerView.player =
                        controller

                    initializePlayer()

                } catch (_: Exception) {

                    showTemporaryMessage(
                        "خطا در اتصال پخش‌کننده"
                    )
                }
            },
            ContextCompat.getMainExecutor(
                this
            )
        )
    }

    private fun setupButtons() {

        previousButton.setOnClickListener {
            if (!isLocked) {
                playPreviousVideo()
            }
        }

        nextButton.setOnClickListener {
            if (!isLocked) {
                playNextVideo()
            }
        }

        repeatButton.setOnClickListener {
            if (!isLocked) {
                toggleRepeat()
            }
        }

        audioButton.setOnClickListener {
            if (!isLocked) {
                showAudioTrackDialog()
            }
        }

        favoriteButton.setOnClickListener {
            if (!isLocked) {
                toggleFavorite()
            }
        }

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

            if (isLocked) {
                return@setOnClickListener
            }

            aspectIndex++

            if (
                aspectIndex >=
                aspectModes.size
            ) {
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

        shareButton.setOnClickListener {

            if (!isLocked) {
                shareCurrentVideo()
            }
        }

        deleteButton.setOnClickListener {

            if (!isLocked) {
                deleteCurrentVideo()
            }
        }

        lockButton.setOnClickListener {
            toggleLock()
        }

        fullscreenButton.setOnClickListener {
            enterFullscreen()
        }

        moreButton.setOnClickListener {

            if (!isLocked) {
                showMoreMenu()
            }
        }

        currentSpeed =
            PlaybackSettings.getDefaultSpeed(
                this
            )

        updateSpeedText()
        updateRepeatButton()
        updateFavoriteButton()
    }

    private fun showMoreMenu() {

        val options =
            arrayOf(
                "تنظیمات سرعت و پخش",
                "همگام‌سازی صدا",
                "همگام‌سازی زیرنویس",
                "تنظیمات تصویر",
                "افزودن به صف پخش",
                "نمایش صف پخش",
                "پاک کردن صف پخش"
            )

        AlertDialog.Builder(this)
            .setTitle(
                "امکانات بیشتر"
            )
            .setItems(
                options
            ) { _, which ->

                when (which) {

                    0 ->
                        showPlaybackSettings()

                    1 ->
                        showAudioSyncDialog()

                    2 ->
                        showSubtitleSyncDialog()

                    3 ->
                        showVideoQualityDialog()

                    4 ->
                        addCurrentToQueue()

                    5 ->
                        showQueue()

                    6 ->
                        PlaybackQueueManager.clear(
                            this
                        )
                }
            }
            .show()
    }

    private fun showPlaybackSettings() {

        val current =
            PlaybackSettings.autoPlayNext(
                this
            )

        AlertDialog.Builder(this)
            .setTitle(
                "تنظیمات پخش"
            )
            .setMultiChoiceItems(
                arrayOf(
                    "پخش خودکار ویدئوی بعدی"
                ),
                booleanArrayOf(current)
            ) { _, _, checked ->

                PlaybackSettings.setAutoPlayNext(
                    this,
                    checked
                )
            }
            .setPositiveButton(
                "باشه",
                null
            )
            .show()
    }

    private fun showAudioSyncDialog() {

        val uri =
            getIncomingVideoUri()
                ?: return

        val current =
            AudioSyncManager.getOffset(
                this,
                uri
            )

        val input =
            EditText(this).apply {

                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_SIGNED

                setText(
                    current.toString()
                )

                hint =
                    "میلی‌ثانیه"
            }

        AlertDialog.Builder(this)
            .setTitle(
                "همگام‌سازی صدا"
            )
            .setMessage(
                "مقدار مثبت یعنی صدا جلوتر تنظیم شود."
            )
            .setView(input)
            .setNegativeButton(
                "لغو",
                null
            )
            .setNeutralButton(
                "صفر"
            ) { _, _ ->

                AudioSyncManager.reset(
                    this,
                    uri
                )
            }
            .setPositiveButton(
                "ذخیره"
            ) { _, _ ->

                val value =
                    input.text
                        .toString()
                        .toLongOrNull()
                        ?: 0L

                AudioSyncManager.setOffset(
                    this,
                    uri,
                    value
                )

                showTemporaryMessage(
                    "تنظیم همگام‌سازی ذخیره شد."
                )
            }
            .show()
    }

    private fun showSubtitleSyncDialog() {

        val uri =
            getIncomingVideoUri()
                ?: return

        val current =
            SubtitleSyncManager.getOffset(
                this,
                uri
            )

        val input =
            EditText(this).apply {

                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_SIGNED

                setText(
                    current.toString()
                )

                hint =
                    "میلی‌ثانیه"
            }

        AlertDialog.Builder(this)
            .setTitle(
                "همگام‌سازی زیرنویس"
            )
            .setMessage(
                "مقدار مثبت یعنی زیرنویس دیرتر نمایش داده شود."
            )
            .setView(input)
            .setNegativeButton(
                "لغو",
                null
            )
            .setNeutralButton(
                "صفر"
            ) { _, _ ->

                SubtitleSyncManager.reset(
                    this,
                    uri
                )
            }
            .setPositiveButton(
                "ذخیره"
            ) { _, _ ->

                val value =
                    input.text
                        .toString()
                        .toLongOrNull()
                        ?: 0L

                SubtitleSyncManager.setOffset(
                    this,
                    uri,
                    value
                )

                showTemporaryMessage(
                    "تنظیم زیرنویس ذخیره شد."
                )
            }
            .show()
    }

    private fun showVideoQualityDialog() {

        val currentPlayer =
            player

        if (currentPlayer == null) {

            showTemporaryMessage(
                "پخش‌کننده هنوز آماده نیست."
            )

            return
        }

        val videoGroups =
            currentPlayer.currentTracks.groups
                .filter {
                    it.type ==
                        C.TRACK_TYPE_VIDEO
                }

        if (videoGroups.isEmpty()) {

            AlertDialog.Builder(this)
                .setTitle(
                    "کیفیت تصویر"
                )
                .setMessage(
                    "برای این ویدئو کیفیت‌های جداگانه قابل انتخاب نیست."
                )
                .setPositiveButton(
                    "باشه",
                    null
                )
                .show()

            return
        }

        try {

            TrackSelectionDialogBuilder(
                this,
                "انتخاب کیفیت ویدئو",
                currentPlayer,
                C.TRACK_TYPE_VIDEO
            )
                .setAllowAdaptiveSelections(true)
                .setShowDisableOption(false)
                .build()
                .show()

        } catch (_: Exception) {

            showTemporaryMessage(
                "انتخاب کیفیت برای این ویدئو در دسترس نیست."
            )
        }
    }

    private fun addCurrentToQueue() {

        val uri =
            getIncomingVideoUri()
                ?: return

        PlaybackQueueManager.add(
            this,
            uri
        )

        val queue =
            PlaybackQueueManager.getQueue(
                this
            )

        PlaybackQueueManager.setCurrentIndex(
            this,
            queue.indexOf(uri)
                .coerceAtLeast(0)
        )

        showTemporaryMessage(
            "ویدئو به صف پخش اضافه شد."
        )
    }

    private fun showQueue() {

        val queue =
            PlaybackQueueManager.getQueue(
                this
            )

        if (queue.isEmpty()) {

            AlertDialog.Builder(this)
                .setTitle(
                    "صف پخش"
                )
                .setMessage(
                    "صف پخش خالی است."
                )
                .setPositiveButton(
                    "باشه",
                    null
                )
                .show()

            return
        }

        val names =
            queue.mapIndexed { index, uri ->

                "${index + 1}. ${
                    getVideoName(uri)
                }"

            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                "صف پخش"
            )
            .setItems(
                names,
                null
            )
            .setPositiveButton(
                "باشه",
                null
            )
            .show()
    }

    private fun getIncomingVideoUri(): Uri? {

        if (
            intent.action ==
            Intent.ACTION_VIEW
        ) {
            return intent.data
        }

        val uriString =
            intent.getStringExtra(
                EXTRA_VIDEO_URI
            )

        if (uriString.isNullOrBlank()) {
            return null
        }

        return Uri.parse(uriString)
    }

    private fun isExternalVideo(): Boolean {

        return intent.action ==
            Intent.ACTION_VIEW
    }

    private fun toggleFavorite() {

        val uri =
            getIncomingVideoUri()
                ?: return

        if (isExternalVideo()) {

            showTemporaryMessage(
                "برای ویدئوی خارجی قابل استفاده نیست"
            )

            return
        }

        val favorite =
            FavoriteManager.toggle(
                this,
                uri
            )

        updateFavoriteButton()

        showTemporaryMessage(
            if (favorite) {
                getString(
                    R.string.added_to_favorites
                )
            } else {
                getString(
                    R.string.removed_from_favorites
                )
            }
        )
    }

    private fun updateFavoriteButton() {

        val uri =
            getIncomingVideoUri()
                ?: return

        if (isExternalVideo()) {

            favoriteButton.visibility =
                View.GONE

            return
        }

        favoriteButton.visibility =
            View.VISIBLE

        val favorite =
            FavoriteManager.isFavorite(
                this,
                uri
            )

        favoriteButton.text =
            if (favorite) {
                getString(
                    R.string.favorite_on
                )
            } else {
                getString(
                    R.string.favorite_off
                )
            }
    }

    private fun showAudioTrackDialog() {

        val currentPlayer =
            player ?: return

        val audioGroups =
            currentPlayer.currentTracks.groups
                .filter {
                    it.type ==
                        C.TRACK_TYPE_AUDIO
                }

        if (audioGroups.isEmpty()) {

            AlertDialog.Builder(this)
                .setTitle(
                    R.string.audio_track
                )
                .setMessage(
                    R.string.no_audio_tracks
                )
                .setPositiveButton(
                    R.string.ok,
                    null
                )
                .show()

            return
        }

        TrackSelectionDialogBuilder(
            this,
            getString(
                R.string.audio_track
            ),
            currentPlayer,
            C.TRACK_TYPE_AUDIO
        )
            .setAllowAdaptiveSelections(false)
            .build()
            .show()
    }

    private fun toggleRepeat() {

        repeatEnabled =
            !repeatEnabled

        player?.repeatMode =
            if (repeatEnabled) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }

        updateRepeatButton()
    }

    private fun updateRepeatButton() {

        repeatButton.text =
            if (repeatEnabled) {
                getString(
                    R.string.repeat_on
                )
            } else {
                getString(
                    R.string.repeat_off
                )
            }
    }

    private fun changeSpeed(
        amount: Float
    ) {

        currentSpeed =
            (
                currentSpeed + amount
            ).coerceIn(
                0.1f,
                5.0f
            )

        currentSpeed =
            String.format(
                java.util.Locale.US,
                "%.1f",
                currentSpeed
            ).toFloat()

        PlaybackSettings.setDefaultSpeed(
            this,
            currentSpeed
        )

        player?.playbackParameters =
            PlaybackParameters(
                currentSpeed
            )

        updateSpeedText()
    }

    private fun resetSpeed() {

        currentSpeed = 1.0f

        PlaybackSettings.setDefaultSpeed(
            this,
            currentSpeed
        )

        player?.playbackParameters =
            PlaybackParameters(
                currentSpeed
            )

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
            getString(
                R.string.sleep_timer_hint
            )

        input.setSingleLine(true)

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        val padding =
            (
                24 *
                    resources.displayMetrics.density
                ).toInt()

        container.setPadding(
            padding,
            0,
            padding,
            0
        )

        container.addView(
            input
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

                startSleepTimer(
                    minutes
                )

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

        sleepTimerRunnable =
            Runnable {

                player?.pause()

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

        if (
            ::sleepTimerButton.isInitialized
        ) {

            sleepTimerButton.text =
                getString(
                    R.string.sleep_timer
                )
        }
    }

    private fun setupGestures() {

        playerView.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.x
                    downY = event.y

                    startPosition =
                        player?.currentPosition
                            ?: 0L

                    startVolume =
                        audioManager.getStreamVolume(
                            AudioManager.STREAM_MUSIC
                        )

                    startBrightness =
                        window.attributes.screenBrightness
                            .takeIf {
                                it >= 0f
                            }
                            ?: 0.5f

                    gestureMode =
                        GestureMode.NONE

                    if (isLocked) {
                        return@setOnTouchListener true
                    }

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    if (isLocked) {
                        return@setOnTouchListener true
                    }

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
                            abs(deltaX) >
                            abs(deltaY)
                        ) {

                            gestureMode =
                                GestureMode.SEEK

                        } else if (
                            abs(deltaY) > 30 &&
                            abs(deltaY) >
                            abs(deltaX)
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

                        GestureMode.NONE ->
                            Unit
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    if (isLocked) {
                        return@setOnTouchListener true
                    }

                    val deltaX =
                        event.x - downX

                    val deltaY =
                        event.y - downY

                    val movement =
                        abs(deltaX) +
                            abs(deltaY)

                    val now =
                        System.currentTimeMillis()

                    if (
                        gestureMode ==
                        GestureMode.NONE &&
                        movement < 30
                    ) {

                        val doubleTap =
                            now - lastTapTime < 350L &&
                                abs(
                                    event.x -
                                        lastTapX
                                ) < 80f &&
                                abs(
                                    event.y -
                                        lastTapY
                                ) < 80f

                        if (doubleTap) {

                            handleDoubleTap(
                                event.x
                            )

                            lastTapTime = 0L

                        } else {

                            lastTapTime = now
                            lastTapX = event.x
                            lastTapY = event.y
                        }

                        return@setOnTouchListener true
                    }

                    gestureMode =
                        GestureMode.NONE

                    hideGestureInfo()

                    true
                }

                MotionEvent.ACTION_CANCEL -> {

                    gestureMode =
                        GestureMode.NONE

                    hideGestureInfo()

                    true
                }

                else -> true
            }
        }
    }

    private fun handleDoubleTap(
        x: Float
    ) {

        val currentPlayer =
            player ?: return

        val duration =
            currentPlayer.duration

        if (duration <= 0L) {
            return
        }

        val amount =
            10_000L

        val newPosition =
            if (
                x <
                playerView.width / 2f
            ) {

                (
                    currentPlayer.currentPosition -
                        amount
                    ).coerceAtLeast(0L)

            } else {

                (
                    currentPlayer.currentPosition +
                        amount
                    ).coerceAtMost(
                        duration
                    )
            }

        currentPlayer.seekTo(
            newPosition
        )

        showTemporaryMessage(
            if (
                x <
                playerView.width / 2f
            ) {
                getString(
                    R.string.seek_backward
                )
            } else {
                getString(
                    R.string.seek_forward
                )
            }
        )
    }

    private fun handleSeek(
        deltaX: Float
    ) {

        val currentPlayer =
            player ?: return

        val duration =
            currentPlayer.duration

        if (duration <= 0L) {
            return
        }

        val seekAmount =
            (
                deltaX /
                    playerView.width
                ) * 60000L

        val newPosition =
            (
                startPosition +
                    seekAmount.toLong()
                ).coerceIn(
                    0L,
                    duration
                )

        currentPlayer.seekTo(
            newPosition
        )

        val seconds =
            seekAmount.toLong() / 1000

        val sign =
            if (seconds >= 0) "+" else ""

        showGestureInfo(
            "$sign${seconds}s"
        )
    }

    private fun handleVolume(
        deltaY: Float
    ) {

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
            (
                startVolume +
                    volumeChange
                ).coerceIn(
                    0,
                    maxVolume
                )

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

    private fun handleBrightness(
        deltaY: Float
    ) {

        val change =
            -deltaY /
                playerView.height

        val newBrightness =
            (
                startBrightness +
                    change
                ).coerceIn(
                    0.05f,
                    1.0f
                )

        val attributes =
            window.attributes

        attributes.screenBrightness =
            newBrightness

        window.attributes =
            attributes

        showGestureInfo(
            getString(
                R.string.brightness_percent,
                (newBrightness * 100).toInt()
            )
        )
    }

    private fun showGestureInfo(
        text: String
    ) {

        gestureInfo.text = text
        gestureInfo.visibility =
            View.VISIBLE
    }

    private fun showTemporaryMessage(
        text: String
    ) {

        showGestureInfo(text)

        gestureInfo.postDelayed(
            {
                hideGestureInfo()
            },
            1200L
        )
    }

    private fun hideGestureInfo() {

        gestureInfo.visibility =
            View.GONE
    }

    private fun toggleLock() {

        isLocked =
            !isLocked

        if (isLocked) {

            lockedOverlay.visibility =
                View.VISIBLE

            lockButton.text =
                getString(
                    R.string.unlock
                )

            setPlayerControlsVisibility(
                false
            )

        } else {

            lockedOverlay.visibility =
                View.GONE

            lockButton.text =
                getString(
                    R.string.lock
                )

            setPlayerControlsVisibility(
                true
            )
        }
    }

    private fun setPlayerControlsVisibility(
        visible: Boolean
    ) {

        val visibility =
            if (visible) {
                View.VISIBLE
            } else {
                View.GONE
            }

        previousButton.visibility =
            visibility

        nextButton.visibility =
            visibility

        repeatButton.visibility =
            visibility

        audioButton.visibility =
            visibility

        favoriteButton.visibility =
            visibility

        speedMinusButton.visibility =
            visibility

        speedButton.visibility =
            visibility

        speedPlusButton.visibility =
            visibility

        aspectButton.visibility =
            visibility

        subtitleButton.visibility =
            visibility

        sleepTimerButton.visibility =
            visibility

        shareButton.visibility =
            visibility

        deleteButton.visibility =
            visibility

        fullscreenButton.visibility =
            visibility

        moreButton.visibility =
            visibility
    }

    private fun initializePlayer() {

        val uri =
            getIncomingVideoUri()
                ?: return

        if (isExternalVideo()) {
            deleteButton.visibility =
                View.GONE
        }

        createPlayer(
            MediaItem.fromUri(uri)
        )
    }

    private fun loadVideoWithSubtitle(
        subtitleUri: Uri
    ) {

        val videoUri =
            getIncomingVideoUri()
                ?: return

        val subtitleMimeType =
            when (
                contentResolver.getType(
                    subtitleUri
                )
            ) {

                "text/vtt" ->
                    MimeTypes.TEXT_VTT

                "application/x-subrip" ->
                    MimeTypes.APPLICATION_SUBRIP

                else ->
                    MimeTypes.APPLICATION_SUBRIP
            }

        val subtitle =
            MediaItem.SubtitleConfiguration
                .Builder(subtitleUri)
                .setMimeType(
                    subtitleMimeType
                )
                .setLanguage("fa")
                .setSelectionFlags(
                    C.SELECTION_FLAG_DEFAULT
                )
                .build()

        val mediaItem =
            MediaItem.Builder()
                .setUri(videoUri)
                .setSubtitleConfigurations(
                    listOf(subtitle)
                )
                .build()

        createPlayer(mediaItem)
    }

    private fun createPlayer(
        mediaItem: MediaItem
    ) {

        val currentPlayer =
            player ?: return

        val videoUri =
            mediaItem.localConfiguration
                ?.uri
                ?: return

        val savedPosition =
            if (isExternalVideo()) {
                0L
            } else {
                PlaybackHistoryManager
                    .getPosition(
                        this,
                        videoUri
                    )
            }

        currentPlayer.setMediaItem(
            mediaItem
        )

        currentPlayer.repeatMode =
            if (repeatEnabled) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }

        currentPlayer.prepare()

        if (savedPosition > 0L) {

            currentPlayer.seekTo(
                savedPosition
            )
        }

        currentPlayer.playbackParameters =
            PlaybackParameters(
                currentSpeed
            )

        currentPlayer.play()

        intent.putExtra(
            EXTRA_VIDEO_URI,
            videoUri.toString()
        )

        updateFavoriteButton()
    }

    private fun getVideoUris(): List<Uri> {

        if (
            ContextCompat.checkSelfPermission(
                this,
                requiredVideoPermission()
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val result =
            mutableListOf<Uri>()

        val collection =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                MediaStore.Video.Media
                    .getContentUri(
                        MediaStore.VOLUME_EXTERNAL
                    )

            } else {

                MediaStore.Video.Media
                    .EXTERNAL_CONTENT_URI
            }

        contentResolver.query(
            collection,
            arrayOf(
                MediaStore.Video.Media._ID
            ),
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media._ID
                )

            while (cursor.moveToNext()) {

                result.add(
                    Uri.withAppendedPath(
                        collection,
                        cursor.getLong(
                            idColumn
                        ).toString()
                    )
                )
            }
        }

        return result
    }

    private fun requiredVideoPermission(): String {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun playPreviousVideo() {

        if (isExternalVideo()) {
            return
        }

        val videos =
            getVideoUris()

        if (videos.isEmpty()) {
            return
        }

        val currentUri =
            intent.getStringExtra(
                EXTRA_VIDEO_URI
            )

        val currentIndex =
            videos.indexOfFirst {
                it.toString() == currentUri
            }

        val previousIndex =
            if (currentIndex <= 0) {
                videos.lastIndex
            } else {
                currentIndex - 1
            }

        openVideo(
            videos[previousIndex]
        )
    }

    private fun playNextVideo() {

        if (isExternalVideo()) {
            return
        }

        val videos =
            getVideoUris()

        if (videos.isEmpty()) {
            return
        }

        val currentUri =
            intent.getStringExtra(
                EXTRA_VIDEO_URI
            )

        val currentIndex =
            videos.indexOfFirst {
                it.toString() == currentUri
            }

        val nextIndex =
            if (
                currentIndex < 0 ||
                currentIndex >= videos.lastIndex
            ) {
                0
            } else {
                currentIndex + 1
            }

        openVideo(
            videos[nextIndex]
        )
    }

    private fun openVideo(
        uri: Uri
    ) {

        savePosition()

        intent.action =
            Intent.ACTION_MAIN

        intent.data = null

        intent.putExtra(
            EXTRA_VIDEO_URI,
            uri.toString()
        )

        intent.putExtra(
            EXTRA_VIDEO_NAME,
            getVideoName(uri)
        )

        createPlayer(
            MediaItem.fromUri(uri)
        )
    }

    private fun getVideoName(
        uri: Uri
    ): String {

        return contentResolver.query(
            uri,
            arrayOf(
                MediaStore.Video.Media.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                cursor.getString(0)
                    ?: getString(
                        R.string.unknown_video
                    )
            } else {
                getString(
                    R.string.unknown_video
                )
            }

        } ?: getString(
            R.string.unknown_video
        )
    }

    private fun savePosition() {

        if (isExternalVideo()) {
            return
        }

        val uriString =
            intent.getStringExtra(
                EXTRA_VIDEO_URI
            )
            ?: return

        val currentPlayer =
            player
                ?: return

        val position =
            currentPlayer.currentPosition

        val duration =
            currentPlayer.duration

        if (position <= 0L) {
            return
        }

        PlaybackHistoryManager.save(
            this,
            Uri.parse(uriString),
            position,
            duration
        )
    }

    private fun shareCurrentVideo() {

        val uri =
            getIncomingVideoUri()
                ?: return

        VideoShareManager.share(
            this,
            uri
        )
    }

    private fun deleteCurrentVideo() {

        if (isExternalVideo()) {
            return
        }

        val uriString =
            intent.getStringExtra(
                EXTRA_VIDEO_URI
            )
            ?: return

        val uri =
            Uri.parse(uriString)

        AlertDialog.Builder(this)
            .setTitle(
                R.string.delete_video_title
            )
            .setMessage(
                R.string.delete_video_message
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete_video
            ) { _, _ ->
                deleteVideo(uri)
            }
            .show()
    }

    private fun deleteVideo(
        uri: Uri
    ) {

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {

                val pendingIntent =
                    MediaStore.createDeleteRequest(
                        contentResolver,
                        listOf(uri)
                    )

                startIntentSenderForResult(
                    pendingIntent.intentSender,
                    DELETE_REQUEST_CODE,
                    null,
                    0,
                    0,
                    0,
                    null
                )

            } else {

                val deleted =
                    contentResolver.delete(
                        uri,
                        null,
                        null
                    )

                if (deleted > 0) {

                    PlaybackHistoryManager.clear(
                        this,
                        uri
                    )

                    showTemporaryMessage(
                        getString(
                            R.string.video_deleted
                        )
                    )

                    finish()
                }
            }

        } catch (_: Exception) {

            showTemporaryMessage(
                "حذف ویدئو انجام نشد."
            )
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode ==
            DELETE_REQUEST_CODE &&
            resultCode ==
            RESULT_OK
        ) {

            val uriString =
                intent.getStringExtra(
                    EXTRA_VIDEO_URI
                )

            if (uriString != null) {

                PlaybackHistoryManager.clear(
                    this,
                    Uri.parse(uriString)
                )
            }

            finish()
        }
    }

    private fun enterPictureInPictureModeIfPossible() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        if (isInPictureInPictureMode) {
            return
        }

        val params =
            PictureInPictureParams.Builder()
                .setAspectRatio(
                    Rational(16, 9)
                )
                .build()

        enterPictureInPictureMode(
            params
        )
    }

    override fun onUserLeaveHint() {

        super.onUserLeaveHint()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            enterPictureInPictureModeIfPossible()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean
    ) {

        super.onPictureInPictureModeChanged(
            isInPictureInPictureMode
        )

        if (isInPictureInPictureMode) {

            setPlayerControlsVisibility(
                false
            )

            lockButton.visibility =
                View.GONE

            gestureInfo.visibility =
                View.GONE

            lockedOverlay.visibility =
                View.GONE

        } else {

            lockButton.visibility =
                View.VISIBLE

            if (!isLocked) {
                setPlayerControlsVisibility(
                    true
                )
            }
        }
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

        super.onStop()
    }

    override fun onDestroy() {

        sleepTimerRunnable?.let {
            sleepHandler.removeCallbacks(it)
        }

        sleepTimerRunnable = null

        savePosition()

        playerView.player = null

        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }

        controllerFuture = null
        player = null

        super.onDestroy()
    }

    companion object {

        const val EXTRA_VIDEO_URI =
            "com.vidora.player.EXTRA_VIDEO_URI"

        const val EXTRA_VIDEO_NAME =
            "com.vidora.player.EXTRA_VIDEO_NAME"

        private const val DELETE_REQUEST_CODE =
            5001
    }
}
