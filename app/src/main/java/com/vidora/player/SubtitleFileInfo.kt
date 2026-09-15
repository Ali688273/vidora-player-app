package com.vidora.player

import android.net.Uri

data class SubtitleFileInfo(
    val uri: Uri,
    val language: String = "fa",
    val displayName: String = "Subtitle"
)
