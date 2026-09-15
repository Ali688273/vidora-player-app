package com.vidora.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class AudioSyncProcessor : AudioProcessor {

    @Volatile
    private var offsetMs: Long = 0L

    private var sampleRate = 0
    private var channelCount = 0
    private var bytesPerFrame = 0

    private var skipBytes = 0

    private var delayedBytes =
        ByteArray(0)

    private var outputBuffer =
        AudioProcessor.EMPTY_BUFFER

    private var ended = false

    fun setOffset(
        offset: Long
    ) {
        offsetMs =
            offset.coerceIn(
                -10_000L,
                10_000L
            )

        flush()
    }

    override fun configure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {

        if (
            inputAudioFormat.encoding !=
            C.ENCODING_PCM_16BIT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(
                inputAudioFormat
            )
        }

        sampleRate =
            inputAudioFormat.sampleRate

        channelCount =
            inputAudioFormat.channelCount

        bytesPerFrame =
            max(
                1,
                channelCount * 2
            )

        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return offsetMs != 0L
    }

    override fun queueInput(
        inputBuffer: ByteBuffer
    ) {

        if (!isActive()) {

            val size =
                inputBuffer.remaining()

            outputBuffer =
                ByteBuffer.allocateDirect(
                    size
                ).order(
                    ByteOrder.nativeOrder()
                )

            outputBuffer.put(
                inputBuffer
            )

            outputBuffer.flip()

            return
        }

        val currentOffset =
            offsetMs

        if (currentOffset < 0L) {

            if (skipBytes == 0) {

                skipBytes =
                    (
                        (-currentOffset) *
                            sampleRate *
                            bytesPerFrame /
                            1000L
                        ).toInt()
                }

                skipBytes -=
                    skipBytes %
                        bytesPerFrame
            }

            val available =
                inputBuffer.remaining()

            val toSkip =
                minOf(
                    skipBytes,
                    available
                )

            inputBuffer.position(
                inputBuffer.position() +
                    toSkip
            )

            skipBytes -= toSkip

            val remaining =
                inputBuffer.remaining()

            if (remaining <= 0) {
                outputBuffer =
                    AudioProcessor.EMPTY_BUFFER
                return
            }

            outputBuffer =
                ByteBuffer.allocateDirect(
                    remaining
                ).order(
                    ByteOrder.nativeOrder()
                )

            outputBuffer.put(
                inputBuffer
            )

            outputBuffer.flip()

            return
        }

        val delayBytes =
            (
                currentOffset *
                    sampleRate *
                    bytesPerFrame /
                    1000L
                ).toInt()
                .coerceAtLeast(0)

        val incoming =
            ByteArray(
                inputBuffer.remaining()
            )

        inputBuffer.get(
            incoming
        )

        val combined =
            ByteArray(
                delayedBytes.size +
                    incoming.size
            )

        System.arraycopy(
            delayedBytes,
            0,
            combined,
            0,
            delayedBytes.size
        )

        System.arraycopy(
            incoming,
            0,
            combined,
            delayedBytes.size,
            incoming.size
        )

        if (
            combined.size <=
            delayBytes
        ) {

            delayedBytes =
                combined

            outputBuffer =
                AudioProcessor.EMPTY_BUFFER

            return
        }

        val outputSize =
            combined.size -
                delayBytes

        val output =
            ByteBuffer.allocateDirect(
                outputSize
            ).order(
                ByteOrder.nativeOrder()
            )

        output.put(
            combined,
            0,
            outputSize
        )

        output.flip()

        outputBuffer =
            output

        delayedBytes =
            combined.copyOfRange(
                outputSize,
                combined.size
            )
    }

    override fun queueEndOfStream() {

        ended = true

        if (
            delayedBytes.isNotEmpty()
        ) {

            val output =
                ByteBuffer.allocateDirect(
                    delayedBytes.size
                ).order(
                    ByteOrder.nativeOrder()
                )

            output.put(
                delayedBytes
            )

            output.flip()

            outputBuffer =
                output

            delayedBytes =
                ByteArray(0)
        }
    }

    override fun getOutput(): ByteBuffer {

        val result =
            outputBuffer

        outputBuffer =
            AudioProcessor.EMPTY_BUFFER

        return result
    }

    override fun isEnded(): Boolean {

        return ended &&
            outputBuffer ===
            AudioProcessor.EMPTY_BUFFER &&
            delayedBytes.isEmpty()
    }

    override fun flush() {

        delayedBytes =
            ByteArray(0)

        outputBuffer =
            AudioProcessor.EMPTY_BUFFER

        ended = false

        if (sampleRate > 0) {

            skipBytes =
                if (offsetMs < 0L) {

                    (
                        (-offsetMs) *
                            sampleRate *
                            bytesPerFrame /
                            1000L
                        ).toInt()

                } else {
                    0
                }

            skipBytes -=
                skipBytes %
                    bytesPerFrame
        }
    }

    override fun reset() {

        flush()

        sampleRate = 0
        channelCount = 0
        bytesPerFrame = 0
        skipBytes = 0
    }
}
