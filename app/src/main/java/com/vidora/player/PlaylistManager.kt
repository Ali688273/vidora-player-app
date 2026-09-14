package com.vidora.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object PlaylistManager {

    private const val PREFS = "vidora_playlists"
    private const val KEY_PLAYLISTS = "playlists"

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    data class Playlist(
        val name: String,
        val videos: List<String>
    )

    fun getPlaylists(
        context: Context
    ): List<Playlist> {

        val raw =
            preferences(context)
                .getString(
                    KEY_PLAYLISTS,
                    "[]"
                )
                ?: "[]"

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (i in 0 until array.length()) {

                    val objectValue =
                        array.getJSONObject(i)

                    val name =
                        objectValue.optString(
                            "name"
                        )

                    val videosArray =
                        objectValue.optJSONArray(
                            "videos"
                        ) ?: JSONArray()

                    val videos =
                        buildList {

                            for (
                                j in
                                0 until videosArray.length()
                            ) {
                                add(
                                    videosArray.getString(j)
                                )
                            }
                        }

                    if (name.isNotBlank()) {
                        add(
                            Playlist(
                                name = name,
                                videos = videos
                            )
                        )
                    }
                }
            }

        } catch (_: Exception) {

            emptyList()
        }
    }

    private fun savePlaylists(
        context: Context,
        playlists: List<Playlist>
    ) {

        val array =
            JSONArray()

        playlists.forEach { playlist ->

            val objectValue =
                JSONObject()

            objectValue.put(
                "name",
                playlist.name
            )

            val videos =
                JSONArray()

            playlist.videos.forEach {
                videos.put(it)
            }

            objectValue.put(
                "videos",
                videos
            )

            array.put(
                objectValue
            )
        }

        preferences(context)
            .edit()
            .putString(
                KEY_PLAYLISTS,
                array.toString()
            )
            .apply()
    }

    fun create(
        context: Context,
        name: String
    ): Boolean {

        val cleanName =
            name.trim()

        if (cleanName.isBlank()) {
            return false
        }

        val playlists =
            getPlaylists(context)
                .toMutableList()

        if (
            playlists.any {
                it.name.equals(
                    cleanName,
                    ignoreCase = true
                )
            }
        ) {
            return false
        }

        playlists.add(
            Playlist(
                name = cleanName,
                videos = emptyList()
            )
        )

        savePlaylists(
            context,
            playlists
        )

        return true
    }

    fun delete(
        context: Context,
        name: String
    ) {

        val playlists =
            getPlaylists(context)
                .filterNot {
                    it.name == name
                }

        savePlaylists(
            context,
            playlists
        )
    }

    fun addVideo(
        context: Context,
        playlistName: String,
        uri: Uri
    ): Boolean {

        val playlists =
            getPlaylists(context)
                .toMutableList()

        val index =
            playlists.indexOfFirst {
                it.name == playlistName
            }

        if (index < 0) {
            return false
        }

        val playlist =
            playlists[index]

        val uriString =
            uri.toString()

        if (
            playlist.videos.contains(
                uriString
            )
        ) {
            return false
        }

        playlists[index] =
            playlist.copy(
                videos =
                    playlist.videos +
                        uriString
            )

        savePlaylists(
            context,
            playlists
        )

        return true
    }

    fun removeVideo(
        context: Context,
        playlistName: String,
        uri: Uri
    ) {

        val playlists =
            getPlaylists(context)
                .toMutableList()

        val index =
            playlists.indexOfFirst {
                it.name == playlistName
            }

        if (index < 0) {
            return
        }

        val playlist =
            playlists[index]

        playlists[index] =
            playlist.copy(
                videos =
                    playlist.videos.filterNot {
                        it == uri.toString()
                    }
            )

        savePlaylists(
            context,
            playlists
        )
    }
}
