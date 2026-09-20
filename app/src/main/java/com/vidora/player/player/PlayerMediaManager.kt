package com.vidora.player

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View

import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player

internal fun PlayerActivity.handlePlaybackEnded() {

    if (isExternalVideo()) {
        return
    }

    if (repeatEnabled) {
        return
    }

    if (!PlaybackSettings.autoPlayNext(this)) {

        updatePauseButton()
        updateCenterPlayButton()
        showPlayerControlsTemporarily()

        return
    }

    val currentUri =
        getIncomingVideoUri()

    if (currentUri != null) {

        val queue =
            PlaybackQueueManager.getQueue(
                this
            )

        val currentQueueIndex =
            queue.indexOfFirst {
                it.toString() ==
                    currentUri.toString()
            }

        if (currentQueueIndex >= 0) {

            PlaybackQueueManager.setCurrentIndex(
                this,
                currentQueueIndex
            )

            val nextUri =
                PlaybackQueueManager.getNext(
                    this,
                    false
                )

            if (nextUri != null) {

                PlaybackQueueManager.setCurrentVideo(
                    this,
                    nextUri
                )

                openVideo(nextUri)

                showTemporaryMessage(
                    p(
                        "ویدئوی بعدی صف",
                        "Next video in queue"
                    )
                )

                return
            }

            updatePauseButton()
            updateCenterPlayButton()
            showPlayerControlsTemporarily()

            showTemporaryMessage(
                p(
                    "صف پخش به پایان رسید.",
                    "Playback queue ended."
                )
            )

            return
        }
    }

    val videos =
        getVideoUris()

    if (videos.isEmpty()) {

        updatePauseButton()
        updateCenterPlayButton()

        return
    }

    val currentIndex =
        currentUri?.let { uri ->
            videos.indexOfFirst {
                it.toString() ==
                    uri.toString()
            }
        } ?: -1

    val nextIndex =
        when {

            currentIndex < 0 ->
                0

            currentIndex >=
                videos.lastIndex ->
                0

            else ->
                currentIndex + 1
        }

    openVideo(
        videos[nextIndex]
    )

    showTemporaryMessage(
        p(
            "ویدئوی بعدی",
            "Next video"
        )
    )
}

internal fun PlayerActivity.getIncomingVideoUri(): Uri? {

    if (
        intent.action ==
        Intent.ACTION_VIEW
    ) {
        return intent.data
    }

    val uriString =
        intent.getStringExtra(
            PlayerActivity.EXTRA_VIDEO_URI
        )

    if (uriString.isNullOrBlank()) {
        return null
    }

    return Uri.parse(uriString)
}

internal fun PlayerActivity.isExternalVideo(): Boolean {
    return intent.action ==
        Intent.ACTION_VIEW
}

internal fun PlayerActivity.initializePlayer() {

    val uri =
        getIncomingVideoUri()
            ?: return

    if (isExternalVideo()) {

        deleteButton.visibility =
            View.GONE

        createPlayer(
            MediaItem.fromUri(uri)
        )

        return
    }

    syncQueueCurrentVideo(uri)

    val savedSubtitle =
        SubtitleFileManager.getSubtitleUri(
            this,
            uri
        )

    if (savedSubtitle != null) {

        createPlayerWithSubtitle(
            uri,
            savedSubtitle
        )

    } else {

        createPlayer(
            MediaItem.fromUri(uri)
        )
    }
}

internal fun PlayerActivity.loadVideoWithSubtitle(
    subtitleUri: Uri
) {

    val videoUri =
        getIncomingVideoUri()
            ?: return

    SubtitleFileManager.setSubtitleUri(
        this,
        videoUri,
        subtitleUri
    )

    createPlayerWithSubtitle(
        videoUri,
        subtitleUri
    )
}

internal fun PlayerActivity.createPlayerWithSubtitle(
    videoUri: Uri,
    subtitleUri: Uri
) {

    val subtitleMimeType =
        getSubtitleMimeType(
            subtitleUri
        )

    val subtitleLanguage =
        detectSubtitleLanguage(
            subtitleUri
        )

    val subtitle =
        MediaItem.SubtitleConfiguration.Builder(
            subtitleUri
        )
            .setMimeType(
                subtitleMimeType
            )
            .setLanguage(
                subtitleLanguage
            )
            .setSelectionFlags(
                C.SELECTION_FLAG_DEFAULT
            )
            .build()

    val mediaItem =
        MediaItem.Builder()
            .setUri(videoUri)
            .setSubtitleConfigurations(
                listOf(subtitle)
            )
            .build()

    createPlayer(
        mediaItem
    )
}

