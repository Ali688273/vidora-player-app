package com.vidora.player

import android.content.Context
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView

object SubtitleAppearance {

    fun apply(
        context: Context,
        subtitleView: SubtitleView
    ) {

        val size =
            SubtitleSettings.getSize(
                context
            )

        subtitleView.setApplyEmbeddedStyles(
            false
        )

        subtitleView.setApplyEmbeddedFontSizes(
            false
        )

        subtitleView.setFractionalTextSize(
            size
        )

        subtitleView.setStyle(
            if (
                SubtitleSettings.bold(
                    context
                )
            ) {
                CaptionStyleCompat(
                    0xFFFFFFFF.toInt(),
                    if (
                        SubtitleSettings.useBackground(
                            context
                        )
                    ) {
                        0xB0000000.toInt()
                    } else {
                        0x00000000
                    },
                    0x00000000,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    0xFF000000.toInt(),
                    null
                )
            } else {
                CaptionStyleCompat.DEFAULT
            }
        )
    }
}
