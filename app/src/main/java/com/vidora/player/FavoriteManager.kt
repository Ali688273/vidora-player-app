package com.vidora.player

import android.content.Context
import android.net.Uri

object FavoriteManager {

    private const val PREFS_NAME =
        "vidora_favorites"

    private const val PREFIX =
        "favorite_"

    private fun key(uri: Uri): String {
        return PREFIX + uri.toString()
    }

    fun isFavorite(
        context: Context,
        uri: Uri
    ): Boolean {

        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                key(uri),
                false
            )
    }

    fun setFavorite(
        context: Context,
        uri: Uri,
        favorite: Boolean
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                key(uri),
                favorite
            )
            .apply()
    }

    fun toggle(
        context: Context,
        uri: Uri
    ): Boolean {

        val newValue =
            !isFavorite(
                context,
                uri
            )

        setFavorite(
            context,
            uri,
            newValue
        )

        return newValue
    }
}
