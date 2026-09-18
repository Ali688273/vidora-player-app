package com.vidora.player

import android.content.Context
import android.os.Build
import java.util.Locale

object VidoraLanguageManager {

    private const val PREFS =
        "vidora_settings"

    private const val KEY_LANGUAGE =
        "language"

    private const val LANGUAGE_AUTO =
        "auto"

    private const val LANGUAGE_FA =
        "fa"

    private const val LANGUAGE_EN =
        "en"

    /**
     * زبان انتخاب‌شده توسط کاربر:
     *
     * auto = تشخیص خودکار از زبان گوشی
     * fa   = فارسی
     * en   = انگلیسی
     */
    fun getSelectedLanguage(
        context: Context
    ): String {

        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_LANGUAGE,
                LANGUAGE_AUTO
            )
            ?: LANGUAGE_AUTO
    }

    fun setLanguage(
        context: Context,
        language: String
    ) {

        val value =
            when (language) {

                LANGUAGE_FA ->
                    LANGUAGE_FA

                LANGUAGE_EN ->
                    LANGUAGE_EN

                else ->
                    LANGUAGE_AUTO
            }

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LANGUAGE,
                value
            )
            .apply()
    }

    fun useAutomaticLanguage(
        context: Context
    ) {

        setLanguage(
            context,
            LANGUAGE_AUTO
        )
    }

    /**
     * زبان نهایی مورد استفاده برنامه.
     */
    fun getLanguage(
        context: Context
    ): String {

        return when (
            getSelectedLanguage(context)
        ) {

            LANGUAGE_FA ->
                LANGUAGE_FA

            LANGUAGE_EN ->
                LANGUAGE_EN

            else ->
                detectDeviceLanguage()
        }
    }

    /**
     * تشخیص زبان گوشی.
     *
     * اگر زبان سیستم فارسی باشد:
     * fa
     *
     * در غیر این صورت:
     * en
     */
    private fun detectDeviceLanguage(): String {

        val locale =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.N
            ) {

                Locale.getDefault()

            } else {

                @Suppress("DEPRECATION")
                Locale.getDefault()
            }

        return if (
            locale.language.equals(
                LANGUAGE_FA,
                ignoreCase = true
            )
        ) {

            LANGUAGE_FA

        } else {

            LANGUAGE_EN
        }
    }

    fun isPersian(
        context: Context
    ): Boolean {

        return getLanguage(context) ==
            LANGUAGE_FA
    }

    fun isEnglish(
        context: Context
    ): Boolean {

        return getLanguage(context) ==
            LANGUAGE_EN
    }

    fun isAutomatic(
        context: Context
    ): Boolean {

        return getSelectedLanguage(context) ==
            LANGUAGE_AUTO
    }

    fun languageName(
        context: Context
    ): String {

        return when (
            getSelectedLanguage(context)
        ) {

            LANGUAGE_FA ->
                "فارسی"

            LANGUAGE_EN ->
                "English"

            else ->
                if (isPersian(context)) {
                    "خودکار • فارسی"
                } else {
                    "Automatic • English"
                }
        }
    }

    const val AUTO =
        LANGUAGE_AUTO

    const val FA =
        LANGUAGE_FA

    const val EN =
        LANGUAGE_EN
    }
