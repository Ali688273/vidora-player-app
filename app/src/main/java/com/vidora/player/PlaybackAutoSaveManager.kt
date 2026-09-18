package com.vidora.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player

/**
 * ذخیره خودکار موقعیت پخش ویدئو
 *
 * هر چند ثانیه موقعیت فعلی ویدئو را در
 * PlaybackHistoryManager ذخیره می‌کند.
 *
 * این کلاس هیچ تغییری در منطق پخش، تبلیغات،
 * امضا یا PlaybackService ایجاد نمی‌کند.
 */
class PlaybackAutoSaveManager(
    private val context: Context,
    private val playerProvider: () -> Player?,
    private val uriProvider: () -> Uri?,
    private val externalVideoProvider: () -> Boolean
) {

    companion object {

        private const val SAVE_INTERVAL_MS = 5000L
    }

    private val handler =
        Handler(Looper.getMainLooper())

    private var started = false

    private val saveRunnable =
        object : Runnable {

            override fun run() {

                saveNow()

                if (started) {

                    handler.postDelayed(
                        this,
                        SAVE_INTERVAL_MS
                    )
                }
            }
        }

    fun start() {

        if (started) {
            return
        }

        started = true

        handler.removeCallbacks(
            saveRunnable
        )

        handler.postDelayed(
            saveRunnable,
            SAVE_INTERVAL_MS
        )
    }

    fun saveNow() {

        if (externalVideoProvider()) {
            return
        }

        val uri =
            uriProvider()
                ?: return

        val currentPlayer =
            playerProvider()
                ?: return

        val position =
            currentPlayer.currentPosition

        val duration =
            currentPlayer.duration

        if (position <= 0L) {
            return
        }

        PlaybackHistoryManager.save(
            context,
            uri,
            position,
            duration
        )
    }

    fun stop() {

        if (!started) {
            return
        }

        started = false

        handler.removeCallbacks(
            saveRunnable
        )

        saveNow()
    }

    fun release() {

        started = false

        handler.removeCallbacks(
            saveRunnable
        )

        saveNow()
    }
}
