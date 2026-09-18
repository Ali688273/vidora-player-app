package com.vidora.player

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.widget.LinearLayout

object PlaylistDialogManager {

    fun showAddToPlaylistDialog(
        context: Context,
        videoUri: Uri
    ) {

        val playlists =
            PlaylistVideoManager.getPlaylists(context)

        if (playlists.isEmpty()) {
            showCreatePlaylistDialog(
                context,
                videoUri
            )
            return
        }

        val names =
            playlists.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("افزودن به پلی‌لیست")
            .setItems(names) { _, which ->

                val name =
                    names[which]

                val added =
                    PlaylistVideoManager.addVideo(
                        context,
                        name,
                        videoUri
                    )

                AlertDialog.Builder(context)
                    .setMessage(
                        if (added) {
                            "ویدئو به «$name» اضافه شد."
                        } else {
                            "این ویدئو قبلاً در «$name» وجود دارد."
                        }
                    )
                    .setPositiveButton(
                        "باشه",
                        null
                    )
                    .show()
            }
            .setNeutralButton("پلی‌لیست جدید") { _, _ ->

                showCreatePlaylistDialog(
                    context,
                    videoUri
                )
            }
            .setNegativeButton(
                "لغو",
                null
            )
            .show()
    }

    private fun showCreatePlaylistDialog(
        context: Context,
        videoUri: Uri
    ) {

        val input =
            EditText(context).apply {
                hint = "نام پلی‌لیست"
                setSingleLine(true)
            }

        val container =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                val padding =
                    (
                        24 *
                            resources.displayMetrics.density
                        ).toInt()

                setPadding(
                    padding,
                    0,
                    padding,
                    0
                )

                addView(input)
            }

        val dialog =
            AlertDialog.Builder(context)
                .setTitle("پلی‌لیست جدید")
                .setView(container)
                .setNegativeButton(
                    "لغو",
                    null
                )
                .setPositiveButton(
                    "ساختن",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name =
                    input.text
                        .toString()
                        .trim()

                if (name.isEmpty()) {

                    input.error =
                        "نام پلی‌لیست را وارد کنید."

                    return@setOnClickListener
                }

                val created =
                    PlaylistVideoManager.createPlaylist(
                        context,
                        name
                    )

                if (!created) {

                    input.error =
                        "این نام قبلاً استفاده شده است."

                    return@setOnClickListener
                }

                PlaylistVideoManager.addVideo(
                    context,
                    name,
                    videoUri
                )

                dialog.dismiss()

                AlertDialog.Builder(context)
                    .setMessage(
                        "پلی‌لیست «$name» ساخته شد و ویدئو به آن اضافه شد."
                    )
                    .setPositiveButton(
                        "باشه",
                        null
                    )
                    .show()
            }
        }

        dialog.show()
    }

    fun showManagePlaylistsDialog(
        context: Context
    ) {

        val playlists =
            PlaylistVideoManager.getPlaylists(context)

        if (playlists.isEmpty()) {

            AlertDialog.Builder(context)
                .setTitle("پلی‌لیست‌ها")
                .setMessage(
                    "هنوز پلی‌لیستی ساخته نشده است."
                )
                .setPositiveButton(
                    "باشه",
                    null
                )
                .show()

            return
        }

        val names =
            playlists.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("پلی‌لیست‌ها")
            .setItems(names) { _, which ->

                showPlaylistActionsDialog(
                    context,
                    names[which]
                )
            }
            .setNegativeButton(
                "بستن",
                null
            )
            .show()
    }

    private fun showPlaylistActionsDialog(
        context: Context,
        playlistName: String
    ) {

        val actions =
            arrayOf(
                "نمایش ویدئوها",
                "تغییر نام",
                "حذف پلی‌لیست"
            )

        AlertDialog.Builder(context)
            .setTitle(playlistName)
            .setItems(actions) { _, which ->

                when (which) {

                    0 ->
                        showPlaylistVideos(
                            context,
                            playlistName
                        )

                    1 ->
                        showRenameDialog(
                            context,
                            playlistName
                        )

                    2 ->
                        confirmDeletePlaylist(
                            context,
                            playlistName
                        )
                }
            }
            .show()
    }

    private fun showPlaylistVideos(
        context: Context,
        playlistName: String
    ) {

        val videos =
            PlaylistVideoManager.getVideos(
                context,
                playlistName
            )

        if (videos.isEmpty()) {

            AlertDialog.Builder(context)
                .setTitle(playlistName)
                .setMessage(
                    "این پلی‌لیست خالی است."
                )
                .setPositiveButton(
                    "باشه",
                    null
                )
                .show()

            return
        }

        val names =
            videos.mapIndexed { index, value ->

                "${index + 1}. ${
                    getVideoDisplayName(
                        context,
                        Uri.parse(value)
                    )
                }"

            }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(playlistName)
            .setItems(names) { _, which ->

                val uri =
                    Uri.parse(
                        videos[which]
                    )

                openVideo(
                    context,
                    uri
                )
            }
            .setNegativeButton(
                "بستن",
                null
            )
            .show()
    }

    private fun showRenameDialog(
        context: Context,
        oldName: String
    ) {

        val input =
            EditText(context).apply {

                setText(oldName)
                setSingleLine(true)
                selectAll()
            }

        AlertDialog.Builder(context)
            .setTitle(
                "تغییر نام پلی‌لیست"
            )
            .setView(input)
            .setNegativeButton(
                "لغو",
                null
            )
            .setPositiveButton(
                "ذخیره"
            ) { _, _ ->

                val newName =
                    input.text
                        .toString()
                        .trim()

                if (newName.isEmpty()) {
                    return@setPositiveButton
                }

                PlaylistVideoManager.renamePlaylist(
                    context,
                    oldName,
                    newName
                )
            }
            .show()
    }

    private fun confirmDeletePlaylist(
        context: Context,
        playlistName: String
    ) {

        AlertDialog.Builder(context)
            .setTitle(
                "حذف پلی‌لیست"
            )
            .setMessage(
                "پلی‌لیست «$playlistName» حذف شود؟"
            )
            .setNegativeButton(
                "لغو",
                null
            )
            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                PlaylistVideoManager.deletePlaylist(
                    context,
                    playlistName
                )
            }
            .show()
    }

    private fun openVideo(
        context: Context,
        uri: Uri
    ) {

        val intent =
            Intent(
                context,
                PlayerActivity::class.java
            ).apply {

                putExtra(
                    PlayerActivity.EXTRA_VIDEO_URI,
                    uri.toString()
                )

                putExtra(
                    PlayerActivity.EXTRA_VIDEO_NAME,
                    getVideoDisplayName(
                        context,
                        uri
                    )
                )
            }

        context.startActivity(intent)
    }

    private fun getVideoDisplayName(
        context: Context,
        uri: Uri
    ): String {

        return try {

            context.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    cursor.getString(0)
                        ?: uri.lastPathSegment
                        ?: "ویدئو"

                } else {

                    uri.lastPathSegment
                        ?: "ویدئو"
                }

            } ?: (
                uri.lastPathSegment
                    ?: "ویدئو"
                )

        } catch (_: Exception) {

            uri.lastPathSegment
                ?: "ویدئو"
        }
    }
}
