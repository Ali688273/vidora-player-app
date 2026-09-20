package com.vidora.player

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.graphics.Rect
import android.os.Build
import android.util.Rational

import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

internal fun PlayerActivity.connectToPlaybackService() {

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
                    p(
                        "خطا در اتصال پخش‌کننده",
                        "Player connection error."
                    )
                )
            }
        },
        ContextCompat.getMainExecutor(
            this
        )
    )
}

internal fun PlayerActivity.stopPlaybackCompletely() {

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

internal fun PlayerActivity.exitPlayer() {

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

internal fun PlayerActivity.enterPictureInPictureModeIfPossible() {

    if (
        Build.VERSION.SDK_INT <
        Build.VERSION_CODES.O
    ) {
        return
    }

    if (isInPictureInPictureMode) {
        return
    }

    if (isExitingPlayer) {
        return
    }

    enteringPictureInPicture = true

    val params =
        PictureInPictureParams.Builder()
            .setAspectRatio(
                Rational(16, 9)
            )
            .build()

    try {

        enterPictureInPictureMode(
            params
        )

    } catch (_: Exception) {

        enteringPictureInPicture = false
    }
}

internal fun PlayerActivity.handlePlayerPause() {

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

internal fun PlayerActivity.handlePlayerResume() {

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
    } else {

        lockedOverlay.visibility =
            android.view.View.VISIBLE

        lockedOverlay.bringToFront()
    }
}

internal fun PlayerActivity.handlePlayerStop() {

    playbackAutoSaveManager.saveNow()
    savePosition()
    saveDisplaySettings()
}

internal fun PlayerActivity.handlePlayerDestroy() {

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
}
