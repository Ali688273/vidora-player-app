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
import android.widget.SeekBar
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

    private lateinit var backButton: Button
    private lateinit var pauseButton: Button

    private lateinit var centerPreviousButton: Button
    private lateinit var centerPlayButton: Button
    private lateinit var centerNextButton: Button

    private lateinit var topBar: View
    private lateinit var controlScroll: View
    private lateinit var progressPanel: View
    private lateinit var progressSeekBar: SeekBar

    private lateinit var currentTimeText: TextView
    private lateinit var remainingTimeText: TextView
    private lateinit var totalTimeText: TextView

    private lateinit var gestureInfo: TextView
    private lateinit var lockedOverlay: TextView

    private var player: Player? = null

    private var controllerFuture:
        ListenableFuture<MediaController>? = null

    private lateinit var playbackAutoSaveManager:
        PlaybackAutoSaveManager

    private var isLocked = false
    private var currentSpeed = 1.0f
    private var aspectIndex = 0
    private var repeatEnabled = false

    private var controlsVisible = true
    private var isExitingPlayer = false
    private var progressUserSeeking = false

    private var pendingResumePosition = 0L
    private var resumePositionApplied = false

    private val controlsHandler =
        Handler(Looper.getMainLooper())

    private val progressHandler =
        Handler(Looper.getMainLooper())

    private val hideControlsRunnable =
        Runnable {
            if (
                !isLocked &&
                !isInPictureInPictureMode
            ) {
                hidePlayerControls()
            }
        }

    private val progressRunnable =
        object : Runnable {
            override fun run() {
                updateProgress()

                progressHandler.postDelayed(
                    this,
                    PROGRESS_UPDATE_INTERVAL
                )
            }
        }

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

    private var wasPlayingBeforePause = false
    private var enteringPictureInPicture = false

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

            val videoUri =
                getIncomingVideoUri()

            if (videoUri != null) {
                SubtitleFileManager.setSubtitleUri(
                    this,
                    videoUri,
                    uri
                )
            }

            loadVideoWithSubtitle(uri)
        }

    private val playbackListener =
        object : Player.Listener {

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {

                if (
                    playbackState ==
                    Player.STATE_READY
                ) {
                    applyPendingResumePosition()
                }

                if (
                    playbackState ==
                    Player.STATE_ENDED
                ) {
                    handlePlaybackEnded()
                }

                updatePauseButton()
                updateCenterPlayButton()
                updateProgress()
            }

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {
                updatePauseButton()
                updateCenterPlayButton()
                updateProgress()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                resumePositionApplied = false
                pendingResumePosition = 0L

                val uri =
                    mediaItem?.localConfiguration?.uri

                if (uri != null) {
                    updateFavoriteButton()
                }

                updateProgress()
            }
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

        topBar =
            findViewById(R.id.topBar)

        progressPanel =
            findViewById(R.id.progressPanel)

        progressSeekBar =
            findViewById(R.id.progressSeekBar)

        currentTimeText =
            findViewById(R.id.currentTimeText)

        remainingTimeText =
            findViewById(R.id.remainingTimeText)

        totalTimeText =
            findViewById(R.id.totalTimeText)

        previousButton =
            findViewById(R.id.previousButton)

        pauseButton =
            findViewById(R.id.pauseButton)

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

        backButton =
            findViewById(R.id.backButton)

        centerPreviousButton =
            findViewById(
                R.id.centerPreviousButton
            )

        centerPlayButton =
            findViewById(
                R.id.centerPlayButton
            )

        centerNextButton =
            findViewById(
                R.id.centerNextButton
            )

        controlScroll =
            findViewById(R.id.controlScroll)

        gestureInfo =
            findViewById(R.id.gestureInfo)

        lockedOverlay =
            findViewById(R.id.lockedOverlay)

        audioManager =
            getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        playbackAutoSaveManager =
            PlaybackAutoSaveManager(
                this,
                { player },
                { getIncomingVideoUri() },
                { isExternalVideo() }
            )

        playerView.useController = false

        setupProgressBar()
        loadSavedDisplaySettings()
        setupButtons()
        setupCenterButtons()
        setupGestures()
        setupCastButton()
        setupLockedOverlay()

        connectToPlaybackService()

        enterFullscreen()

        playbackAutoSaveManager.start()

        progressHandler.removeCallbacks(
            progressRunnable
        )

        progressHandler.post(
            progressRunnable
        )
    }

    private fun setupProgressBar() {

        progressSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onStartTrackingTouch(
                    seekBar: SeekBar
                ) {
                    progressUserSeeking = true
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar
                ) {

                    val currentPlayer =
                        player ?: run {
                            progressUserSeeking = false
                            return
                        }

                    val duration =
                        currentPlayer.duration

                    if (
                        duration <= 0L ||
                        duration == C.TIME_UNSET
                    ) {
                        progressUserSeeking = false
                        return
                    }

                    val target =
                        (
                            duration *
                                seekBar.progress
                            ).toLong() /
                            1000L

                    currentPlayer.seekTo(
                        target.coerceIn(
                            0L,
                            duration
                        )
                    )

                    progressUserSeeking = false

                    updateProgress()

                    showPlayerControlsTemporarily()
                }

                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    if (!fromUser) {
                        return
                    }

                    val duration =
                        player?.duration
                            ?: return

                    if (
                        duration <= 0L ||
                        duration == C.TIME_UNSET
                    ) {
                        return
                    }

                    val position =
                        duration *
                            progress.toLong() /
                            1000L

                    currentTimeText.text =
                        formatTime(position)

                    remainingTimeText.text =
                        "-${formatTime(
                            (
                                duration -
                                    position
                                ).coerceAtLeast(0L)
                        )}"
                }
            }
        )
    }

    private fun loadSavedDisplaySettings() {

        val savedVolume =
            getSharedPreferences(
                DISPLAY_PREFS,
                Context.MODE_PRIVATE
            ).getInt(
                KEY_VOLUME,
                -1
            )

        if (savedVolume >= 0) {

            val maxVolume =
                audioManager.getStreamMaxVolume(
                    AudioManager.STREAM_MUSIC
                )

            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                savedVolume.coerceIn(
                    0,
                    maxVolume
                ),
                0
            )
        }

        val savedBrightness =
            getSharedPreferences(
                DISPLAY_PREFS,
                Context.MODE_PRIVATE
            ).getFloat(
                KEY_BRIGHTNESS,
                -1f
            )

        if (savedBrightness >= 0f) {

            val attributes =
                window.attributes

            attributes.screenBrightness =
                savedBrightness.coerceIn(
                    0.05f,
                    1.0f
                )

            window.attributes =
                attributes
        }
    }

    private fun saveDisplaySettings() {

        val currentBrightness =
            window.attributes.screenBrightness
                .takeIf {
                    it >= 0f
                }
                ?: 0.5f

        getSharedPreferences(
            DISPLAY_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putInt(
                KEY_VOLUME,
                audioManager.getStreamVolume(
                    AudioManager.STREAM_MUSIC
                )
            )
            .putFloat(
                KEY_BRIGHTNESS,
                currentBrightness
            )
            .apply()
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

    private fun setupLockedOverlay() {

        lockedOverlay.setOnClickListener {

            if (isLocked) {

                isLocked = false

                lockedOverlay.visibility =
                    View.GONE

                lockButton.text =
                    getString(
                        R.string.lock
                    )

                showPlayerControlsTemporarily()
            }
        }
    }

    private fun setupCenterButtons() {

        centerPreviousButton.setOnClickListener {

            if (!isLocked) {

                playPreviousVideo()

                showPlayerControlsTemporarily()
            }
        }

        centerPlayButton.setOnClickListener {

            if (!isLocked) {

                val currentPlayer =
                    player
                        ?: return@setOnClickListener

                if (currentPlayer.isPlaying) {
                    currentPlayer.pause()
                } else {
                    currentPlayer.play()
                }

                updatePauseButton()
                updateCenterPlayButton()
                showPlayerControlsTemporarily()
            }
        }

        centerNextButton.setOnClickListener {

            if (!isLocked) {

                playNextVideo()

                showPlayerControlsTemporarily()
            }
        }
    }

    private fun updateCenterPlayButton() {

        if (!::centerPlayButton.isInitialized) {
            return
        }

        centerPlayButton.text =
            if (player?.isPlaying == true) {
                "⏸"
            } else {
                "▶"
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

                    controller.addListener(
                        playbackListener
                    )

                    playerView.player =
                        controller

                    initializePlayer()

                    updatePauseButton()
                    updateCenterPlayButton()
                    updateProgress()

                    showPlayerControlsTemporarily()

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

        backButton.setOnClickListener {

            if (!isLocked) {
                exitPlayer()
            }
        }

        pauseButton.setOnClickListener {

            if (!isLocked) {

                val currentPlayer =
                    player
                        ?: return@setOnClickListener

                if (currentPlayer.isPlaying) {
                    currentPlayer.pause()
                } else {
                    currentPlayer.play()
                }

                updatePauseButton()
                updateCenterPlayButton()
                showPlayerControlsTemporarily()
            }
        }

        previousButton.setOnClickListener {

            if (!isLocked) {
                playPreviousVideo()
                showPlayerControlsTemporarily()
            }
        }

        nextButton.setOnClickListener {

            if (!isLocked) {
                playNextVideo()
                showPlayerControlsTemporarily()
            }
        }

        repeatButton.setOnClickListener {

            if (!isLocked) {
                toggleRepeat()
                showPlayerControlsTemporarily()
            }
        }

        audioButton.setOnClickListener {

            if (!isLocked) {
                showAudioTrackDialog()
                showPlayerControlsTemporarily()
            }
        }

        favoriteButton.setOnClickListener {

            if (!isLocked) {
                toggleFavorite()
                showPlayerControlsTemporarily()
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

            showPlayerControlsTemporarily()
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

                showPlayerControlsTemporarily()
            }
        }

        sleepTimerButton.setOnClickListener {

            if (!isLocked) {
                showSleepTimerDialog()
                showPlayerControlsTemporarily()
            }
        }

        shareButton.setOnClickListener {

            if (!isLocked) {
                shareCurrentVideo()
                showPlayerControlsTemporarily()
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

            if (!isLocked) {
                enterFullscreen()
                showPlayerControlsTemporarily()
            }
        }

        moreButton.setOnClickListener {

            if (!isLocked) {
                showMoreMenu()
                showPlayerControlsTemporarily()
            }
        }

        currentSpeed =
            getCurrentVideoSpeed()

        updateSpeedText()
        updateRepeatButton()
        updateFavoriteButton()
        updatePauseButton()
        updateCenterPlayButton()
    }

    private fun getCurrentVideoSpeed(): Float {

        val uri =
            getIncomingVideoUri()
                ?: return 1.0f

        if (isExternalVideo()) {
            return 1.0f
        }

        return VideoSpeedManager.getSpeed(
            this,
            uri
        )
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
            .setTitle("امکانات بیشتر")
            .setItems(options) { _, which ->

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

                    6 -> {

                        PlaybackQueueManager.clear(
                            this
                        )

                        showTemporaryMessage(
                            "صف پخش پاک شد."
                        )
                    }
                }
            }
            .show()
    }

    private fun showPlaybackSettings() {

        val currentAutoPlayNext =
            PlaybackSettings.autoPlayNext(this)

        val currentAutoResume =
            VidoraSettings.autoResume(this)

        AlertDialog.Builder(this)
            .setTitle("تنظیمات پخش")
            .setMultiChoiceItems(
                arrayOf(
                    "پخش خودکار ویدئوی بعدی",
                    "ادامه پخش از آخرین موقعیت"
                ),
                booleanArrayOf(
                    currentAutoPlayNext,
                    currentAutoResume
                )
            ) { _, which, checked ->

                when (which) {

                    0 ->
                        PlaybackSettings.setAutoPlayNext(
                            this,
                            checked
                        )

                    1 ->
                        VidoraSettings.setAutoResume(
                            this,
                            checked
                        )
                }
            }
            .setPositiveButton(
                "باشه",
                null
            )
            .show()
    }

    private fun handlePlaybackEnded() {

        if (isExternalVideo()) {
            return
        }

        if (repeatEnabled) {
            return
        }

        if (!PlaybackSettings.autoPlayNext(this)) {
            updatePauseButton()
            updateCenterPlayButton()
            showPlayerControlsTemporarily()
            return
        }

        val currentUri =
            getIncomingVideoUri()

        if (currentUri != null) {

            val queue =
                PlaybackQueueManager.getQueue(this)

            val currentQueueIndex =
                queue.indexOfFirst {
                    it.toString() ==
                        currentUri.toString()
                }

            if (currentQueueIndex >= 0) {

                PlaybackQueueManager.setCurrentIndex(
                    this,
                    currentQueueIndex
                )

                val nextUri =
                    PlaybackQueueManager.getNext(
                        this,
                        false
                    )

                if (nextUri != null) {

                    PlaybackQueueManager.setCurrentVideo(
                        this,
                        nextUri
                    )

                    openVideo(nextUri)

                    showTemporaryMessage(
                        "ویدئوی بعدی صف"
                    )

                    return
                }

                updatePauseButton()
                updateCenterPlayButton()
                showPlayerControlsTemporarily()

                showTemporaryMessage(
                    "صف پخش به پایان رسید."
                )

                return
            }
        }

        val videos =
            getVideoUris()

        if (videos.isEmpty()) {
            updatePauseButton()
            updateCenterPlayButton()
            return
        }

        val currentIndex =
            currentUri?.let { uri ->
                videos.indexOfFirst {
                    it.toString() ==
                        uri.toString()
                }
            } ?: -1

        val nextIndex =
            when {

                currentIndex < 0 ->
                    0

                currentIndex >= videos.lastIndex ->
                    0

                else ->
                    currentIndex + 1
            }

        openVideo(
            videos[nextIndex]
        )

        showTemporaryMessage(
            "ویدئوی بعدی"
        )
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

                hint = "میلی‌ثانیه"
            }

        AlertDialog.Builder(this)
            .setTitle("همگام‌سازی صدا")
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

                hint = "میلی‌ثانیه"
            }

        AlertDialog.Builder(this)
            .setTitle("همگام‌سازی زیرنویس")
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
                .setTitle("کیفیت تصویر")
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

        val added =
            PlaybackQueueManager.addAndSetCurrent(
                this,
                uri
            )

        showTemporaryMessage(
            if (added) {
                "ویدئو به صف پخش اضافه شد."
            } else {
                "ویدئو از قبل در صف پخش بود."
            }
        )
    }

    private fun showQueue() {

        val queue =
            PlaybackQueueManager.getQueue(this)

        if (queue.isEmpty()) {

            AlertDialog.Builder(this)
                .setTitle("صف پخش")
                .setMessage("صف پخش خالی است.")
                .setPositiveButton(
                    "باشه",
                    null
                )
                .show()

            return
        }

        val currentUri =
            getIncomingVideoUri()

        val names =
            queue.mapIndexed { index, uri ->

                val marker =
                    if (
                        currentUri != null &&
                        currentUri.toString() ==
                        uri.toString()
                    ) {
                        " ▶ "
                    } else {
                        ""
                    }

                "${index + 1}.$marker${getVideoName(uri)}"

            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("صف پخش")
            .setItems(names) { _, which ->

                if (which !in queue.indices) {
                    return@setItems
                }

                val selectedUri =
                    queue[which]

                PlaybackQueueManager.setCurrentIndex(
                    this,
                    which
                )

                openVideo(selectedUri)

                showTemporaryMessage(
                    "در حال پخش از صف"
                )
            }
            .setNegativeButton(
                "بستن",
                null
            )
            .show()
    }

    private fun getIncomingVideoUri(): Uri? {

        if (intent.action == Intent.ACTION_VIEW) {
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

        val uri =
            getIncomingVideoUri()
                ?: return

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

        VideoSpeedManager.setSpeed(
            this,
            uri,
            currentSpeed
        )

        player?.playbackParameters =
            PlaybackParameters(
                currentSpeed
            )

        updateSpeedText()
        showPlayerControlsTemporarily()
    }

    private fun resetSpeed() {

        val uri =
            getIncomingVideoUri()
                ?: return

        currentSpeed =
            1.0f

        VideoSpeedManager.setSpeed(
            this,
            uri,
            currentSpeed
        )

        player?.playbackParameters =
            PlaybackParameters(
                currentSpeed
            )

        updateSpeedText()
        showPlayerControlsTemporarily()
    }

    private fun updateSpeedText() {

        speedButton.text =
            String.format(
                java.util.Locale.US,
                "%.1f×",
                currentSpeed
            )
    }

    private fun updatePauseButton() {

        if (!::pauseButton.isInitialized) {
            return
        }

        pauseButton.text =
            if (player?.isPlaying == true) {
                "⏸ توقف"
            } else {
                "▶ پخش"
            }
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

        container.addView(input)

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

        sleepTimerRunnable =
            Runnable {

                player?.pause()

                updatePauseButton()
                updateCenterPlayButton()

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

        if (::sleepTimerButton.isInitialized) {

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

                            togglePlayerControls()
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

    private fun togglePlayerControls() {

        if (isLocked) {
            return
        }

        if (controlsVisible) {
            hidePlayerControls()
        } else {
            showPlayerControlsTemporarily()
        }
    }

    private fun showPlayerControlsTemporarily() {

        if (
            isLocked ||
            isInPictureInPictureMode
        ) {
            return
        }

        controlsHandler.removeCallbacks(
            hideControlsRunnable
        )

        setPlayerControlsVisibility(true)

        controlsVisible = true

        controlsHandler.postDelayed(
            hideControlsRunnable,
            CONTROL_HIDE_DELAY
        )
    }

    private fun hidePlayerControls() {

        controlsHandler.removeCallbacks(
            hideControlsRunnable
        )

        setPlayerControlsVisibility(false)

        controlsVisible = false
    }

    private fun handleDoubleTap(
        x: Float
    ) {

        val currentPlayer =
            player ?: return

        val duration =
            currentPlayer.duration

        if (
            duration <= 0L ||
            duration == C.TIME_UNSET
        ) {
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
                    ).coerceAtMost(duration)
            }

        currentPlayer.seekTo(newPosition)

        updateProgress()

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

        if (
            duration <= 0L ||
            duration == C.TIME_UNSET
        ) {
            return
        }

        val width =
            playerView.width
                .coerceAtLeast(1)

        val seekAmount =
            (
                deltaX /
                    width
                ) * 60000L

        val newPosition =
            (
                startPosition +
                    seekAmount.toLong()
                ).coerceIn(
                    0L,
                    duration
                )

        currentPlayer.seekTo(newPosition)

        val seconds =
            seekAmount.toLong() / 1000

        val sign =
            if (seconds >= 0) "+" else ""

        showGestureInfo(
            "$sign${seconds}s"
        )

        updateProgress()
    }

    private fun handleVolume(
        deltaY: Float
    ) {

        val maxVolume =
            audioManager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        val height =
            playerView.height
                .coerceAtLeast(1)

        val volumeChange =
            (
                -deltaY /
                    height *
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

        saveDisplaySettings()

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

        val height =
            playerView.height
                .coerceAtLeast(1)

        val change =
            -deltaY /
                height

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

        saveDisplaySettings()

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

            hidePlayerControls()

        } else {

            lockedOverlay.visibility =
                View.GONE

            lockButton.text =
                getString(
                    R.string.lock
                )

            showPlayerControlsTemporarily()
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

        topBar.visibility = visibility
        progressPanel.visibility = visibility
        backButton.visibility = visibility
        pauseButton.visibility = visibility
        previousButton.visibility = visibility
        nextButton.visibility = visibility
        repeatButton.visibility = visibility
        audioButton.visibility = visibility
        favoriteButton.visibility = visibility
        speedMinusButton.visibility = visibility
        speedButton.visibility = visibility
        speedPlusButton.visibility = visibility
        aspectButton.visibility = visibility
        subtitleButton.visibility = visibility
        sleepTimerButton.visibility = visibility
        shareButton.visibility = visibility
        deleteButton.visibility = visibility
        lockButton.visibility = visibility
        fullscreenButton.visibility = visibility
        moreButton.visibility = visibility
        controlScroll.visibility = visibility

        centerPreviousButton.visibility =
            visibility

        centerPlayButton.visibility =
            visibility

        centerNextButton.visibility =
            visibility

        if (visible) {
            updatePauseButton()
            updateCenterPlayButton()
            updateProgress()
        }
    }

    private fun initializePlayer() {

        val uri =
            getIncomingVideoUri()
                ?: return

        if (isExternalVideo()) {

            deleteButton.visibility =
                View.GONE

            createPlayer(
                MediaItem.fromUri(uri)
            )

            return
        }

        syncQueueCurrentVideo(uri)

        val savedSubtitle =
            SubtitleFileManager.getSubtitleUri(
                this,
                uri
            )

        if (savedSubtitle != null) {

            createPlayerWithSubtitle(
                uri,
                savedSubtitle
            )

        } else {

            createPlayer(
                MediaItem.fromUri(uri)
            )
        }
    }

    private fun loadVideoWithSubtitle(
        subtitleUri: Uri
    ) {

        val videoUri =
            getIncomingVideoUri()
                ?: return

        SubtitleFileManager.setSubtitleUri(
            this,
            videoUri,
            subtitleUri
        )

        createPlayerWithSubtitle(
            videoUri,
            subtitleUri
        )
    }

    private fun createPlayerWithSubtitle(
        videoUri: Uri,
        subtitleUri: Uri
    ) {

        val subtitleMimeType =
            getSubtitleMimeType(subtitleUri)

        val subtitleLanguage =
            detectSubtitleLanguage(subtitleUri)

        val subtitle =
            MediaItem.SubtitleConfiguration
                .Builder(subtitleUri)
                .setMimeType(subtitleMimeType)
                .setLanguage(subtitleLanguage)
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

    private fun getSubtitleMimeType(
        subtitleUri: Uri
    ): String {

        val detectedType =
            try {
                contentResolver.getType(
                    subtitleUri
                )
            } catch (_: Exception) {
                null
            }

        if (detectedType == MimeTypes.TEXT_VTT) {
            return MimeTypes.TEXT_VTT
        }

        if (
            detectedType ==
            MimeTypes.APPLICATION_SUBRIP
        ) {
            return MimeTypes.APPLICATION_SUBRIP
        }

        val name =
            getDisplayName(
                subtitleUri
            ).lowercase()

        return when {

            name.endsWith(".vtt") ->
                MimeTypes.TEXT_VTT

            name.endsWith(".srt") ->
                MimeTypes.APPLICATION_SUBRIP

            else ->
                MimeTypes.APPLICATION_SUBRIP
        }
    }

    private fun detectSubtitleLanguage(
        subtitleUri: Uri
    ): String {

        val name =
            getDisplayName(
                subtitleUri
            ).lowercase()

        return when {

            name.contains(".fa.") ||
                name.contains("_fa.") ||
                name.contains("-fa.") ->
                "fa"

            name.contains(".en.") ||
                name.contains("_en.") ||
                name.contains("-en.") ->
                "en"

            name.contains(".ar.") ||
                name.contains("_ar.") ||
                name.contains("-ar.") ->
                "ar"

            name.contains(".de.") ||
                name.contains("_de.") ||
                name.contains("-de.") ->
                "de"

            name.contains(".fr.") ||
                name.contains("_fr.") ||
                name.contains("-fr.") ->
                "fr"

            else ->
                "fa"
        }
    }

    private fun getDisplayName(
        uri: Uri
    ): String {

        return try {

            contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                        ?: uri.lastPathSegment
                        ?: "subtitle"
                } else {
                    uri.lastPathSegment
                        ?: "subtitle"
                }
            } ?: (
                uri.lastPathSegment
                    ?: "subtitle"
                )

        } catch (_: Exception) {

            uri.lastPathSegment
                ?: "subtitle"
        }
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

        if (!isExternalVideo()) {
            syncQueueCurrentVideo(videoUri)
        }

        pendingResumePosition =
            if (
                isExternalVideo() ||
                !VidoraSettings.autoResume(this)
            ) {
                0L
            } else {
                PlaybackHistoryManager
                    .getPosition(
                        this,
                        videoUri
                    )
                    .coerceAtLeast(0L)
            }

        resumePositionApplied =
            pendingResumePosition <= 0L

        currentSpeed =
            if (isExternalVideo()) {
                1.0f
            } else {
                VideoSpeedManager.getSpeed(
                    this,
                    videoUri
                )
            }

        currentPlayer.pause()

        currentPlayer.setMediaItem(
            mediaItem
        )

        currentPlayer.repeatMode =
            if (repeatEnabled) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }

        currentPlayer.playbackParameters =
            PlaybackParameters(
                currentSpeed
            )

        currentPlayer.prepare()

        updateSpeedText()

        intent.putExtra(
            EXTRA_VIDEO_URI,
            videoUri.toString()
        )

        updateFavoriteButton()
        updatePauseButton()
        updateCenterPlayButton()
        updateProgress()

        showPlayerControlsTemporarily()

        if (pendingResumePosition <= 0L) {
            currentPlayer.play()
        }
    }

    private fun applyPendingResumePosition() {

        if (resumePositionApplied) {
            return
        }

        val currentPlayer =
            player
                ?: return

        val position =
            pendingResumePosition

        resumePositionApplied = true
        pendingResumePosition = 0L

        if (position <= 0L) {
            currentPlayer.play()
            return
        }

        val duration =
            currentPlayer.duration

        val safePosition =
            if (
                duration > 0L &&
                duration != C.TIME_UNSET
            ) {
                position.coerceIn(
                    0L,
                    (duration - 500L)
                        .coerceAtLeast(0L)
                )
            } else {
                position
            }

        try {

            currentPlayer.seekTo(
                safePosition
            )

        } catch (_: Exception) {
        }

        currentPlayer.play()

        updateProgress()
        updatePauseButton()
        updateCenterPlayButton()
    }

    private fun updateProgress() {

        if (!::progressSeekBar.isInitialized) {
            return
        }

        val currentPlayer =
            player

        if (currentPlayer == null) {

            currentTimeText.text = "00:00"
            remainingTimeText.text = "-00:00"
            totalTimeText.text = "/ 00:00"
            progressSeekBar.progress = 0

            return
        }

        val duration =
            currentPlayer.duration

        val position =
            currentPlayer.currentPosition
                .coerceAtLeast(0L)

        if (
            duration <= 0L ||
            duration == C.TIME_UNSET
        ) {

            currentTimeText.text =
                formatTime(position)

            remainingTimeText.text =
                "-00:00"

            totalTimeText.text =
                "/ 00:00"

            if (!progressUserSeeking) {
                progressSeekBar.progress = 0
            }

            return
        }

        val safePosition =
            position.coerceIn(
                0L,
                duration
            )

        currentTimeText.text =
            formatTime(safePosition)

        remainingTimeText.text =
            "-${formatTime(
                (duration - safePosition)
                    .coerceAtLeast(0L)
            )}"

        totalTimeText.text =
            "/ ${formatTime(duration)}"

        if (!progressUserSeeking) {

            progressSeekBar.progress =
                (
                    safePosition *
                        1000L /
                        duration
                    ).toInt()
                        .coerceIn(
                            0,
                            1000
                        )
        }
    }

    private fun formatTime(
        milliseconds: Long
    ): String {

        val totalSeconds =
            milliseconds.coerceAtLeast(0L) /
                1000L

        val seconds =
            totalSeconds % 60

        val minutes =
            (totalSeconds / 60) % 60

        val hours =
            totalSeconds / 3600

        return if (hours > 0L) {

            String.format(
                java.util.Locale.US,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )

        } else {

            String.format(
                java.util.Locale.US,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }

    private fun syncQueueCurrentVideo(
        uri: Uri
    ) {

        if (isExternalVideo()) {
            return
        }

        val queue =
            PlaybackQueueManager.getQueue(this)

        if (queue.isEmpty()) {
            return
        }

        val index =
            queue.indexOfFirst {
                it.toString() ==
                    uri.toString()
            }

        if (index >= 0) {

            PlaybackQueueManager.setCurrentIndex(
                this,
                index
            )
        }
    }

    private fun isCurrentVideoInQueue(): Boolean {

        if (isExternalVideo()) {
            return false
        }

        val currentUri =
            getIncomingVideoUri()
                ?: return false

        return PlaybackQueueManager
            .getQueue(this)
            .any {
                it.toString() ==
                    currentUri.toString()
            }
    }

    private fun getVideoUris(): List<Uri> {

        if (
            ContextCompat.checkSelfPermission(
                this,
                requiredVideoPermission()
            ) != PackageManager.PERMISSION_GRANTED
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
                        cursor.getLong(idColumn).toString()
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

        val currentUri =
            getIncomingVideoUri()

        if (
            currentUri != null &&
            isCurrentVideoInQueue()
        ) {

            val previousUri =
                PlaybackQueueManager.getPrevious(
                    this,
                    true
                )

            if (previousUri != null) {

                PlaybackQueueManager.setCurrentVideo(
                    this,
                    previousUri
                )

                openVideo(previousUri)

                showTemporaryMessage(
                    "ویدئوی قبلی صف"
                )

                return
            }
        }

        val videos =
            getVideoUris()

        if (videos.isEmpty()) {
            return
        }

        val currentIndex =
            currentUri?.let { uri ->
                videos.indexOfFirst {
                    it.toString() ==
                        uri.toString()
                }
            } ?: -1

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

        val currentUri =
            getIncomingVideoUri()

        if (
            currentUri != null &&
            isCurrentVideoInQueue()
        ) {

            val nextUri =
                PlaybackQueueManager.getNext(
                    this,
                    true
                )

            if (nextUri != null) {

                PlaybackQueueManager.setCurrentVideo(
                    this,
                    nextUri
                )

                openVideo(nextUri)

                showTemporaryMessage(
                    "ویدئوی بعدی صف"
                )

                return
            }
        }

        val videos =
            getVideoUris()

        if (videos.isEmpty()) {
            return
        }

        val currentIndex =
            currentUri?.let { uri ->
                videos.indexOfFirst {
                    it.toString() ==
                        uri.toString()
                }
            } ?: -1

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

        syncQueueCurrentVideo(uri)

        val savedSubtitle =
            SubtitleFileManager.getSubtitleUri(
                this,
                uri
            )

        if (savedSubtitle != null) {

            createPlayerWithSubtitle(
                uri,
                savedSubtitle
            )

        } else {

            createPlayer(
                MediaItem.fromUri(uri)
            )
        }

        showPlayerControlsTemporarily()
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

        val uri =
            getIncomingVideoUri()
                ?: return

        val currentPlayer =
            player
                ?: return

        val position =
            currentPlayer.currentPosition

        val duration =
            currentPlayer.duration

        if (
            position <= 0L ||
            duration <= 0L ||
            duration == C.TIME_UNSET
        ) {
            return
        }

        PlaybackHistoryManager.save(
            this,
            uri,
            position,
            duration
        )
    }

    private fun stopPlaybackCompletely() {

        val currentPlayer =
            player
                ?: return

        try {
            currentPlayer.pause()
        } catch (_: Exception) {
        }

        try {
            currentPlayer.stop()
        } catch (_: Exception) {
        }

        try {
            currentPlayer.clearMediaItems()
        } catch (_: Exception) {
        }

        wasPlayingBeforePause = false
    }

    private fun exitPlayer() {

        if (isExitingPlayer) {
            return
        }

        isExitingPlayer = true

        wasPlayingBeforePause = false
        enteringPictureInPicture = false

        savePosition()

        playbackAutoSaveManager.stop()

        saveDisplaySettings()

        progressHandler.removeCallbacks(
            progressRunnable
        )

        controlsHandler.removeCallbacks(
            hideControlsRunnable
        )

        stopPlaybackCompletely()

        playerView.player = null

        finish()
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

        val uri =
            getIncomingVideoUri()
                ?: return

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

                    SubtitleFileManager.removeSubtitle(
                        this,
                        uri
                    )

                    PlaybackQueueManager.remove(
                        this,
                        uri
                    )

                    VideoSpeedManager.clear(
                        this,
                        uri
                    )

                    showTemporaryMessage(
                        getString(
                            R.string.video_deleted
                        )
                    )

                    exitPlayer()
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

            val uri =
                getIncomingVideoUri()

            if (uri != null) {

                PlaybackHistoryManager.clear(
                    this,
                    uri
                )

                SubtitleFileManager.removeSubtitle(
                    this,
                    uri
                )

                PlaybackQueueManager.remove(
                    this,
                    uri
                )

                VideoSpeedManager.clear(
                    this,
                    uri
                )
            }

            exitPlayer()
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

        enteringPictureInPicture = true

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
            Build.VERSION_CODES.O &&
            !isExitingPlayer
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

        enteringPictureInPicture =
            isInPictureInPictureMode

        if (isInPictureInPictureMode) {

            hidePlayerControls()

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
                showPlayerControlsTemporarily()
            }
        }
    }

    override fun onBackPressed() {

        if (isLocked) {
            return
        }

        exitPlayer()
    }

    override fun onPause() {

        if (!isExitingPlayer) {

            playbackAutoSaveManager.saveNow()
            savePosition()
            saveDisplaySettings()

            val currentPlayer =
                player

            if (
                currentPlayer != null &&
                currentPlayer.isPlaying &&
                !isInPictureInPictureMode &&
                !enteringPictureInPicture
            ) {

                wasPlayingBeforePause = true

                currentPlayer.pause()

                updatePauseButton()
                updateCenterPlayButton()
            }
        }

        super.onPause()
    }

    override fun onResume() {

        super.onResume()

        loadSavedDisplaySettings()

        if (
            isExitingPlayer ||
            enteringPictureInPicture
        ) {
            return
        }

        val currentPlayer =
            player

        if (
            wasPlayingBeforePause &&
            currentPlayer != null
        ) {

            currentPlayer.play()

            wasPlayingBeforePause = false

            updatePauseButton()
            updateCenterPlayButton()
        }

        if (!isLocked) {
            showPlayerControlsTemporarily()
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

        if (!isExitingPlayer) {

            playbackAutoSaveManager.saveNow()
            savePosition()
            saveDisplaySettings()
        }

        super.onStop()
    }

    override fun onDestroy() {

        progressHandler.removeCallbacks(
            progressRunnable
        )

        controlsHandler.removeCallbacks(
            hideControlsRunnable
        )

        sleepTimerRunnable?.let {
            sleepHandler.removeCallbacks(it)
        }

        sleepTimerRunnable = null

        if (!isExitingPlayer) {
            playbackAutoSaveManager.release()
            savePosition()
            saveDisplaySettings()
        } else {
            playbackAutoSaveManager.stop()
        }

        player?.removeListener(
            playbackListener
        )

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

        private const val DISPLAY_PREFS =
            "vidora_display_settings"

        private const val KEY_VOLUME =
            "volume"

        private const val KEY_BRIGHTNESS =
            "brightness"

        private const val CONTROL_HIDE_DELAY =
            4000L

        private const val PROGRESS_UPDATE_INTERVAL =
            500L
    }
}
