package com.vidora.player

import java.util.Locale

internal fun formatTime(milliseconds: Long): String {

    val totalSeconds =
        (milliseconds.coerceAtLeast(0L) / 1000L)

    val hours =
        totalSeconds / 3600L

    val minutes =
        (totalSeconds % 3600L) / 60L

    val seconds =
        totalSeconds % 60L

    return if (hours > 0L) {

        String.format(
            Locale.US,
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds
        )

    } else {

        String.format(
            Locale.US,
            "%02d:%02d",
            minutes,
            seconds
        )
    }
}
