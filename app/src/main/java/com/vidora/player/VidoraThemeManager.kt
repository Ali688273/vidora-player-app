package com.vidora.player

import android.app.UiModeManager
import android.content.Context
import android.os.Build

object VidoraThemeManager {

    fun apply(
        context: Context,
        darkMode: Boolean
    ) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val uiModeManager =
            context.getSystemService(
                UiModeManager::class.java
            ) ?: return

        uiModeManager.setApplicationNightMode(
            if (darkMode) {
                UiModeManager.MODE_NIGHT_YES
            } else {
                UiModeManager.MODE_NIGHT_NO
            }
        )
    }
}
