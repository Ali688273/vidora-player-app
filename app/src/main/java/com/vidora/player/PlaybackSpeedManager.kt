package com.vidora.player

import android.content.Context

object PlaybackSpeedManager {

    private val supportedSpeeds =
        listOf(
            0.25f,
            0.5f,
            0.75f,
            1.0f,
            1.25f,
            1.5f,
            1.75f,
            2.0f,
            2.5f,
            3.0f,
            4.0f,
            5.0f
        )

    fun values(): List<Float> {
        return supportedSpeeds
    }

    fun next(
        current: Float
    ): Float {

        return supportedSpeeds
            .firstOrNull {
                it > current + 0.001f
            }
            ?: supportedSpeeds.last()
    }

    fun previous(
        current: Float
    ): Float {

        return supportedSpeeds
            .lastOrNull {
                it < current - 0.001f
            }
            ?: supportedSpeeds.first()
    }

    fun defaultSpeed(
        context: Context
    ): Float {

        return PlaybackSettings
            .getDefaultSpeed(
                context
            )
    }

    fun setDefaultSpeed(
        context: Context,
        speed: Float
    ) {

        PlaybackSettings
            .setDefaultSpeed(
                context,
                speed
            )
    }
}
