package com.vidora.player

import android.content.Context

object VideoQualitySettings {

    private const val PREFS =
        "vidora_video_quality"

    private const val KEY_ENABLED =
        "enhancement_enabled"

    private const val KEY_SHARPNESS =
        "sharpness"

    private const val KEY_CONTRAST =
        "contrast"

    private const val KEY_BRIGHTNESS =
        "brightness"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun enhancementEnabled(
        context: Context
    ): Boolean {

        return prefs(context)
            .getBoolean(
                KEY_ENABLED,
                false
            )
    }

    fun setEnhancementEnabled(
        context: Context,
        enabled: Boolean
    ) {

        prefs(context)
            .edit()
            .putBoolean(
                KEY_ENABLED,
                enabled
            )
            .apply()
    }

    fun getSharpness(
        context: Context
    ): Float {

        return prefs(context)
            .getFloat(
                KEY_SHARPNESS,
                0f
            )
    }

    fun setSharpness(
        context: Context,
        value: Float
    ) {

        prefs(context)
            .edit()
            .putFloat(
                KEY_SHARPNESS,
                value.coerceIn(
                    -1f,
                    1f
                )
            )
            .apply()
    }

    fun getContrast(
        context: Context
    ): Float {

        return prefs(context)
            .getFloat(
                KEY_CONTRAST,
                1f
            )
    }

    fun setContrast(
        context: Context,
        value: Float
    ) {

        prefs(context)
            .edit()
            .putFloat(
                KEY_CONTRAST,
                value.coerceIn(
                    0.5f,
                    1.5f
                )
            )
            .apply()
    }

    fun getBrightness(
        context: Context
    ): Float {

        return prefs(context)
            .getFloat(
                KEY_BRIGHTNESS,
                0f
            )
    }

    fun setBrightness(
        context: Context,
        value: Float
    ) {

        prefs(context)
            .edit()
            .putFloat(
                KEY_BRIGHTNESS,
                value.coerceIn(
                    -1f,
                    1f
                )
            )
            .apply()
    }
}
