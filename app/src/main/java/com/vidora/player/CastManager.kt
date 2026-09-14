package com.vidora.player

import android.content.Context
import android.net.Uri

object CastManager {

    private var currentUri: Uri? =
        null

    fun prepare(
        context: Context,
        uri: Uri
    ) {
        currentUri = uri
    }

    fun getCurrentUri(): Uri? {
        return currentUri
    }

    fun clear() {
        currentUri = null
    }

    fun isPrepared(): Boolean {
        return currentUri != null
    }
}