internal fun PlayerActivity.getSubtitleMimeType(
    subtitleUri: Uri
): String {

    val detectedType =
        try {
            contentResolver.getType(
                subtitleUri
            )
        } catch (_: Exception) {
            null
        }

    if (
        detectedType ==
        MimeTypes.TEXT_VTT
    ) {
        return MimeTypes.TEXT_VTT
    }

    if (
        detectedType ==
        MimeTypes.APPLICATION_SUBRIP
    ) {
        return MimeTypes.APPLICATION_SUBRIP
    }

    val name =
        getDisplayName(
            subtitleUri
        ).lowercase()

    return when {

        name.endsWith(".vtt") ->
            MimeTypes.TEXT_VTT

        name.endsWith(".srt") ->
            MimeTypes.APPLICATION_SUBRIP

        else ->
            MimeTypes.APPLICATION_SUBRIP
    }
}

internal fun PlayerActivity.detectSubtitleLanguage(
    subtitleUri: Uri
): String {

    val name =
        getDisplayName(
            subtitleUri
        ).lowercase()

    return when {

        name.contains(".fa.") ||
            name.contains("_fa.") ||
            name.contains("-fa.") ->
            "fa"

        name.contains(".en.") ||
            name.contains("_en.") ||
            name.contains("-en.") ->
            "en"

        name.contains(".ar.") ||
            name.contains("_ar.") ||
            name.contains("-ar.") ->
            "ar"

        name.contains(".de.") ||
            name.contains("_de.") ||
            name.contains("-de.") ->
            "de"

        name.contains(".fr.") ||
            name.contains("_fr.") ||
            name.contains("-fr.") ->
            "fr"

        else ->
            "fa"
    }
}

internal fun PlayerActivity.getDisplayName(
    uri: Uri
): String {

    return try {

        contentResolver.query(
            uri,
            arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                cursor.getString(0)
                    ?: uri.lastPathSegment
                    ?: "subtitle"

            } else {

                uri.lastPathSegment
                    ?: "subtitle"
            }

        } ?: (
            uri.lastPathSegment
                ?: "subtitle"
            )

    } catch (_: Exception) {

        uri.lastPathSegment
            ?: "subtitle"
    }
}

internal fun PlayerActivity.createPlayer(
    mediaItem: MediaItem
) {

    val currentPlayer =
        player ?: return

    val videoUri =
        mediaItem.localConfiguration
            ?.uri
            ?: return

    if (!isExternalVideo()) {
        syncQueueCurrentVideo(
            videoUri
        )
    }

    /*
     * موقعیت Resume قبل از setMediaItem مشخص می‌شود.
     * onMediaItemTransition دیگر این مقدار را پاک نمی‌کند.
     */
    pendingResumePosition =
        if (
            isExternalVideo() ||
            !VidoraSettings.autoResume(this)
        ) {

            0L

        } else {

            PlaybackHistoryManager
                .getPosition(
                    this,
                    videoUri
                )
                .coerceAtLeast(0L)
        }

    resumePositionApplied =
        pendingResumePosition <= 0L

    currentSpeed =
        if (isExternalVideo()) {

            1.0f

        } else {

            VideoSpeedManager.getSpeed(
                this,
                videoUri
            )
        }

    currentPlayer.pause()

    currentPlayer.setMediaItem(
        mediaItem
    )

    currentPlayer.repeatMode =
        if (repeatEnabled) {

            Player.REPEAT_MODE_ONE

        } else {

            Player.REPEAT_MODE_OFF
        }

    currentPlayer.playbackParameters =
        PlaybackParameters(
            currentSpeed
        )

    currentPlayer.prepare()

    updateSpeedText()

    intent.putExtra(
        PlayerActivity.EXTRA_VIDEO_URI,
        videoUri.toString()
    )

    updateFavoriteButton()
    updatePauseButton()
    updateCenterPlayButton()
    updateProgress()

    applyAspectMode()

    showPlayerControlsTemporarily()

    if (
        pendingResumePosition <= 0L
    ) {
        currentPlayer.play()
    }
}

internal fun PlayerActivity.applyPendingResumePosition() {

    if (resumePositionApplied) {
        return
    }

    val currentPlayer =
        player ?: return

    val position =
        pendingResumePosition

    /*
     * قبل از تغییر وضعیت، پرچم را set می‌کنیم
     * تا callbackهای متعدد دوباره Resume را اجرا نکنند.
     */
    resumePositionApplied =
        true

    pendingResumePosition =
        0L

    if (position <= 0L) {

        currentPlayer.play()

        return
    }

    val duration =
        currentPlayer.duration

    val safePosition =
        if (
            duration > 0L &&
            duration != C.TIME_UNSET
        ) {

            position.coerceIn(
                0L,
                (
                    duration -
                        500L
                    ).coerceAtLeast(0L)
            )

        } else {

            position
        }

    try {

        currentPlayer.seekTo(
            safePosition
        )

    } catch (_: Exception) {
    }

    currentPlayer.play()

    updateProgress()
    updatePauseButton()
    updateCenterPlayButton()
}

