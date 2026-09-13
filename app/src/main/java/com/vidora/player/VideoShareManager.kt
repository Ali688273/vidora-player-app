package com.vidora.player

import android.content.Context
import android.content.Intent
import android.net.Uri

object VideoShareManager {

    fun share(
        context: Context,
        uri: Uri
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_video)
            )
        )
    }
}
