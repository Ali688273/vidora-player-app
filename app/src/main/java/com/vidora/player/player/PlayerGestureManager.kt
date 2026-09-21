package com.vidora.player

import android.view.MotionEvent
import android.view.View
import androidx.media3.common.C
import kotlin.math.abs

internal fun PlayerActivity.setupPlayerGestures() {

    var longPressSpeedActive = false
    var longPressOriginalSpeed = 1.0f
    var longPressRunnable: Runnable? = null

    playerView.setOnTouchListener { _, event ->

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                downX = event.x
                downY = event.y

                startPosition =
                    player?.currentPosition
                        ?: 0L

                startVolume =
                    ((player?.volume ?: 0.30f) * 100f)
                        .toInt()
                        .coerceIn(0, 100)

                startBrightness =
                    window.attributes.screenBrightness
                        .takeIf { it >= 0f }
                        ?: 0.5f

                gestureMode =
                    PlayerActivity.GestureMode.NONE

                longPressSpeedActive = false
                longPressOriginalSpeed =
                    player?.playbackParameters?.speed
                        ?: 1.0f

                longPressRunnable?.let {
                    controlsHandler.removeCallbacks(it)
                }

                val runnable = Runnable {
                    if (
                        !isLocked &&
                        gestureMode == PlayerActivity.GestureMode.NONE
                    ) {
                        longPressSpeedActive = true
                        longPressOriginalSpeed =
                            player?.playbackParameters?.speed
                                ?: 1.0f

                        player?.playbackParameters =
                            androidx.media3.common.PlaybackParameters(2.0f)

                        showSpeedIndicator(2.0f)
                    }
                }

                longPressRunnable = runnable
                controlsHandler.postDelayed(runnable, 450L)

                if (isLocked) {
                    return@setOnTouchListener true
                }

                true
            }

            MotionEvent.ACTION_MOVE -> {

                if (isLocked) {
                    return@setOnTouchListener true
                }

                val deltaX = event.x - downX
                val deltaY = event.y - downY

                if (longPressSpeedActive) {
                    val steps =
                        kotlin.math.round(deltaX / 50f)
                            .toInt()

                    val speed =
                        (2.0f + steps * 0.1f)
                            .coerceIn(0.5f, 4.0f)

                    player?.playbackParameters =
                        androidx.media3.common.PlaybackParameters(speed)

                    showSpeedIndicator(speed)

                    return@setOnTouchListener true
                }

                if (
                    gestureMode ==
                    PlayerActivity.GestureMode.NONE
                ) {

                    if (
                        abs(deltaX) > 30 &&
                        abs(deltaX) > abs(deltaY)
                    ) {
                        longPressRunnable?.let {
                            controlsHandler.removeCallbacks(it)
                        }

                        gestureMode =
                            PlayerActivity.GestureMode.SEEK

                    } else if (
                        abs(deltaY) > 30 &&
                        abs(deltaY) > abs(deltaX)
                    ) {
                        longPressRunnable?.let {
                            controlsHandler.removeCallbacks(it)
                        }

                        gestureMode =
                            if (
                                downX < playerView.width / 2f
                            ) {
                                PlayerActivity.GestureMode.BRIGHTNESS
                            } else {
                                PlayerActivity.GestureMode.VOLUME
                            }
                    }
                }

                when (gestureMode) {

                    PlayerActivity.GestureMode.SEEK ->
                        handleSeek(deltaX)

                    PlayerActivity.GestureMode.VOLUME ->
                        handleVolume(deltaY)

                    PlayerActivity.GestureMode.BRIGHTNESS ->
                        handleBrightness(deltaY)

                    PlayerActivity.GestureMode.NONE ->
                        Unit
                }

                true
            }

            MotionEvent.ACTION_UP -> {

                longPressRunnable?.let {
                    controlsHandler.removeCallbacks(it)
                }

                if (longPressSpeedActive) {
                    player?.playbackParameters =
                        androidx.media3.common.PlaybackParameters(
                            longPressOriginalSpeed
                        )

                    longPressSpeedActive = false
                    hideSpeedIndicator()
                    gestureMode =
                        PlayerActivity.GestureMode.NONE

                    return@setOnTouchListener true
                }

                if (isLocked) {
                    return@setOnTouchListener true
                }

                val deltaX = event.x - downX
                val deltaY = event.y - downY

                val movement =
                    abs(deltaX) + abs(deltaY)

                val now =
                    System.currentTimeMillis()

                if (
                    gestureMode ==
                    PlayerActivity.GestureMode.NONE &&
                    movement < 30
                ) {

                    val doubleTap =
                        now - lastTapTime < 350L &&
                            abs(event.x - lastTapX) < 80f &&
                            abs(event.y - lastTapY) < 80f

                    if (doubleTap) {
                        handleDoubleTap(event.x)
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
                    PlayerActivity.GestureMode.NONE

                hideGestureInfo()

                true
            }

            MotionEvent.ACTION_CANCEL -> {

                longPressRunnable?.let {
                    controlsHandler.removeCallbacks(it)
                }

                if (longPressSpeedActive) {
                    player?.playbackParameters =
                        androidx.media3.common.PlaybackParameters(
                            longPressOriginalSpeed
                        )
                    longPressSpeedActive = false
                    hideSpeedIndicator()
                }

                gestureMode =
                    PlayerActivity.GestureMode.NONE

                hideGestureInfo()

                true
            }

            else -> true
        }
    }
}


