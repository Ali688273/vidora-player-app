package com.vidora.player

import android.content.Context

object VidoraSettingsController {

    fun setLanguageAuto(
        context: Context
    ) {
        VidoraLanguageManager.setLanguage(
            context,
            VidoraLanguageManager.AUTO
        )
    }

    fun setLanguagePersian(
        context: Context
    ) {
        VidoraLanguageManager.setLanguage(
            context,
            VidoraLanguageManager.FA
        )
    }

    fun setLanguageEnglish(
        context: Context
    ) {
        VidoraLanguageManager.setLanguage(
            context,
            VidoraLanguageManager.EN
        )
    }

    fun selectedLanguage(
        context: Context
    ): String {
        return VidoraLanguageManager
            .getSelectedLanguage(context)
    }

    fun effectiveLanguage(
        context: Context
    ): String {
        return VidoraLanguageManager
            .getEffectiveLanguage(context)
    }
}
