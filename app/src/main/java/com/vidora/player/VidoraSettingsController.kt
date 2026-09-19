package com.vidora.player

import android.content.Context

object VidoraSettingsController {

    fun getLanguage(
        context: Context
    ): String {

        return VidoraLanguageManager.getLanguage(
            context
        )
    }

    fun getEffectiveLanguage(
        context: Context
    ): String {

        return VidoraLanguageManager.getLanguage(
            context
        )
    }

    fun getSelectedLanguage(
        context: Context
    ): String {

        return VidoraLanguageManager.getSelectedLanguage(
            context
        )
    }

    fun setLanguage(
        context: Context,
        language: String
    ) {

        VidoraLanguageManager.setLanguage(
            context,
            language
        )
    }

    fun useAutomaticLanguage(
        context: Context
    ) {

        VidoraLanguageManager.useAutomaticLanguage(
            context
        )
    }

    fun isPersian(
        context: Context
    ): Boolean {

        return VidoraLanguageManager.isPersian(
            context
        )
    }

    fun isEnglish(
        context: Context
    ): Boolean {

        return VidoraLanguageManager.isEnglish(
            context
        )
    }

    fun isAutomatic(
        context: Context
    ): Boolean {

        return VidoraLanguageManager.isAutomatic(
            context
        )
    }

    fun languageName(
        context: Context
    ): String {

        return VidoraLanguageManager.languageName(
            context
        )
    }

    fun isDarkMode(
        context: Context
    ): Boolean {

        return VidoraSettings.isDarkMode(
            context
        )
    }

    fun setDarkMode(
        context: Context,
        value: Boolean
    ) {

        VidoraSettings.setDarkMode(
            context,
            value
        )
    }

    fun showHidden(
        context: Context
    ): Boolean {

        return VidoraSettings.showHidden(
            context
        )
    }

    fun setShowHidden(
        context: Context,
        value: Boolean
    ) {

        VidoraSettings.setShowHidden(
            context,
            value
        )
    }

    fun autoResume(
        context: Context
    ): Boolean {

        return VidoraSettings.autoResume(
            context
        )
    }

    fun setAutoResume(
        context: Context,
        value: Boolean
    ) {

        VidoraSettings.setAutoResume(
            context,
            value
        )
    }
}
