package com.vidora.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object PlaybackHistory {

    private const val PREFS =
        "vidora_playback_history"

    private const val KEY_ITEMS =
        "items"

    private const val MAX_ITEMS =
        100

    data class Item(
        val uri: String,
        val position: Long,
        val duration: Long,
        val updatedAt: Long
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun save(
        context: Context,
        uri: Uri,
        position: Long,
        duration: Long
    ) {

        val current =
            load(context)
                .filterNot {
                    it.uri == uri.toString()
                }
                .toMutableList()

        current.add(
            0,
            Item(
                uri = uri.toString(),
                position = position.coerceAtLeast(0L),
                duration = duration.coerceAtLeast(0L),
                updatedAt = System.currentTimeMillis()
            )
        )

        val limited =
            current.take(
                MAX_ITEMS
            )

        val array =
            JSONArray()

        limited.forEach { item ->

            array.put(
                JSONObject().apply {

                    put(
                        "uri",
                        item.uri
                    )

                    put(
                        "position",
                        item.position
                    )

                    put(
                        "duration",
                        item.duration
                    )

                    put(
                        "updatedAt",
                        item.updatedAt
                    )
                }
            )
        }

        prefs(context)
            .edit()
            .putString(
                KEY_ITEMS,
                array.toString()
            )
            .apply()
    }

    fun load(
        context: Context
    ): List<Item> {

        val raw =
            prefs(context)
                .getString(
                    KEY_ITEMS,
                    null
                )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (
                    index in
                    0 until array.length()
                ) {

                    val item =
                        array.optJSONObject(
                            index
                        )
                            ?: continue

                    add(
                        Item(
                            uri =
                                item.optString(
                                    "uri"
                                ),
                            position =
                                item.optLong(
                                    "position"
                                ),
                            duration =
                                item.optLong(
                                    "duration"
                                ),
                            updatedAt =
                                item.optLong(
                                    "updatedAt"
                                )
                        )
                    )
                }
            }

        } catch (
            _: Exception
        ) {

            emptyList()
        }
    }

    fun get(
        context: Context,
        uri: Uri
    ): Item? {

        return load(context)
            .firstOrNull {
                it.uri ==
                    uri.toString()
            }
    }

    fun remove(
        context: Context,
        uri: Uri
    ) {

        val remaining =
            load(context)
                .filterNot {
                    it.uri ==
                        uri.toString()
                }

        val array =
            JSONArray()

        remaining.forEach { item ->

            array.put(
                JSONObject().apply {

                    put(
                        "uri",
                        item.uri
                    )

                    put(
                        "position",
                        item.position
                    )

                    put(
                        "duration",
                        item.duration
                    )

                    put(
                        "updatedAt",
                        item.updatedAt
                    )
                }
            )
        }

        prefs(context)
            .edit()
            .putString(
                KEY_ITEMS,
                array.toString()
            )
            .apply()
    }

    fun clear(
        context: Context
    ) {

        prefs(context)
            .edit()
            .remove(
                KEY_ITEMS
            )
            .apply()
    }
}
