package com.vidora.player

import android.content.Context

object PrivateVaultManager {

    private const val PREFS =
        "vidora_private_vault"

    private const val KEY_ENABLED =
        "enabled"

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun isEnabled(
        context: Context
    ): Boolean {

        return prefs(context)
            .getBoolean(
                KEY_ENABLED,
                false
            )
    }

    fun setEnabled(
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

    fun disable(
        context: Context
    ) {

        setEnabled(
            context,
            false
        )

        SecureStorage
            .clearPassword(
                context
            )
    }
}
