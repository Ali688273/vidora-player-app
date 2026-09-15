package com.vidora.player

import android.content.Context
import android.util.AttributeSet
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.CaptionStyleCompat

@UnstableApi
class VidoraPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(
    context,
    attrs,
    defStyleAttr
) {

    init {

        keepScreenOn = true

        controllerAutoShow = true

        controllerHideOnTouch = true

        controllerShowTimeoutMs =
            3500

        subtitleView?.apply {

            setApplyEmbeddedStyles(
                false
            )

            setApplyEmbeddedFontSizes(
                false
            )

            setStyle(
                CaptionStyleCompat.DEFAULT
            )

            applySettings()
        }
    }

    private fun applySettings() {

        val preferences =
            context.getSharedPreferences(
                "vidora_player_preferences",
                Context.MODE_PRIVATE
            )

        val size =
            preferences.getFloat(
                "subtitle_size",
                0.0533f
            )

        subtitleView?.setFractionalTextSize(
            size
        )
    }

    fun refreshSubtitleSettings() {

        subtitleView?.apply {

            applySettings()

            invalidate()
        }
    }
}
