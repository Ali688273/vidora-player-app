package com.vidora.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player

/**
 * مدیریت مسیر صدای Vidora.
 *
 * ولوم واقعی خروجی توسط STREAM_MUSIC خود Android کنترل می‌شود.
 * player.volume فقط روی 1.0 نگه داشته می‌شود تا دو ولوم روی هم ضرب نشوند.
 *
 * کلیدهای فیزیکی گوشی و ژست ولوم سمت راست Vidora بنابراین یک ولوم واقعی
 * را کنترل می‌کنند.
 *
 * بعد از جدا شدن هندزفری، برای جلوگیری از پخش ناخواسته روی اسپیکر،
 * فقط player موقتاً mute می‌شود و با تغییر دستی ولوم دوباره فعال می‌شود.
 */
internal object VidoraAudioRouteManager {

    private var registered = false
    private var lastRoute = ""
    private var mutedAfterHeadphoneRemoval = false
    private var player: Player? = null
    private var audioManager: AudioManager? = null

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val deviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(
                addedDevices: Array<out AudioDeviceInfo>
            ) {
                scheduleRouteCheck()
            }

            override fun onAudioDevicesRemoved(
                removedDevices: Array<out AudioDeviceInfo>
            ) {
                scheduleRouteCheck()
            }
        }

    fun register(
        context: PlayerActivity
    ) {
        player = context.player

        audioManager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as? AudioManager

        val manager =
            audioManager ?: return

        if (!registered) {
            registered = true
            lastRoute = detectRoute(manager)
            mutedAfterHeadphoneRemoval = false

            manager.registerAudioDeviceCallback(
                deviceCallback,
                null
            )
        }

        applyCurrentRouteVolume(context)
    }

    fun unregister(
        context: PlayerActivity
    ) {
        if (registered) {
            audioManager?.unregisterAudioDeviceCallback(
                deviceCallback
            )
        }

        mainHandler.removeCallbacksAndMessages(null)

        registered = false
        lastRoute = ""
        mutedAfterHeadphoneRemoval = false
        player = null
        audioManager = null
    }

    /**
     * gain داخلی همیشه 100% است.
     * ولوم واقعی فقط از STREAM_MUSIC می‌آید.
     */
    fun applyCurrentRouteVolume(
        context: PlayerActivity
    ) {
        player = context.player

        audioManager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as? AudioManager

        val currentPlayer =
            player ?: return

        val manager =
            audioManager ?: return

        lastRoute = detectRoute(manager)
        mutedAfterHeadphoneRemoval = false

        currentPlayer.volume = 1f
    }

    /**
     * اگر Media volume سیستم صفر باشد، هنگام شروع پخش فقط یک پله بالا می‌رود.
     */
    fun ensureSystemMediaVolumeAudible(
        context: PlayerActivity
    ) {
        val manager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as? AudioManager
                ?: return

        if (manager.isVolumeFixed()) {
            return
        }

        if (
            manager.getStreamVolume(
                AudioManager.STREAM_MUSIC
            ) <= 0
        ) {
            try {
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    0
                )
            } catch (_: Exception) {
            }
        }
    }

    /**
     * ژست سمت راست = ولوم واقعی Media گوشی.
     */
    fun onUserVolumeChanged(
        volume: Float
    ) {
        val currentPlayer =
            player ?: return

        val manager =
            audioManager ?: return

        if (manager.isVolumeFixed()) {
            currentPlayer.volume = 1f
            mutedAfterHeadphoneRemoval = false
            return
        }

        val safeVolume =
            volume.coerceIn(0f, 1f)

        val maxVolume =
            manager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        if (maxVolume > 0) {
            val targetIndex =
                (safeVolume * maxVolume)
                    .toInt()
                    .coerceIn(
                        0,
                        maxVolume
                    )

            try {
                manager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    targetIndex,
                    0
                )
            } catch (_: Exception) {
            }
        }

        currentPlayer.volume = 1f
        mutedAfterHeadphoneRemoval = false
    }

    /**
     * درصد واقعی Media volume سیستم.
     */
    fun getSystemMediaVolumePercent(): Int {
        val manager =
            audioManager ?: return 0

        val maxVolume =
            manager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        if (maxVolume <= 0) {
            return 0
        }

        val currentVolume =
            manager.getStreamVolume(
                AudioManager.STREAM_MUSIC
            )

        return (
            currentVolume * 100f / maxVolume
        )
            .toInt()
            .coerceIn(0, 100)
    }

    private fun scheduleRouteCheck() {
        mainHandler.removeCallbacks(
            routeCheckRunnable
        )

        mainHandler.postDelayed(
            routeCheckRunnable,
            250L
        )
    }

    private val routeCheckRunnable =
        Runnable {
            handleRouteChanged()
        }

    private fun handleRouteChanged() {
        val currentPlayer =
            player ?: return

        val manager =
            audioManager ?: return

        val newRoute =
            detectRoute(manager)

        if (newRoute == lastRoute) {
            return
        }

        val oldRoute =
            lastRoute

        val oldWasHeadphone =
            isHeadphoneRoute(oldRoute)

        val newIsSpeaker =
            newRoute == KEY_SPEAKER

        lastRoute = newRoute

        if (
            oldWasHeadphone &&
            newIsSpeaker
        ) {
            mutedAfterHeadphoneRemoval = true
            currentPlayer.volume = 0f
            return
        }

        mutedAfterHeadphoneRemoval = false
        currentPlayer.volume = 1f
    }

    private fun isHeadphoneRoute(
        route: String
    ): Boolean {
        return route == KEY_WIRED ||
            route == KEY_BLUETOOTH ||
            route == KEY_USB
    }

    private fun detectRoute(
        manager: AudioManager
    ): String {
        val outputs =
            manager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )

        var hasBluetooth = false
        var hasWired = false
        var hasUsb = false

        for (device in outputs) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                    hasBluetooth = true

                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                    hasWired = true

                AudioDeviceInfo.TYPE_USB_HEADSET ->
                    hasUsb = true
            }
        }

        return when {
            hasBluetooth -> KEY_BLUETOOTH
            hasWired -> KEY_WIRED
            hasUsb -> KEY_USB
            else -> KEY_SPEAKER
        }
    }

    private const val KEY_SPEAKER = "speaker"
    private const val KEY_WIRED = "wired"
    private const val KEY_BLUETOOTH = "bluetooth"
    private const val KEY_USB = "usb"
}
