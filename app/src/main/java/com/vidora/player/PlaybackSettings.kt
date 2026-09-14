package com.vidora.player

import android.content.Context

object PlaybackSettings {

    private const val PREFS =
        "vidora_playback_settings"

    private const val KEY_SPEED =
        "default_speed"

    private const val KEY_ASPECT =
        "default_aspect"

    private const val KEY_AUTO_PLAY =
        "auto_play_next"

    private const val KEY_RESUME =
        "resume_playback"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun getDefaultSpeed(
        context: Context
    ): Float {

        return prefs(context)
            .getFloat(
                KEY_SPEED,
                1.0f
            )
            .coerceIn(
                0.1f,
                5.0f
            )
    }

    fun setDefaultSpeed(
        context: Context,
        value: Float
    ) {

        prefs(context)
            .edit()
            .putFloat(
                KEY_SPEED,
                value.coerceIn(
                    0.1f,
                    5.0f
                )
            )
            .apply()
    }

    fun getDefaultAspect(
        context: Context
    ): Int {

        return prefs(context)
            .getInt(
                KEY_ASPECT,
                0
            )
    }

    fun setDefaultAspect(
        context: Context,
        value: Int
    ) {

        prefs(context)
            .edit()
            .putInt(
                KEY_ASPECT,
                value
            )
            .apply()
    }

    fun autoPlayNext(
        context: Context
    ): Boolean {

        return prefs(context)
            .getBoolean(
                KEY_AUTO_PLAY,
                true
            )
    }

    fun setAutoPlayNext(
        context: Context,
        value: Boolean
    ) {

        prefs(context)
            .edit()
            .putBoolean(
                KEY_AUTO_PLAY,
                value
            )
            .apply()
    }

    fun resumePlayback(
        context: Context
    ): Boolean {

        return prefs(context)
            .getBoolean(
                KEY_RESUME,
                true
            )
    }

    fun setResumePlayback(
        context: Context,
        value: Boolean
    ) {

        prefs(context)
            .edit()
            .putBoolean(
                KEY_RESUME,
                value
            )
            .apply()
    }
}
