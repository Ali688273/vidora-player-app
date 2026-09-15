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

    private const val KEY_BACKGROUND =
        "background_playback"

    private const val KEY_KEEP_SCREEN =
        "keep_screen_on"

    private const val KEY_GESTURES =
        "gesture_controls"

    private const val KEY_SUBTITLE_SIZE =
        "subtitle_size"

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

    fun backgroundPlayback(
        context: Context
    ): Boolean {
        return prefs(context)
            .getBoolean(
                KEY_BACKGROUND,
                true
            )
    }

    fun setBackgroundPlayback(
        context: Context,
        value: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                KEY_BACKGROUND,
                value
            )
            .apply()
    }

    fun keepScreenOn(
        context: Context
    ): Boolean {
        return prefs(context)
            .getBoolean(
                KEY_KEEP_SCREEN,
                true
            )
    }

    fun setKeepScreenOn(
        context: Context,
        value: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                KEY_KEEP_SCREEN,
                value
            )
            .apply()
    }

    fun gestureControls(
        context: Context
    ): Boolean {
        return prefs(context)
            .getBoolean(
                KEY_GESTURES,
                true
            )
    }

    fun setGestureControls(
        context: Context,
        value: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                KEY_GESTURES,
                value
            )
            .apply()
    }

    fun getSubtitleSize(
        context: Context
    ): Float {
        return prefs(context)
            .getFloat(
                KEY_SUBTITLE_SIZE,
                0.0533f
            )
            .coerceIn(
                0.025f,
                0.12f
            )
    }

    fun setSubtitleSize(
        context: Context,
        value: Float
    ) {
        prefs(context)
            .edit()
            .putFloat(
                KEY_SUBTITLE_SIZE,
                value.coerceIn(
                    0.025f,
                    0.12f
                )
            )
            .apply()
    }
}
