package com.vidora.player

import android.app.Activity
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object VidoraLocaleManager {

    fun apply(
        activity: Activity
    ) {

        val context =
            activity.applicationContext

        val language =
            VidoraLanguageManager
                .getEffectiveLanguage(context)

        val locale =
            Locale.forLanguageTag(language)

        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val localeManager =
                activity.getSystemService(
                    android.app.LocaleManager::class.java
                )

            localeManager.applicationLocales =
                LocaleList(
                    locale
                )

        } else {

            @Suppress("DEPRECATION")
            activity.resources.configuration.setLocale(
                locale
            )

            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(
                activity.resources.configuration,
                activity.resources.displayMetrics
            )
        }
    }
}
