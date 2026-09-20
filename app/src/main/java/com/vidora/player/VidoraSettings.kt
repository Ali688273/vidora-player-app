package com.vidora.player

import android.app.Activity
import android.content.Context

object VidoraSettings {

    private const val PREFS =
        "vidora_settings"

    private const val KEY_DARK_MODE =
        "dark_mode"

    private const val KEY_LANGUAGE =
        "language"

    private const val KEY_SHOW_HIDDEN =
        "show_hidden"

    private const val KEY_AUTO_RESUME =
        "auto_resume"

    private fun preferences(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun isDarkMode(
        context: Context
    ): Boolean {

        return preferences(context)
            .getBoolean(
                KEY_DARK_MODE,
                false
            )
    }

    fun setDarkMode(
        context: Context,
        value: Boolean
    ) {

        preferences(context)
            .edit()
            .putBoolean(
                KEY_DARK_MODE,
                value
            )
            .apply()

        VidoraThemeManager.apply(
            context,
            value
        )

        /*
         * SettingsActivity رابط را به صورت
         * programmatic می‌سازد.
         *
         * بنابراین بعد از تغییر حالت تاریک،
         * Activity باید دوباره ساخته شود تا
         * colors-night و Theme دوباره resolve شوند.
         */
        if (context is Activity) {

            context.window.decorView.post {
                if (
                    !context.isFinishing &&
                    !context.isDestroyed
                ) {
                    context.recreate()
                }
            }
        }
    }

    fun getLanguage(
        context: Context
    ): String {

        return preferences(context)
            .getString(
                KEY_LANGUAGE,
                "fa"
            )
            ?: "fa"
    }

    fun setLanguage(
        context: Context,
        language: String
    ) {

        preferences(context)
            .edit()
            .putString(
                KEY_LANGUAGE,
                language
            )
            .apply()
    }

    fun showHidden(
        context: Context
    ): Boolean {

        return preferences(context)
            .getBoolean(
                KEY_SHOW_HIDDEN,
                false
            )
    }

    fun setShowHidden(
        context: Context,
        value: Boolean
    ) {

        preferences(context)
            .edit()
            .putBoolean(
                KEY_SHOW_HIDDEN,
                value
            )
            .apply()
    }

    fun autoResume(
        context: Context
    ): Boolean {

        return preferences(context)
            .getBoolean(
                KEY_AUTO_RESUME,
                true
            )
    }

    fun setAutoResume(
        context: Context,
        value: Boolean
    ) {

        preferences(context)
            .edit()
            .putBoolean(
                KEY_AUTO_RESUME,
                value
            )
            .apply()
    }
}
