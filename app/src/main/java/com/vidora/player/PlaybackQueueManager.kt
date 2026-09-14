package com.vidora.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray

object PlaybackQueueManager {

    private const val PREFS =
        "vidora_playback_queue"

    private const val KEY_QUEUE =
        "queue"

    private const val KEY_INDEX =
        "current_index"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun getQueue(
        context: Context
    ): List<Uri> {

        val raw =
            prefs(context)
                .getString(
                    KEY_QUEUE,
                    "[]"
                )
                ?: "[]"

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (
                    index in
                    0 until array.length()
                ) {

                    add(
                        Uri.parse(
                            array.getString(index)
                        )
                    )
                }
            }

        } catch (_: Exception) {

            emptyList()
        }
    }

    fun setQueue(
        context: Context,
        queue: List<Uri>
    ) {

        val array =
            JSONArray()

        queue.forEach {
            array.put(
                it.toString()
            )
        }

        prefs(context)
            .edit()
            .putString(
                KEY_QUEUE,
                array.toString()
            )
            .apply()
    }

    fun add(
        context: Context,
        uri: Uri
    ) {

        val queue =
            getQueue(context)
                .toMutableList()

        if (
            !queue.contains(uri)
        ) {
            queue.add(uri)
        }

        setQueue(
            context,
            queue
        )
    }

    fun remove(
        context: Context,
        uri: Uri
    ) {

        val queue =
            getQueue(context)
                .filterNot {
                    it == uri
                }

        setQueue(
            context,
            queue
        )
    }

    fun clear(
        context: Context
    ) {

        setQueue(
            context,
            emptyList()
        )

        setCurrentIndex(
            context,
            0
        )
    }

    fun setCurrentIndex(
        context: Context,
        index: Int
    ) {

        prefs(context)
            .edit()
            .putInt(
                KEY_INDEX,
                index
            )
            .apply()
    }

    fun getCurrentIndex(
        context: Context
    ): Int {

        return prefs(context)
            .getInt(
                KEY_INDEX,
                0
            )
    }

    fun next(
        context: Context
    ): Uri? {

        val queue =
            getQueue(context)

        if (queue.isEmpty()) {
            return null
        }

        val nextIndex =
            getCurrentIndex(context) + 1

        if (
            nextIndex >= queue.size
        ) {
            return null
        }

        setCurrentIndex(
            context,
            nextIndex
        )

        return queue[nextIndex]
    }

    fun previous(
        context: Context
    ): Uri? {

        val queue =
            getQueue(context)

        if (queue.isEmpty()) {
            return null
        }

        val previousIndex =
            getCurrentIndex(context) - 1

        if (
            previousIndex < 0
        ) {
            return null
        }

        setCurrentIndex(
            context,
            previousIndex
        )

        return queue[previousIndex]
    }
}