internal fun PlayerActivity.syncQueueCurrentVideo(
    uri: Uri
) {

    if (isExternalVideo()) {
        return
    }

    val queue =
        PlaybackQueueManager.getQueue(
            this
        )

    if (queue.isEmpty()) {
        return
    }

    val index =
        queue.indexOfFirst {
            it.toString() ==
                uri.toString()
        }

    if (index >= 0) {

        PlaybackQueueManager.setCurrentIndex(
            this,
            index
        )
    }
}

internal fun PlayerActivity.isCurrentVideoInQueue(): Boolean {

    if (isExternalVideo()) {
        return false
    }

    val currentUri =
        getIncomingVideoUri()
            ?: return false

    return PlaybackQueueManager
        .getQueue(this)
        .any {
            it.toString() ==
                currentUri.toString()
        }
}

internal fun PlayerActivity.getVideoUris(): List<Uri> {

    if (
        ContextCompat.checkSelfPermission(
            this,
            requiredVideoPermission()
        ) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return emptyList()
    }

    val result =
        mutableListOf<Uri>()

    val collection =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            MediaStore.Video.Media
                .getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )

        } else {

            MediaStore.Video.Media
                .EXTERNAL_CONTENT_URI
        }

    contentResolver.query(
        collection,
        arrayOf(
            MediaStore.Video.Media._ID
        ),
        null,
        null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )?.use { cursor ->

        val idColumn =
            cursor.getColumnIndexOrThrow(
                MediaStore.Video.Media._ID
            )

        while (cursor.moveToNext()) {

            result.add(
                Uri.withAppendedPath(
                    collection,
                    cursor.getLong(
                        idColumn
                    ).toString()
                )
            )
        }
    }

    return result
}

internal fun PlayerActivity.requiredVideoPermission(): String {

    return if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {

        Manifest.permission.READ_MEDIA_VIDEO

    } else {

        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

internal fun PlayerActivity.playPreviousVideo() {

    if (isExternalVideo()) {
        return
    }

    val currentUri =
        getIncomingVideoUri()

    if (
        currentUri != null &&
        isCurrentVideoInQueue()
    ) {

        val previousUri =
            PlaybackQueueManager.getPrevious(
                this,
                true
            )

        if (previousUri != null) {

            PlaybackQueueManager.setCurrentVideo(
                this,
                previousUri
            )

            openVideo(
                previousUri
            )

            showTemporaryMessage(
                p(
                    "ویدئوی قبلی صف",
                    "Previous video in queue"
                )
            )

            return
        }
    }

    val videos =
        getVideoUris()

    if (videos.isEmpty()) {
        return
    }

    val currentIndex =
        currentUri?.let { uri ->
            videos.indexOfFirst {
                it.toString() ==
                    uri.toString()
            }
        } ?: -1

    val previousIndex =
        if (currentIndex <= 0) {
            videos.lastIndex
        } else {
            currentIndex - 1
        }

    openVideo(
        videos[previousIndex]
    )
}

internal fun PlayerActivity.playNextVideo() {

    if (isExternalVideo()) {
        return
    }

    val currentUri =
        getIncomingVideoUri()

    if (
        currentUri != null &&
        isCurrentVideoInQueue()
    ) {

        val nextUri =
            PlaybackQueueManager.getNext(
                this,
                true
            )

        if (nextUri != null) {

            PlaybackQueueManager.setCurrentVideo(
                this,
                nextUri
            )

            openVideo(
                nextUri
            )

            showTemporaryMessage(
                p(
                    "ویدئوی بعدی صف",
                    "Next video in queue"
                )
            )

            return
        }
    }

    val videos =
        getVideoUris()

    if (videos.isEmpty()) {
        return
    }

    val currentIndex =
        currentUri?.let { uri ->
            videos.indexOfFirst {
                it.toString() ==
                    uri.toString()
            }
        } ?: -1

    val nextIndex =
        if (
            currentIndex < 0 ||
            currentIndex >=
                videos.lastIndex
        ) {

            0

        } else {

            currentIndex + 1
        }

    openVideo(
        videos[nextIndex]
    )
}

internal fun PlayerActivity.openVideo(
    uri: Uri
) {

    /*
     * قبل از تغییر URI، موقعیت ویدئوی فعلی ذخیره می‌شود.
     */
    savePosition()

    intent.action =
        Intent.ACTION_MAIN

    intent.data =
        null

    intent.putExtra(
        PlayerActivity.EXTRA_VIDEO_URI,
        uri.toString()
    )

    intent.putExtra(
        PlayerActivity.EXTRA_VIDEO_NAME,
        getVideoName(uri)
    )

    syncQueueCurrentVideo(uri)

    val savedSubtitle =
        SubtitleFileManager.getSubtitleUri(
            this,
            uri
        )

    if (savedSubtitle != null) {

        createPlayerWithSubtitle(
            uri,
            savedSubtitle
        )

    } else {

        createPlayer(
            MediaItem.fromUri(uri)
        )
    }

    showPlayerControlsTemporarily()
}

internal fun PlayerActivity.getVideoName(
    uri: Uri
): String {

    return contentResolver.query(
        uri,
        arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME
        ),
        null,
        null,
        null
    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            cursor.getString(0)
                ?: p(
                    "ویدئوی ناشناس",
                    "Unknown video"
                )

        } else {

            p(
                "ویدئوی ناشناس",
                "Unknown video"
            )
        }

    } ?: p(
        "ویدئوی ناشناس",
        "Unknown video"
    )
}

