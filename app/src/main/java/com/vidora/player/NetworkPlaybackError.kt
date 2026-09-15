package com.vidora.player

import androidx.media3.common.PlaybackException

object NetworkPlaybackError {

    fun message(
        error: PlaybackException
    ): String {

        return when (
            error.errorCode
        ) {

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                "اتصال اینترنت برقرار نیست."

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "سرور ویدیو پاسخ مناسبی نداد."

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "ویدیو پیدا نشد."

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                "فرمت ویدیو قابل پخش نیست."

            else ->
                error.localizedMessage
                    ?: "خطا در پخش ویدیو."
        }
    }
}
