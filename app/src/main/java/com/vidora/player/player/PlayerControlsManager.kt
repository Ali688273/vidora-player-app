package com.vidora.player

import android.view.View
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import java.util.Locale

internal val PlayerActivity.aspectModes: IntArray
    get() = intArrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )

internal val PlayerActivity.aspectNamesFa: Array<String>
    get() = arrayOf(
        "تطبیق",
        "کامل",
        "بزرگ‌نمایی"
    )

internal val PlayerActivity.aspectNamesEn: Array<String>
    get() = arrayOf(
        "FIT",
        "FILL",
        "ZOOM"
    )

internal fun PlayerActivity.setupPlayerButtons() {

    backButton.setOnClickListener {

        if (!isLocked) {
            exitPlayer()
        }
    }

    pauseButton.setOnClickListener {

        if (!isLocked) {

            val currentPlayer =
                player ?: return@setOnClickListener

            if (currentPlayer.isPlaying) {
                currentPlayer.pause()
            } else {
                VidoraAudioRouteManager.ensureSystemMediaVolumeAudible(
                    this
                )
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
            openAudioFileManager()
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
            if (isPersian()) {
                aspectNamesFa[aspectIndex]
            } else {
                aspectNamesEn[aspectIndex]
            }

        showPlayerControlsTemporarily()
    }

    subtitleButton.setOnClickListener {

        if (!isLocked) {

            subtitlePickerLaunch()

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

private const val AUDIO_FILE_REQUEST_CODE = 6102

internal fun PlayerActivity.setupPlayerCenterButtons() {

    centerPreviousButton.setOnClickListener {

        if (!isLocked) {

            playPreviousVideo()

            showPlayerControlsTemporarily()
        }
    }

    centerPlayButton.setOnClickListener {

        if (!isLocked) {

            val currentPlayer =
                player ?: return@setOnClickListener

            if (currentPlayer.isPlaying) {
                currentPlayer.pause()
            } else {
                VidoraAudioRouteManager.ensureSystemMediaVolumeAudible(
                    this
                )
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

private fun PlayerActivity.subtitlePickerLaunch() {

    subtitlePickerInternalLaunch()
}

internal fun PlayerActivity.updateCenterPlayButton() {

    centerPlayButton.text =
        if (player?.isPlaying == true) {
            "⏸"
        } else {
            "▶"
        }
}

internal fun PlayerActivity.updatePauseButton() {

    pauseButton.text =
        if (player?.isPlaying == true) {
            "⏸"
        } else {
            "▶"
        }

    pauseButton.contentDescription =
        if (player?.isPlaying == true) {
            p(
                "توقف",
                "Pause"
            )
        } else {
            p(
                "پخش",
                "Play"
            )
        }
}

internal fun PlayerActivity.getCurrentVideoSpeed(): Float {

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

internal fun PlayerActivity.updateSpeedText() {

    speedButton.text =
        String.format(
            Locale.US,
            "%.1f×",
            currentSpeed
        )
}

internal fun PlayerActivity.changeSpeed(
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
            Locale.US,
            "%.1f",
            currentSpeed
        ).toFloat()

    VideoSpeedManager.setSpeed(
        this,
        uri,
        currentSpeed
    )

    player?.playbackParameters =
        androidx.media3.common.PlaybackParameters(
            currentSpeed
        )

    updateSpeedText()
    showPlayerControlsTemporarily()
}

internal fun PlayerActivity.resetSpeed() {

    val uri =
        getIncomingVideoUri()
            ?: return

    currentSpeed = 1.0f

    VideoSpeedManager.setSpeed(
        this,
        uri,
        currentSpeed
    )

    player?.playbackParameters =
        androidx.media3.common.PlaybackParameters(
            currentSpeed
        )

    updateSpeedText()
    showPlayerControlsTemporarily()
}

internal fun PlayerActivity.toggleRepeat() {

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

internal fun PlayerActivity.updateRepeatButton() {

    repeatButton.text =
        if (repeatEnabled) {
            "🔁"
        } else {
            "↪"
        }

    repeatButton.contentDescription =
        if (repeatEnabled) {
            p(
                "تکرار فعال است",
                "Repeat is enabled"
            )
        } else {
            p(
                "تکرار خاموش است",
                "Repeat is disabled"
            )
        }
}

internal fun PlayerActivity.toggleFavorite() {

    val uri =
        getIncomingVideoUri()
            ?: return

    if (isExternalVideo()) {

        showTemporaryMessage(
            p(
                "برای ویدئوی خارجی قابل استفاده نیست",
                "Not available for external videos."
            )
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
            p(
                "به علاقه‌مندی‌ها اضافه شد.",
                "Added to favorites."
            )
        } else {
            p(
                "از علاقه‌مندی‌ها حذف شد.",
                "Removed from favorites."
            )
        }
    )
}

internal fun PlayerActivity.updateFavoriteButton() {

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
            "★"
        } else {
            "☆"
        }

    favoriteButton.contentDescription =
        if (favorite) {
            p(
                "حذف از علاقه‌مندی‌ها",
                "Remove from favorites"
            )
        } else {
            p(
                "افزودن به علاقه‌مندی‌ها",
                "Add to favorites"
            )
        }
}

internal fun PlayerActivity.toggleLock() {

    if (isLocked) {

        isLocked = false

        lockedOverlay.visibility =
            View.GONE

        lockButton.text =
            p(
                "قفل",
                "Lock"
            )

        showPlayerControlsTemporarily()

        return
    }

    isLocked = true

    controlsHandler.removeCallbacks(
        hideControlsRunnable
    )

    lockedOverlay.visibility =
        View.VISIBLE

    lockButton.text =
        p(
            "بازکردن قفل",
            "Unlock"
        )

    setPlayerControlsVisibility(false)

    controlsVisible = false

    lockedOverlay.visibility =
        View.VISIBLE

    lockedOverlay.bringToFront()
}

internal fun PlayerActivity.togglePlayerControls() {

    if (isLocked) {
        return
    }

    if (controlsVisible) {
        hidePlayerControls()
    } else {
        showPlayerControlsTemporarily()
    }
}

internal fun PlayerActivity.showPlayerControlsTemporarily() {

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
        PlayerActivity.CONTROL_HIDE_DELAY
    )
}

internal fun PlayerActivity.hidePlayerControls() {

    controlsHandler.removeCallbacks(
        hideControlsRunnable
    )

    setPlayerControlsVisibility(false)

    controlsVisible = false

    if (isLocked) {

        lockedOverlay.visibility =
            View.VISIBLE

        lockedOverlay.bringToFront()
    }
}

internal fun PlayerActivity.setPlayerControlsVisibility(
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

    if (isLocked) {

        lockedOverlay.visibility =
            View.VISIBLE

        lockedOverlay.bringToFront()
    }
}

internal fun PlayerActivity.enterFullscreen() {

    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
}

internal fun PlayerActivity.openAudioFileManager() {

    try {
        val intent =
            android.content.Intent(
                android.content.Intent.ACTION_OPEN_DOCUMENT
            ).apply {
                addCategory(
                    android.content.Intent.CATEGORY_OPENABLE
                )
                type = "audio/*"
            }

        startActivityForResult(
            intent,
            AUDIO_FILE_REQUEST_CODE
        )

    } catch (_: Exception) {

        showTemporaryMessage(
            p(
                "مدیریت فایل در دسترس نیست.",
                "File manager is not available."
            )
        )
    }
}