internal fun PlayerActivity.savePosition() {

    if (isExternalVideo()) {
        return
    }

    val uri =
        getIncomingVideoUri()
            ?: return

    val currentPlayer =
        player
            ?: return

    val position =
        currentPlayer.currentPosition

    val duration =
        currentPlayer.duration

    if (
        position <= 0L ||
        duration <= 0L ||
        duration == C.TIME_UNSET
    ) {
        return
    }

    PlaybackHistoryManager.save(
        this,
        uri,
        position,
        duration
    )
}

internal fun PlayerActivity.shareCurrentVideo() {

    val uri =
        getIncomingVideoUri()
            ?: return

    VideoShareManager.share(
        this,
        uri
    )
}

internal fun PlayerActivity.deleteCurrentVideo() {

    if (isExternalVideo()) {
        return
    }

    val uri =
        getIncomingVideoUri()
            ?: return

    AlertDialog.Builder(this)

        .setTitle(
            p(
                "حذف ویدئو",
                "Delete video"
            )
        )

        .setMessage(
            p(
                "آیا از حذف این ویدئو مطمئن هستید؟",
                "Are you sure you want to delete this video?"
            )
        )

        .setNegativeButton(
            p(
                "لغو",
                "Cancel"
            ),
            null
        )

        .setPositiveButton(
            p(
                "حذف",
                "Delete"
            )
        ) { _, _ ->

            deleteVideo(uri)
        }

        .show()
}

internal fun PlayerActivity.deleteVideo(
    uri: Uri
) {

    try {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {

            val pendingIntent =
                MediaStore.createDeleteRequest(
                    contentResolver,
                    listOf(uri)
                )

            startIntentSenderForResult(
                pendingIntent.intentSender,
                PlayerActivity.DELETE_REQUEST_CODE,
                null,
                0,
                0,
                0,
                null
            )

        } else {

            val deleted =
                contentResolver.delete(
                    uri,
                    null,
                    null
                )

            if (deleted > 0) {

                clearDeletedVideoData(
                    uri
                )

                showTemporaryMessage(
                    p(
                        "ویدئو حذف شد.",
                        "Video deleted."
                    )
                )

                exitPlayer()
            }
        }

    } catch (_: Exception) {

        showTemporaryMessage(
            p(
                "حذف ویدئو انجام نشد.",
                "Video deletion failed."
            )
        )
    }
}

internal fun PlayerActivity.clearDeletedVideoData(
    uri: Uri
) {

    PlaybackHistoryManager.clear(
        this,
        uri
    )

    SubtitleFileManager.removeSubtitle(
        this,
        uri
    )

    PlaybackQueueManager.remove(
        this,
        uri
    )

    VideoSpeedManager.clear(
        this,
        uri
    )
}

internal fun PlayerActivity.handleDeleteResult(
    requestCode: Int,
    resultCode: Int
) {

    if (
        requestCode ==
        PlayerActivity.DELETE_REQUEST_CODE &&
        resultCode ==
        android.app.Activity.RESULT_OK
    ) {

        val uri =
            getIncomingVideoUri()

        if (uri != null) {

            clearDeletedVideoData(
                uri
            )
        }

        exitPlayer()
    }
}
