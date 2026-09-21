package com.vidora.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player

/**
 * مدیریت مستقل صدای خروجی‌های مختلف Vidora.
 *
 * حجم هر مسیر جداگانه ذخیره می‌شود:
 * - speaker
 * - wired
 * - bluetooth
 * - usb
 *
 * نکته مهم:
 * حذف هندزفری نباید حجم ذخیره‌شده هندزفری را به بلندگو منتقل کند.
 * همچنین بعد از جدا شدن هندزفری، پخش خودکار از بلندگو انجام نمی‌شود.
 * در این حالت فقط صدای player موقتاً صفر می‌شود و با تغییر دستی صدا دوباره فعال می‌شود.
 */
internal object VidoraAudioRouteManager {

    private const val PREFS_NAME =
        "vidora_audio_routes"

    private const val KEY_SPEAKER =
        "speaker"

    private const val KEY_WIRED =
        "wired"

    private const val KEY_BLUETOOTH =
        "bluetooth"

    private const val KEY_USB =
        "usb"

    private const val DEFAULT_VOLUME = 0.30f

    private const val KEY_MIGRATION =
        "route_volume_migration_v7"

    private var registered =
        false

    private var lastRoute =
        ""

    private var mutedAfterHeadphoneRemoval =
        false

    private var player:
        Player? = null

    private var audioManager:
        AudioManager? = null

    private var applicationContext:
        Context? = null

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val deviceCallback =
        object : AudioDeviceCallback() {

            override fun onAudioDevicesAdded(
                addedDevices:
                    Array<out AudioDeviceInfo>
            ) {
                scheduleRouteCheck()
            }

            override fun onAudioDevicesRemoved(
                removedDevices:
                    Array<out AudioDeviceInfo>
            ) {
                scheduleRouteCheck()
            }
        }

    fun register(
        context: PlayerActivity
    ) {

        player =
            context.player

        applicationContext =
            context.applicationContext

        audioManager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as? AudioManager

        val manager =
            audioManager
                ?: return

        if (!registered) {

            migrateBrokenRouteVolumes(context)

            registered =
                true

            lastRoute =
                detectRoute(
                    manager
                )

            mutedAfterHeadphoneRemoval =
                false

            manager.registerAudioDeviceCallback(
                deviceCallback,
                null
            )
        }

        applyCurrentRouteVolume(
            context
        )
    }

    fun unregister(
        context: PlayerActivity
    ) {

        val currentPlayer =
            player

        val contextToSave =
            applicationContext

        if (
            currentPlayer != null &&
            contextToSave != null &&
            lastRoute.isNotBlank()
        ) {
            if (
                !(
                    lastRoute == KEY_SPEAKER &&
                    mutedAfterHeadphoneRemoval
                )
            ) {
                saveVolume(
                    contextToSave,
                    lastRoute,
                    currentPlayer.volume
                )
            }
        }

        if (registered) {

            audioManager
                ?.unregisterAudioDeviceCallback(
                    deviceCallback
                )
        }

        mainHandler.removeCallbacksAndMessages(null)

        registered =
            false

        lastRoute =
            ""

        mutedAfterHeadphoneRemoval =
            false

        player =
            null

        audioManager =
            null

        applicationContext =
            null
    }

    fun applyCurrentRouteVolume(
        context: PlayerActivity
    ) {

        player =
            context.player

        applicationContext =
            context.applicationContext

        audioManager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as? AudioManager

        val currentPlayer =
            player ?: return

        val manager =
            audioManager ?: return

        val route =
            detectRoute(
                manager
            )

        lastRoute =
            route

        mutedAfterHeadphoneRemoval =
            false

        val saved =
            getSavedVolume(
                context,
                route
            )

        val targetVolume =
            (saved ?: DEFAULT_VOLUME).coerceIn(
                0f,
                1f
            )

        applySystemMediaVolume(
            manager,
            targetVolume
        )

        currentPlayer.volume = 1f
    }

