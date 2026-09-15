package com.vidora.player

import android.content.Context

object SubtitleSettings {

    private const val PREFS =
        "vidora_subtitle_settings"

    private const val KEY_SIZE =
        "size"

    private const val KEY_BACKGROUND =
        "background"

    private const val KEY_BOLD =
        "bold"

    private const val KEY_POSITION =
        "position"

    private const val KEY_DELAY =
        "delay"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun getSize(
        context: Context
    ): Float {
        return prefs(context)
            .getFloat(
                KEY_SIZE,
                0.0533f
            )
            .coerceIn(
                0.025f,
                0.12f
            )
    }

    fun setSize(
        context: Context,
        value: Float
    ) {
        prefs(context)
            .edit()
            .putFloat(
                KEY_SIZE,
                value.coerceIn(
                    0.025f,
                    0.12f
                )
            )
            .apply()
    }

    fun useBackground(
        context: Context
    ): Boolean {
        return prefs(context)
            .getBoolean(
                KEY_BACKGROUND,
                true
            )
    }

    fun setUseBackground(
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

    fun bold(
        context: Context
    ): Boolean {
        return prefs(context)
            .getBoolean(
                KEY_BOLD,
                false
            )
    }

    fun setBold(
        context: Context,
        value: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                KEY_BOLD,
                value
            )
            .apply()
    }

    fun getPosition(
        context: Context
    ): Int {
        return prefs(context)
            .getInt(
                KEY_POSITION,
                90
            )
            .coerceIn(
                0,
                100
            )
    }

    fun setPosition(
        context: Context,
        value: Int
    ) {
        prefs(context)
            .edit()
            .putInt(
                KEY_POSITION,
                value.coerceIn(
                    0,
                    100
                )
            )
            .apply()
    }

    fun getDelay(
        context: Context
    ): Long {
        return prefs(context)
            .getLong(
                KEY_DELAY,
                0L
            )
            .coerceIn(
                -10_000L,
                10_000L
            )
    }

    fun setDelay(
        context: Context,
        value: Long
    ) {
        prefs(context)
            .edit()
            .putLong(
                KEY_DELAY,
                value.coerceIn(
                    -10_000L,
                    10_000L
                )
            )
            .apply()
    }

    fun resetDelay(
        context: Context
    ) {
        setDelay(
            context,
            0L
        )
    }
}
