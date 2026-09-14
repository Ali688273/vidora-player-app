package com.vidora.player

import android.content.Context
import android.net.Uri

object SubtitleSyncManager {

    private const val PREFS =
        "vidora_subtitle_sync"

    private fun key(uri: Uri) =
        "offset_${uri}"

    fun getOffset(
        context: Context,
        uri: Uri
    ): Long {

        return context
            .getSharedPreferences(
                PREFS,
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
        offset: Long
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putLong(
                key(uri),
                offset
            )
            .apply()
    }

    fun increase(
        context: Context,
        uri: Uri,
        amount: Long
    ): Long {

        val value =
            getOffset(context, uri) +
                amount

        setOffset(
            context,
            uri,
            value
        )

        return value
    }

    fun reset(
        context: Context,
        uri: Uri
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(key(uri))
            .apply()
    }
}
