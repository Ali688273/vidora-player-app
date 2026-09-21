package com.vidora.player

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView

import com.google.common.util.concurrent.ListenableFuture

@OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity : FragmentActivity() {

    internal lateinit var playerView: PlayerView

    internal lateinit var previousButton: Button
    internal lateinit var nextButton: Button
    internal lateinit var repeatButton: Button
    internal lateinit var audioButton: Button
    internal lateinit var favoriteButton: Button
    internal lateinit var speedMinusButton: Button
    internal lateinit var speedButton: Button
    internal lateinit var speedPlusButton: Button
    internal lateinit var aspectButton: Button
    internal lateinit var subtitleButton: Button
    internal lateinit var sleepTimerButton: Button
    internal lateinit var shareButton: Button
    internal lateinit var deleteButton: Button
    internal lateinit var lockButton: Button
    internal lateinit var fullscreenButton: Button
    internal lateinit var moreButton: Button

    internal lateinit var backButton: Button
    internal lateinit var pauseButton: Button

    internal lateinit var centerPreviousButton: Button
    internal lateinit var centerPlayButton: Button
    internal lateinit var centerNextButton: Button

    internal lateinit var topBar: View
    internal lateinit var controlScroll: View
    internal lateinit var progressPanel: View
    internal lateinit var progressSeekBar: SeekBar

    internal lateinit var currentTimeText: TextView
    internal lateinit var remainingTimeText: TextView
    internal lateinit var totalTimeText: TextView

    internal lateinit var gestureInfo: TextView
    internal lateinit var speedIndicator: TextView
    internal lateinit var lockedOverlay: TextView

    internal var player: Player? = null

    internal var controllerFuture:
        ListenableFuture<MediaController>? = null

    internal lateinit var playbackAutoSaveManager:
        PlaybackAutoSaveManager

    internal var isLocked = false
    internal var currentSpeed = 1.0f
    internal var aspectIndex = 0
    internal var repeatEnabled = false

    internal var controlsVisible = true
    internal var isExitingPlayer = false
    internal var progressUserSeeking = false

    internal var pendingResumePosition = 0L
    internal var resumePositionApplied = false

    internal val controlsHandler =
        Handler(Looper.getMainLooper())

    internal val progressHandler =
        Handler(Looper.getMainLooper())

    internal val hideControlsRunnable =
        Runnable {
            if (
                !isLocked &&
                !isInPictureInPictureMode
            ) {
                hidePlayerControls()
            }
        }

    internal val progressRunnable =
        object : Runnable {

            override fun run() {
                updateProgress()

                progressHandler.postDelayed(
                    this,
                    PROGRESS_UPDATE_INTERVAL
                )
            }
        }

    internal val audioManager: AudioManager by lazy {
        getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager
    }

    internal val sleepHandler =
        Handler(Looper.getMainLooper())

    internal var sleepTimerRunnable: Runnable? =
        null

    internal var lastTapTime = 0L
    internal var lastTapX = 0f
    internal var lastTapY = 0f

    internal var wasPlayingBeforePause = false
    internal var enteringPictureInPicture = false

    /**
     * وقتی پنجره تبلیغ روی Activity می‌آید،
     * بعضی SDKها Activity را pause نمی‌کنند ولی focus پنجره را می‌گیرند.
     * این متغیر اجازه می‌دهد بعد از بسته‌شدن تبلیغ فقط در صورتی
     * که ویدئو قبل از تبلیغ در حال پخش بوده، دوباره پخش شود.
     */
    internal var wasPlayingBeforeWindowFocusLoss = false

    internal var downX = 0f
    internal var downY = 0f
    internal var startPosition = 0L
    internal var startVolume = 0
    internal var startBrightness = 0.5f

    internal var gestureMode =
        GestureMode.NONE

    internal enum class GestureMode {
        NONE,
        SEEK,
        VOLUME,
        BRIGHTNESS
    }

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
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
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

    internal fun subtitlePickerInternalLaunch() {
        subtitlePicker.launch(
            arrayOf(
                "text/*",
                "application/x-subrip",
                "text/vtt"
            )
        )
    }

    internal val playbackListener =
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

                /*
                 * مهم:
                 * قبلاً اینجا pendingResumePosition صفر می‌شد.
                 * چون Media3 هنگام setMediaItem/prepare می‌تواند
                 * همین callback را قبل از STATE_READY صدا بزند،
                 * موقعیت ذخیره‌شده از بین می‌رفت.
                 *
                 * createPlayer() مسئول تعیین موقعیت Resume است
                 * و applyPendingResumePosition() آن را اعمال می‌کند.
                 */
                val uri =
                    mediaItem
                        ?.localConfiguration
                        ?.uri

                if (uri != null) {
                    updateFavoriteButton()
                }

                updateProgress()
            }
        }

    override fun attachBaseContext(
        newBase: Context
    ) {
        super.attachBaseContext(
            VidoraLocaleManager.apply(
                newBase
            )
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(
            R.layout.activity_player
        )

        bindViews()

        VidoraLocaleManager.applyDirection(
            window.decorView,
            this
        )

        playbackAutoSaveManager =
            PlaybackAutoSaveManager(
                this,
                { player },
                { getIncomingVideoUri() },
                { isExternalVideo() }
            )

        setupProgressBar()
        loadSavedDisplaySettings()

        setupPlayerButtons()
        setupPlayerCenterButtons()
        setupPlayerGestures()
        setupLockedOverlay()
        setupCastButton()

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

    private fun bindViews() {

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
            findViewById(R.id.centerPreviousButton)

        centerPlayButton =
            findViewById(R.id.centerPlayButton)

        centerNextButton =
            findViewById(R.id.centerNextButton)

        controlScroll =
            findViewById(R.id.controlScroll)

        gestureInfo =
            findViewById(R.id.gestureInfo)

        speedIndicator =
            findViewById(R.id.speedIndicator)

        lockedOverlay =
            findViewById(R.id.lockedOverlay)
    }

    internal fun isPersian(): Boolean {
        return VidoraLanguageManager.isPersian(this)
    }

    internal fun p(
        persian: String,
        english: String
    ): String {
        return if (isPersian()) {
            persian
        } else {
            english
        }
    }

    internal fun setupProgressBar() {

        progressSeekBar.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

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
                        duration ==
                        androidx.media3.common.C.TIME_UNSET
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
                        duration ==
                        androidx.media3.common.C.TIME_UNSET
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
                        "-${
                            formatTime(
                                (
                                    duration -
                                        position
                                ).coerceAtLeast(
                                    0L
                                )
                            )
                        }"
                }
            }
        )
    }

    internal fun updateProgress() {

        if (!::progressSeekBar.isInitialized) {
            return
        }

        if (!::currentTimeText.isInitialized) {
            return
        }

        if (!::remainingTimeText.isInitialized) {
            return
        }

        if (!::totalTimeText.isInitialized) {
            return
        }

        val currentPlayer =
            player ?: return

        val duration =
            currentPlayer.duration

        if (
            duration <= 0L ||
            duration ==
            androidx.media3.common.C.TIME_UNSET
        ) {

            if (!progressUserSeeking) {
                progressSeekBar.progress = 0
            }

            currentTimeText.text =
                formatTime(0L)

            remainingTimeText.text =
                "-${formatTime(0L)}"

            totalTimeText.text =
                formatTime(0L)

            return
        }

        val position =
            currentPlayer.currentPosition
                .coerceIn(
                    0L,
                    duration
                )

        if (!progressUserSeeking) {

            progressSeekBar.progress =
                (
                    position.toDouble() /
                        duration.toDouble() *
                        1000.0
                )
                    .toInt()
                    .coerceIn(
                        0,
                        1000
                    )
        }

        currentTimeText.text =
            formatTime(position)

        remainingTimeText.text =
            "-${
                formatTime(
                    (
                        duration -
                            position
                    ).coerceAtLeast(0L)
                )
            }"

        totalTimeText.text =
            formatTime(duration)
    }

    /**
     * اعمال مجدد حالت FIT/FILL/ZOOM.
     *
     * چون PlayerActivity با configChanges کار می‌کند،
     * هنگام چرخش Activity دوباره ساخته نمی‌شود.
     * بنابراین باید resizeMode را صریحاً بعد از Rotation
     * دوباره اعمال کنیم.
     */
    internal fun applyAspectMode() {

        val mode =
            when (aspectIndex) {

                1 ->
                    androidx.media3.ui.AspectRatioFrameLayout
                        .RESIZE_MODE_FILL

                2 ->
                    androidx.media3.ui.AspectRatioFrameLayout
                        .RESIZE_MODE_ZOOM

                else ->
                    androidx.media3.ui.AspectRatioFrameLayout
                        .RESIZE_MODE_FIT
            }

        playerView.resizeMode = mode
        playerView.requestLayout()
        playerView.invalidate()
    }

    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {

        super.onConfigurationChanged(
            newConfig
        )

        window.decorView.post {
            applyAspectMode()

            if (!isLocked) {
                showPlayerControlsTemporarily()
            } else {
                lockedOverlay.visibility =
                    View.VISIBLE

                lockedOverlay.bringToFront()
            }
        }
    }

    internal fun loadSavedDisplaySettings() {

        val savedVolume =
            getSharedPreferences(
                DISPLAY_PREFS,
                Context.MODE_PRIVATE
            )
                .getInt(
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
            )
                .getFloat(
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

    internal fun saveDisplaySettings() {

        val currentBrightness =
            window.attributes
                .screenBrightness
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

    internal fun setupCastButton() {

        try {

            playerView.setMediaRouteButtonViewProvider(
                androidx.media3.cast.MediaRouteButtonViewProvider()
            )

        } catch (_: Exception) {

            showTemporaryMessage(
                p(
                    "Cast در این دستگاه در دسترس نیست.",
                    "Cast is not available on this device."
                )
            )
        }
    }

    internal fun setupLockedOverlay() {

        lockedOverlay.layoutParams =
            (
                lockedOverlay.layoutParams
                    as? ViewGroup.MarginLayoutParams
                )?.apply {

                    width =
                        dpToPx(52)

                    height =
                        dpToPx(52)

                    topMargin =
                        dpToPx(64)

                    marginEnd =
                        dpToPx(12)

                }
                ?: ViewGroup.MarginLayoutParams(
                    dpToPx(52),
                    dpToPx(52)
                ).apply {

                    topMargin =
                        dpToPx(64)

                    marginEnd =
                        dpToPx(12)
                }

        lockedOverlay.layoutParams.let {
            params ->

            if (
                params
                    is FrameLayout.LayoutParams
            ) {

                params.gravity =
                    Gravity.TOP or
                        Gravity.END

                lockedOverlay.layoutParams =
                    params
            }
        }

        lockedOverlay.elevation =
            dpToPx(20).toFloat()

        lockedOverlay.bringToFront()

        lockedOverlay.setOnClickListener {

            if (!isLocked) {
                return@setOnClickListener
            }

            isLocked = false

            lockedOverlay.visibility =
                View.GONE

            lockButton.text =
                p(
                    "قفل",
                    "Lock"
                )

            showPlayerControlsTemporarily()
        }
    }

    internal fun dpToPx(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        handleDeleteResult(
            requestCode,
            resultCode
        )
    }

    override fun onUserLeaveHint() {

        super.onUserLeaveHint()

        enterPictureInPictureModeIfPossible()
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

            } else {

                lockedOverlay.visibility =
                    View.VISIBLE

                lockedOverlay.bringToFront()
            }
        }
    }

    /**
     * بعض تبلیغات Activity را Pause نمی‌کنند
     * ولی Focus پنجره را می‌گیرند.
     *
     * فقط در صورتی که قبل از از دست رفتن Focus
     * ویدئو در حال پخش بوده باشد، بعد از برگشت Focus
     * دوباره آن را اجرا می‌کنیم.
     */
    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (isExitingPlayer) {
            return
        }

        if (
            !hasFocus &&
            !isInPictureInPictureMode &&
            !enteringPictureInPicture
        ) {

            val currentPlayer =
                player

            if (
                currentPlayer != null &&
                currentPlayer.isPlaying
            ) {

                wasPlayingBeforeWindowFocusLoss =
                    true

                currentPlayer.pause()

                updatePauseButton()
                updateCenterPlayButton()
            }

            return
        }

        if (
            hasFocus &&
            wasPlayingBeforeWindowFocusLoss &&
            !isInPictureInPictureMode &&
            !enteringPictureInPicture
        ) {

            val currentPlayer =
                player

            if (currentPlayer != null) {
                currentPlayer.play()
            }

            wasPlayingBeforeWindowFocusLoss =
                false

            updatePauseButton()
            updateCenterPlayButton()
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
            handlePlayerPause()
        }

        super.onPause()
    }

    override fun onResume() {

        super.onResume()

        handlePlayerResume()
    }

    override fun onStop() {

        if (!isExitingPlayer) {
            handlePlayerStop()
        }

        super.onStop()
    }

    override fun onDestroy() {

        handlePlayerDestroy()

        super.onDestroy()
    }

    companion object {

        const val EXTRA_VIDEO_URI =
            "com.vidora.player.EXTRA_VIDEO_URI"

        const val EXTRA_VIDEO_NAME =
            "com.vidora.player.EXTRA_VIDEO_NAME"

        internal const val DELETE_REQUEST_CODE =
            5001

        internal const val DISPLAY_PREFS =
            "vidora_display_settings"

        internal const val KEY_VOLUME =
            "volume"

        internal const val KEY_BRIGHTNESS =
            "brightness"

        internal const val CONTROL_HIDE_DELAY =
            4000L

        internal const val PROGRESS_UPDATE_INTERVAL =
            500L
    }
}
