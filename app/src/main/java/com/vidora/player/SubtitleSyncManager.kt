package com.vidora.player

import android.content.Context
import android.net.Uri

object SubtitleSyncManager {

    private const val PREF =
        "vidora_subtitle_sync"

    private fun key(
        uri: Uri
    ): String {
        return "offset_${uri}"
    }

    fun getOffset(
        context: Context,
        uri: Uri
    ): Long {

        return context
            .getSharedPreferences(
                PREF,
                Context.MODE_PRIVATE
            )
            .getLong(
                key(uri),
                0L
            )
    }

    fun setOffset(
        context: Context,
        uri: Uri,
        offsetMs: Long
    ) {

        context
            .getSharedPreferences(
                PREF,
                Context.MODE_PRIVATE
            )
            .edit()
            .putLong(
                key(uri),
                offsetMs.coerceIn(
                    -10_000L,
                    10_000L
                )
            )
            .apply()
    }

    fun reset(
        context: Context,
        uri: Uri
    ) {

        context
            .getSharedPreferences(
                PREF,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                key(uri)
            )
            .apply()
    }
}
