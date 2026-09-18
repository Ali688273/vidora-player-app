package com.vidora.player

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object VidoraLocaleManager {

    fun apply(
        context: Context
    ): Context {

        val language =
            VidoraLanguageManager.getLanguage(
                context
            )

        val locale =
            Locale(language)

        Locale.setDefault(locale)

        val configuration =
            Configuration(
                context.resources.configuration
            )

        configuration.setLocale(locale)

        return context.createConfigurationContext(
            configuration
        )
    }

    fun applyToConfiguration(
        context: Context,
        configuration: Configuration
    ) {

        val language =
            VidoraLanguageManager.getLanguage(
                context
            )

        val locale =
            Locale(language)

        Locale.setDefault(locale)

        configuration.setLocale(locale)
    }
}