internal fun PlayerActivity.handleDoubleTap(
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

    currentPlayer.seekTo(
        newPosition
    )

    updateProgress()

    showTemporaryMessage(
        if (
            x <
            playerView.width / 2f
        ) {
            p(
                "⏪ ۱۰ ثانیه",
                "⏪ 10 seconds"
            )
        } else {
            p(
                "۱۰ ثانیه ⏩",
                "10 seconds ⏩"
            )
        }
    )
}

internal fun PlayerActivity.handleSeek(
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

    updateProgress()
}

internal fun PlayerActivity.handleVolume(
    deltaY: Float
) {

    val currentPlayer =
        player ?: return

    VidoraAudioRouteManager.ensureSystemMediaVolumeAudible(
        this
    )

    val height =
        playerView.height
            .coerceAtLeast(1)

    val volumeChange =
        (
            -deltaY /
                height *
                100f
            ).toInt()

    val newVolume =
        (
            startVolume +
                volumeChange
            ).coerceIn(
                0,
                100
            )

    val volume =
        newVolume / 100f

    VidoraAudioRouteManager.onUserVolumeChanged(
        volume
    )

    saveDisplaySettings()

    showGestureInfo(
        p(
            "صدا $newVolume٪",
            "Volume $newVolume%"
        )
    )
}

internal fun PlayerActivity.handleBrightness(
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
        p(
            "روشنایی ${(newBrightness * 100).toInt()}٪",
            "Brightness ${(newBrightness * 100).toInt()}%"
        )
    )
}

internal fun PlayerActivity.showGestureInfo(
    text: String
) {

    gestureInfo.text =
        text

    gestureInfo.visibility =
        View.VISIBLE
}

internal fun PlayerActivity.showTemporaryMessage(
    text: String
) {

    showGestureInfo(
        text
    )

    gestureInfo.postDelayed(
        {
            hideGestureInfo()
        },
        1200L
    )
}

internal fun PlayerActivity.hideGestureInfo() {

    gestureInfo.visibility =
        View.GONE
}


internal fun PlayerActivity.showSpeedIndicator(
    speed: Float
) {

    speedIndicator.text =
        "⏩ %.1f×".format(speed)

    speedIndicator.visibility =
        View.VISIBLE
}

internal fun PlayerActivity.hideSpeedIndicator() {

    speedIndicator.visibility =
        View.GONE
}
