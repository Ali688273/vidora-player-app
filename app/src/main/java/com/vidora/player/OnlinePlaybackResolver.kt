package com.vidora.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

object OnlinePlaybackResolver {

    fun createMediaItem(
        url: String
    ): MediaItem {

        val cleanUrl =
            url.trim()

        require(
            cleanUrl.startsWith(
                "http://",
                ignoreCase = true
            ) ||
                cleanUrl.startsWith(
                    "https://",
                    ignoreCase = true
                )
        ) {
            "Invalid video URL"
        }

        val uri =
            Uri.parse(cleanUrl)

        val lower =
            cleanUrl.lowercase()

        val builder =
            MediaItem.Builder()
                .setUri(uri)

        when {
            lower.contains(".m3u8") -> {
                builder.setMimeType(
                    MimeTypes.APPLICATION_M3U8
                )
            }

            lower.contains(".mpd") -> {
                builder.setMimeType(
                    MimeTypes.APPLICATION_MPD
                )
            }
        }

        return builder.build()
    }
}
