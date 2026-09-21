package com.vidora.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioSyncProcessor : AudioProcessor {

    @Volatile
    private var offsetMs: Long = 0L

    private var sampleRate = 0
    private var channelCount = 0
    private var bytesPerFrame = 0

    private var skipBytes = 0

    private var delayedBytes = ByteArray(0)

    private var outputBuffer =
        AudioProcessor.EMPTY_BUFFER

    private var ended = false
    private var configured = false

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

        configured =
            inputAudioFormat.encoding ==
                C.ENCODING_PCM_16BIT

        sampleRate =
            inputAudioFormat.sampleRate

        channelCount =
            inputAudioFormat.channelCount

        bytesPerFrame =
            if (configured) {
                (channelCount * 2).coerceAtLeast(1)
            } else {
                0
            }

        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return configured && offsetMs != 0L
    }

    override fun queueInput(
        inputBuffer: ByteBuffer
    ) {

        if (!configured || !isActive()) {
            outputBuffer = inputBuffer
            return
        }

        val currentOffset =
            offsetMs

        if (currentOffset < 0L) {

            queueNegativeOffset(
                inputBuffer,
                currentOffset
            )

            return
        }

        queuePositiveOffset(
            inputBuffer,
            currentOffset
        )
    }

    private fun queueNegativeOffset(
        inputBuffer: ByteBuffer,
        currentOffset: Long
    ) {

        if (skipBytes <= 0) {

            skipBytes =
                (
                    (-currentOffset) *
                        sampleRate *
                        bytesPerFrame /
                        1000L
                    ).toInt()

            if (bytesPerFrame > 0) {

                skipBytes -=
                    skipBytes % bytesPerFrame
            }
        }

        val available =
            inputBuffer.remaining()

        if (available <= 0) {

            outputBuffer =
                AudioProcessor.EMPTY_BUFFER

            return
        }

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

        val output =
            ByteBuffer.allocateDirect(
                remaining
            ).order(
                ByteOrder.nativeOrder()
            )

        output.put(inputBuffer)
        output.flip()

        outputBuffer = output
    }

    private fun queuePositiveOffset(
        inputBuffer: ByteBuffer,
        currentOffset: Long
    ) {

        val delayBytes =
            (
                currentOffset *
                    sampleRate *
                    bytesPerFrame /
                    1000L
                ).toInt()
                .coerceAtLeast(0)
                .let { value ->

                    if (bytesPerFrame > 0) {
                        value -
                            (value % bytesPerFrame)
                    } else {
                        value
                    }
                }

        val incomingSize =
            inputBuffer.remaining()

        if (incomingSize <= 0) {

            outputBuffer =
                AudioProcessor.EMPTY_BUFFER

            return
        }

        val incoming =
            ByteArray(incomingSize)

        inputBuffer.get(incoming)

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

        outputBuffer = output

        delayedBytes =
            combined.copyOfRange(
                outputSize,
                combined.size
            )
    }

    override fun queueEndOfStream() {

        ended = true

        if (
            delayedBytes.isEmpty()
        ) {

            return
        }

        val output =
            ByteBuffer.allocateDirect(
                delayedBytes.size
            ).order(
                ByteOrder.nativeOrder()
            )

        output.put(delayedBytes)
        output.flip()

        outputBuffer = output

        delayedBytes =
            ByteArray(0)
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

        skipBytes =
            if (
                configured &&
                offsetMs < 0L &&
                sampleRate > 0 &&
                bytesPerFrame > 0
            ) {

                (
                    (-offsetMs) *
                        sampleRate *
                        bytesPerFrame /
                        1000L
                    ).toInt()
                    .let { value ->
                        value -
                            (
                                value %
                                    bytesPerFrame
                                )
                    }

            } else {

                0
            }
    }

    override fun reset() {

        flush()

        sampleRate = 0
        channelCount = 0
        bytesPerFrame = 0
        skipBytes = 0
        configured = false
    }
}
