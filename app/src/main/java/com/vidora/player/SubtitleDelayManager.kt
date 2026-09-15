package com.vidora.player

import android.content.Context
import android.net.Uri

object SubtitleDelayManager {

    private const val PREFS =
        "vidora_subtitle_delay"

    private fun key(
        uri: Uri
    ): String {
        return "delay_${uri}"
    }

    fun get(
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
            .coerceIn(
                -10_000L,
                10_000L
            )
    }

    fun set(
        context: Context,
        uri: Uri,
        value: Long
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putLong(
                key(uri),
                value.coerceIn(
                    -10_000L,
                    10_000L
                )
            )
            .apply()
    }

    fun increase(
        context: Context,
        uri: Uri,
        step: Long = 250L
    ): Long {

        val value =
            (
                get(context, uri) +
                    step
            ).coerceIn(
                -10_000L,
                10_000L
            )

        set(
            context,
            uri,
            value
        )

        return value
    }

    fun decrease(
        context: Context,
        uri: Uri,
        step: Long = 250L
    ): Long {

        val value =
            (
                get(context, uri) -
                    step
            ).coerceIn(
                -10_000L,
                10_000L
            )

        set(
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

        set(
            context,
            uri,
            0L
        )
    }
}
