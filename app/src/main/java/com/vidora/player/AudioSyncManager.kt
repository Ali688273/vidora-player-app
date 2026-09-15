package com.vidora.player

import android.content.Context
import android.content.Intent
import android.net.Uri

object AudioSyncManager {

    private const val PREF =
        "vidora_audio_sync"

    private const val ACTION_CHANGED =
        "com.vidora.player.AUDIO_SYNC_CHANGED"

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

        val value =
            offsetMs.coerceIn(
                -10_000L,
                10_000L
            )

        context
            .getSharedPreferences(
                PREF,
                Context.MODE_PRIVATE
            )
            .edit()
            .putLong(
                key(uri),
                value
            )
            .apply()

        context.sendBroadcast(
            Intent(ACTION_CHANGED).apply {
                setPackage(
                    context.packageName
                )
                putExtra(
                    "uri",
                    uri.toString()
                )
                putExtra(
                    "offset",
                    value
                )
            }
        )
    }

    fun reset(
        context: Context,
        uri: Uri
    ) {
        setOffset(
            context,
            uri,
            0L
        )
    }

    fun actionChanged(): String {
        return ACTION_CHANGED
    }
}
