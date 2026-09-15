package com.vidora.player

import android.util.Patterns

object VideoUrlValidator {

    fun isValid(
        value: String
    ): Boolean {

        val url =
            value.trim()

        if (
            url.isEmpty()
        ) {
            return false
        }

        if (
            !Patterns.WEB_URL
                .matcher(url)
                .matches()
        ) {
            return false
        }

        return url.startsWith(
            "http://",
            ignoreCase = true
        ) ||
            url.startsWith(
                "https://",
                ignoreCase = true
            )
    }
}
