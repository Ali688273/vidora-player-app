package com.vidora.player

import android.content.Context
import android.net.Uri

object SubtitleFileManager {

    private const val PREFS =
        "vidora_subtitle_files"

    private const val KEY_PREFIX =
        "subtitle_"

    private fun key(
        videoUri: Uri
    ): String {
        return KEY_PREFIX + videoUri.toString()
    }

    fun setSubtitleUri(
        context: Context,
        videoUri: Uri,
        subtitleUri: Uri
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                key(videoUri),
                subtitleUri.toString()
            )
            .apply()
    }

    fun getSubtitleUri(
        context: Context,
        videoUri: Uri
    ): Uri? {

        val value =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    key(videoUri),
                    null
                )
                ?: return null

        return try {
            Uri.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    fun hasSubtitle(
        context: Context,
        videoUri: Uri
    ): Boolean {

        return getSubtitleUri(
            context,
            videoUri
        ) != null
    }

    fun removeSubtitle(
        context: Context,
        videoUri: Uri
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                key(videoUri)
            )
            .apply()
    }

    fun clearAll(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }
}
