package com.vidora.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray

object PlaylistVideoManager {

    private const val PREFS = "vidora_playlists"
    private const val KEY_PLAYLISTS = "playlists"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun getPlaylists(context: Context): List<String> {

        val raw =
            prefs(context).getString(
                KEY_PLAYLISTS,
                null
            ) ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (i in 0 until array.length()) {
                    add(
                        array.getString(i)
                    )
                }
            }

        } catch (_: Exception) {
            emptyList()
        }
    }

    fun createPlaylist(
        context: Context,
        name: String
    ): Boolean {

        val cleanName =
            name.trim()

        if (cleanName.isEmpty()) {
            return false
        }

        val playlists =
            getPlaylists(context).toMutableList()

        if (
            playlists.any {
                it.equals(
                    cleanName,
                    ignoreCase = true
                )
            }
        ) {
            return false
        }

        playlists.add(cleanName)

        savePlaylists(
            context,
            playlists
        )

        return true
    }

    fun deletePlaylist(
        context: Context,
        name: String
    ) {

        val playlists =
            getPlaylists(context)
                .filterNot {
                    it == name
                }

        savePlaylists(
            context,
            playlists
        )

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                playlistKey(name)
            )
            .apply()
    }

    fun renamePlaylist(
        context: Context,
        oldName: String,
        newName: String
    ): Boolean {

        val cleanName =
            newName.trim()

        if (cleanName.isEmpty()) {
            return false
        }

        val playlists =
            getPlaylists(context).toMutableList()

        if (
            playlists.any {
                it != oldName &&
                    it.equals(
                        cleanName,
                        ignoreCase = true
                    )
            }
        ) {
            return false
        }

        val index =
            playlists.indexOf(oldName)

        if (index < 0) {
            return false
        }

        val videos =
            getVideos(
                context,
                oldName
            )

        playlists[index] =
            cleanName

        savePlaylists(
            context,
            playlists
        )

        saveVideos(
            context,
            cleanName,
            videos
        )

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                playlistKey(oldName)
            )
            .apply()

        return true
    }

    fun addVideo(
        context: Context,
        playlistName: String,
        uri: Uri
    ): Boolean {

        if (
            !getPlaylists(context)
                .contains(playlistName)
        ) {
            return false
        }

        val videos =
            getVideos(
                context,
                playlistName
            ).toMutableList()

        val value =
            uri.toString()

        if (videos.contains(value)) {
            return false
        }

        videos.add(value)

        saveVideos(
            context,
            playlistName,
            videos
        )

        return true
    }

    fun removeVideo(
        context: Context,
        playlistName: String,
        uri: Uri
    ) {

        val videos =
            getVideos(
                context,
                playlistName
            ).filterNot {
                it == uri.toString()
            }

        saveVideos(
            context,
            playlistName,
            videos
        )
    }

    fun getVideos(
        context: Context,
        playlistName: String
    ): List<String> {

        val raw =
            prefs(context).getString(
                playlistKey(playlistName),
                null
            ) ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (i in 0 until array.length()) {
                    add(
                        array.getString(i)
                    )
                }
            }

        } catch (_: Exception) {
            emptyList()
        }
    }

    fun containsVideo(
        context: Context,
        playlistName: String,
        uri: Uri
    ): Boolean {

        return getVideos(
            context,
            playlistName
        ).contains(
            uri.toString()
        )
    }

    private fun savePlaylists(
        context: Context,
        playlists: List<String>
    ) {

        val array =
            JSONArray()

        playlists.forEach {
            array.put(it)
        }

        prefs(context)
            .edit()
            .putString(
                KEY_PLAYLISTS,
                array.toString()
            )
            .apply()
    }

    private fun saveVideos(
        context: Context,
        playlistName: String,
        videos: List<String>
    ) {

        val array =
            JSONArray()

        videos.forEach {
            array.put(it)
        }

        prefs(context)
            .edit()
            .putString(
                playlistKey(playlistName),
                array.toString()
            )
            .apply()
    }

    private fun playlistKey(
        name: String
    ): String {

        return "playlist_" +
            name.hashCode()
    }
}
