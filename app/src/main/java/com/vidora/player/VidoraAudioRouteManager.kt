package com.vidora.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.Player

/**
 * مدیریت حجم صدای داخلی Vidora برای مسیرهای مختلف خروجی.
 *
 * صدای سیستم را تغییر نمی‌دهد.
 * فقط player.volume را برای هر مسیر خروجی جداگانه نگه می‌دارد.
 *
 * مسیرها:
 * - speaker
 * - wired
 * - bluetooth
 * - usb
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

    private const val KEY_MIGRATION =
        "route_volume_migration_v2"

    private var registered =
        false

    private var lastRoute =
        ""

    private var player:
        Player? = null

    private var audioManager:
        AudioManager? = null

    private var applicationContext:
        Context? = null

    private val deviceCallback =
        object : AudioDeviceCallback() {

            override fun onAudioDevicesAdded(
                addedDevices:
                    Array<out AudioDeviceInfo>
            ) {
                handleRouteChanged()
            }

            override fun onAudioDevicesRemoved(
                removedDevices:
                    Array<out AudioDeviceInfo>
            ) {
                handleRouteChanged()
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

            saveVolume(
                contextToSave,
                lastRoute,
                currentPlayer.volume
            )
        }

        if (registered) {

            audioManager
                ?.unregisterAudioDeviceCallback(
                    deviceCallback
                )
        }

        registered =
            false

        lastRoute =
            ""

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

        val saved =
            getSavedVolume(
                context,
                route
            )

        currentPlayer.volume =
            (saved ?: 1f).coerceIn(
                0f,
                1f
            )
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

        if (
            lastRoute.isNotBlank()
        ) {

            saveVolume(
                context,
                lastRoute,
                currentPlayer.volume
            )
        }

        lastRoute =
            newRoute

        val saved =
            getSavedVolume(
                context,
                newRoute
            )

        currentPlayer.volume =
            (saved ?: 1f).coerceIn(
                0f,
                1f
            )
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

        return prefs.getFloat(
            route,
            1f
        )
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
