package com.vidora.player

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }
    }

    fun applyDirection(
        view: View,
        context: Context
    ) {

        if (
            VidoraLanguageManager.isPersian(
                context
            )
        ) {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.JELLY_BEAN_MR1
            ) {
                view.layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        } else {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.JELLY_BEAN_MR1
            ) {
                view.layoutDirection =
                    View.LAYOUT_DIRECTION_LTR
            }
        }
    }
}
