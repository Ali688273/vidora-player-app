package com.vidora.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray

object PlaybackQueueManager {

    private const val PREFS = "vidora_playback_queue"
    private const val KEY_QUEUE = "queue"
    private const val KEY_INDEX = "current_index"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun getQueue(context: Context): List<Uri> {

        val json =
            prefs(context)
                .getString(KEY_QUEUE, null)
                ?: return emptyList()

        return try {
            val array = JSONArray(json)
            val result = mutableListOf<Uri>()

            for (index in 0 until array.length()) {
                val value = array.optString(index)

                if (value.isNotBlank()) {
                    result.add(Uri.parse(value))
                }
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setQueue(
        context: Context,
        queue: List<Uri>
    ) {

        val array = JSONArray()

        queue.distinctBy { it.toString() }
            .forEach {
                array.put(it.toString())
            }

        prefs(context)
            .edit()
            .putString(
                KEY_QUEUE,
                array.toString()
            )
            .apply()

        val maxIndex =
            (queue.size - 1)
                .coerceAtLeast(0)

        val currentIndex =
            getCurrentIndex(context)
                .coerceIn(0, maxIndex)

        setCurrentIndex(
            context,
            currentIndex
        )
    }

    fun add(
        context: Context,
        uri: Uri
    ) {

        val queue =
            getQueue(context)
                .toMutableList()

        if (
            queue.none {
                it.toString() ==
                    uri.toString()
            }
        ) {
            queue.add(uri)
            setQueue(context, queue)
        }
    }

    fun remove(
        context: Context,
        uri: Uri
    ) {

        val queue =
            getQueue(context)
                .filter {
                    it.toString() !=
                        uri.toString()
                }

        setQueue(context, queue)
    }

    fun clear(context: Context) {

        prefs(context)
            .edit()
            .remove(KEY_QUEUE)
            .putInt(KEY_INDEX, 0)
            .apply()
    }

    fun getCurrentIndex(
        context: Context
    ): Int {

        return prefs(context)
            .getInt(KEY_INDEX, 0)
    }

    fun setCurrentIndex(
        context: Context,
        index: Int
    ) {

        prefs(context)
            .edit()
            .putInt(
                KEY_INDEX,
                index.coerceAtLeast(0)
            )
            .apply()
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

        if (nextIndex >= queue.size) {
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

        if (previousIndex < 0) {
            return null
        }

        setCurrentIndex(
            context,
            previousIndex
        )

        return queue[previousIndex]
    }
}
