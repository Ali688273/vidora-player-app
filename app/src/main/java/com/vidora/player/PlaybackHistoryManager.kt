package com.vidora.player

import android.content.Context
import android.net.Uri

object PlaybackHistoryManager {

    private const val PREFS_NAME = "vidora_playback_history"

    private const val KEY_PREFIX = "history_"

    private fun key(uri: Uri): String {
        return KEY_PREFIX + uri.toString()
    }

    fun save(
        context: Context,
        uri: Uri,
        position: Long,
        duration: Long
    ) {
        if (position <= 0L) return

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putLong(key(uri), position)
            .putLong("${key(uri)}_duration", duration)
            .apply()
    }

    fun getPosition(
        context: Context,
        uri: Uri
    ): Long {
        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getLong(key(uri), 0L)
    }

    fun getDuration(
        context: Context,
        uri: Uri
    ): Long {
        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getLong("${key(uri)}_duration", 0L)
    }

    fun getProgressPercent(
        context: Context,
        uri: Uri
    ): Int {
        val position = getPosition(context, uri)
        val duration = getDuration(context, uri)

        if (position <= 0L || duration <= 0L) {
            return 0
        }

        return ((position.toDouble() / duration.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    fun clear(
        context: Context,
        uri: Uri
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(key(uri))
            .remove("${key(uri)}_duration")
            .apply()
    }
}
