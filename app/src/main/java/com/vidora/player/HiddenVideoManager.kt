package com.vidora.player

import android.content.Context
import android.net.Uri

object HiddenVideoManager {

    private const val PREFS = "vidora_hidden_videos"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(uri: Uri): String =
        uri.toString()

    fun isHidden(
        context: Context,
        uri: Uri
    ): Boolean {
        return preferences(context)
            .getBoolean(key(uri), false)
    }

    fun hide(
        context: Context,
        uri: Uri
    ) {
        preferences(context)
            .edit()
            .putBoolean(key(uri), true)
            .apply()
    }

    fun unhide(
        context: Context,
        uri: Uri
    ) {
        preferences(context)
            .edit()
            .remove(key(uri))
            .apply()
    }

    fun toggle(
        context: Context,
        uri: Uri
    ): Boolean {

        val hidden = isHidden(
            context,
            uri
        )

        if (hidden) {
            unhide(context, uri)
        } else {
            hide(context, uri)
        }

        return !hidden
    }
}
