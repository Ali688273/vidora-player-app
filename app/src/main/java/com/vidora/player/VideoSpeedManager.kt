package com.vidora.player

import android.content.Context
import android.net.Uri

object VideoSpeedManager {

    private const val PREFS = "vidora_video_speed"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    private fun key(uri: Uri): String {
        return "speed_${uri.toString()}"
    }

    fun getSpeed(
        context: Context,
        uri: Uri
    ): Float {

        val stored =
            prefs(context).getFloat(
                key(uri),
                -1f
            )

        if (stored > 0f) {
            return stored.coerceIn(
                0.1f,
                5.0f
            )
        }

        return PlaybackSettings.getDefaultSpeed(
            context
        ).coerceIn(
            0.1f,
            5.0f
        )
    }

    fun setSpeed(
        context: Context,
        uri: Uri,
        speed: Float
    ) {

        val safeSpeed =
            speed.coerceIn(
                0.1f,
                5.0f
            )

        prefs(context)
            .edit()
            .putFloat(
                key(uri),
                safeSpeed
            )
            .apply()
    }

    fun resetToDefault(
        context: Context,
        uri: Uri
    ) {

        prefs(context)
            .edit()
            .remove(
                key(uri)
            )
            .apply()
    }

    fun clear(
        context: Context,
        uri: Uri
    ) {

        prefs(context)
            .edit()
            .remove(
                key(uri)
            )
            .apply()
    }
}