    /**
     * وقتی کاربر با ژست تغییر صدا را به‌صورت دستی انجام می‌دهد،
     * mute موقت ناشی از جدا شدن هندزفری باید برداشته شود.
     */
    fun onUserVolumeChanged(
        volume: Float
    ) {

        val context =
            applicationContext
                ?: return

        val currentPlayer =
            player
                ?: return

        val route =
            lastRoute

        if (route.isBlank()) {
            return
        }

        mutedAfterHeadphoneRemoval =
            false

        val safeVolume =
            volume.coerceIn(
                0f,
                1f
            )

        val manager =
            audioManager

        if (manager != null) {
            applySystemMediaVolume(
                manager,
                safeVolume
            )
        }

        currentPlayer.volume =
            if (safeVolume > 0f) 1f else 0f

        saveVolume(
            context,
            route,
            safeVolume
        )
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

        val context =
            applicationContext
                ?: return

        val currentPlayer =
            player
                ?: return

        val manager =
            audioManager
                ?: return

        val newRoute =
            detectRoute(
                manager
            )

        if (
            newRoute ==
            lastRoute
        ) {
            return
        }

        val oldRoute =
            lastRoute

        val oldWasHeadphone =
            isHeadphoneRoute(
                oldRoute
            )

        val newIsSpeaker =
            newRoute ==
                KEY_SPEAKER

        /*
         * ابتدا حجم مسیر قبلی را ذخیره می‌کنیم.
         * بنابراین volume هندزفری هیچ‌وقت جای volume بلندگو را نمی‌گیرد.
         */
        if (
            oldRoute.isNotBlank() &&
            !(
                oldRoute == KEY_SPEAKER &&
                mutedAfterHeadphoneRemoval
            )
        ) {
            saveVolume(
                context,
                oldRoute,
                currentPlayer.volume
            )
        }

        lastRoute =
            newRoute

        /*
         * هندزفری جدا شده و خروجی به بلندگو برگشته:
         * صدای بلندگو را از مقدار ذخیره‌شده‌اش بازیابی نمی‌کنیم.
         * فقط موقتاً player را mute می‌کنیم تا صدا خودکار از بلندگو پخش نشود.
         *
         * مقدار واقعی volume بلندگو در SharedPreferences دست‌نخورده می‌ماند.
         */
        if (
            oldWasHeadphone &&
            newIsSpeaker
        ) {
            mutedAfterHeadphoneRemoval =
                true

            currentPlayer.volume =
                0f

            return
        }

        /*
         * در سایر تغییر مسیرها، volume مخصوص همان مسیر را اعمال می‌کنیم.
         */
        mutedAfterHeadphoneRemoval =
            false

        val saved =
            getSavedVolume(
                context,
                newRoute
            )

        val targetVolume =
            (saved ?: DEFAULT_VOLUME).coerceIn(
                0f,
                1f
            )

        applySystemMediaVolume(
            manager,
            targetVolume
        )

        currentPlayer.volume = 1f
    }


    /**
     * صدای واقعی Media در اندروید با STREAM_MUSIC کنترل می‌شود.
     * player.volume فقط gain داخلی Media3 است.
     */
    private fun applySystemMediaVolume(
        manager: AudioManager,
        fraction: Float
    ) {
        val maxVolume =
            manager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        if (maxVolume <= 0) {
            return
        }

        val target =
            (fraction.coerceIn(0f, 1f) * maxVolume)
                .toInt()
                .coerceIn(0, maxVolume)

        manager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            target,
            0
        )
    }

    private fun isHeadphoneRoute(
        route: String
    ): Boolean {

        return route ==
            KEY_WIRED ||
            route ==
            KEY_BLUETOOTH ||
            route ==
            KEY_USB
    }

    private fun migrateBrokenRouteVolumes(
        context: Context
    ) {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        if (
            prefs.getBoolean(
                KEY_MIGRATION,
                false
            )
        ) {
            return
        }

        prefs.edit()
            .clear()
            .putBoolean(
                KEY_MIGRATION,
                true
            )
            .apply()
    }

    private fun detectRoute(
        manager: AudioManager
    ): String {

        val outputs =
            manager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )

        var hasBluetooth =
            false

        var hasWired =
            false

        var hasUsb =
            false

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

            hasBluetooth ->
                KEY_BLUETOOTH

            hasWired ->
                KEY_WIRED

            hasUsb ->
                KEY_USB

            else ->
                KEY_SPEAKER
        }
    }

    private fun getSavedVolume(
        context: Context,
        route: String
    ): Float? {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        if (
            !prefs.contains(route)
        ) {
            return null
        }

        val value =
            prefs.getFloat(
                route,
                DEFAULT_VOLUME
            )

        return if (
            value.isFinite() &&
            value in 0f..1f
        ) {
            value
        } else {
            null
        }
    }

    private fun saveVolume(
        context: Context,
        route: String,
        volume: Float
    ) {

        if (
            route.isBlank()
        ) {
            return
        }

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putFloat(
                route,
                volume.coerceIn(
                    0f,
                    1f
                )
            )
            .apply()
    }
}
