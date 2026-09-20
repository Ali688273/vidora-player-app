package com.vidora.player

import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout

import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.ui.TrackSelectionDialogBuilder

internal fun PlayerActivity.showMoreMenu() {

    val options =
        arrayOf(
            p(
                "تنظیمات سرعت و پخش",
                "Playback and speed settings"
            ),
            p(
                "همگام‌سازی صدا",
                "Audio synchronization"
            ),
            p(
                "همگام‌سازی زیرنویس",
                "Subtitle synchronization"
            ),
            p(
                "تنظیمات تصویر",
                "Video settings"
            ),
            p(
                "افزودن به صف پخش",
                "Add to playback queue"
            ),
            p(
                "نمایش صف پخش",
                "Show playback queue"
            ),
            p(
                "پاک کردن صف پخش",
                "Clear playback queue"
            )
        )

    AlertDialog.Builder(this)
        .setTitle(
            p(
                "امکانات بیشتر",
                "More options"
            )
        )
        .setItems(options) { _, which ->

            when (which) {

                0 ->
                    showPlaybackSettings()

                1 ->
                    showAudioSyncDialog()

                2 ->
                    showSubtitleSyncDialog()

                3 ->
                    showVideoQualityDialog()

                4 ->
                    addCurrentToQueue()

                5 ->
                    showQueue()

                6 -> {

                    PlaybackQueueManager.clear(
                        this
                    )

                    showTemporaryMessage(
                        p(
                            "صف پخش پاک شد.",
                            "Playback queue cleared."
                        )
                    )
                }
            }
        }
        .show()
}

internal fun PlayerActivity.showPlaybackSettings() {

    val currentAutoPlayNext =
        PlaybackSettings.autoPlayNext(this)

    val currentAutoResume =
        VidoraSettings.autoResume(this)

    AlertDialog.Builder(this)
        .setTitle(
            p(
                "تنظیمات پخش",
                "Playback settings"
            )
        )
        .setMultiChoiceItems(
            arrayOf(
                p(
                    "پخش خودکار ویدئوی بعدی",
                    "Autoplay next video"
                ),
                p(
                    "ادامه پخش از آخرین موقعیت",
                    "Resume from last position"
                )
            ),
            booleanArrayOf(
                currentAutoPlayNext,
                currentAutoResume
            )
        ) { _, which, checked ->

            when (which) {

                0 ->
                    PlaybackSettings.setAutoPlayNext(
                        this,
                        checked
                    )

                1 ->
                    VidoraSettings.setAutoResume(
                        this,
                        checked
                    )
            }
        }
        .setPositiveButton(
            p(
                "باشه",
                "OK"
            ),
            null
        )
        .show()
}

internal fun PlayerActivity.showAudioSyncDialog() {

    val uri =
        getIncomingVideoUri()
            ?: return

    val current =
        AudioSyncManager.getOffset(
            this,
            uri
        )

    val input =
        EditText(this).apply {

            inputType =
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_SIGNED

            setText(
                current.toString()
            )

            hint =
                p(
                    "میلی‌ثانیه",
                    "Milliseconds"
                )
        }

    AlertDialog.Builder(this)
        .setTitle(
            p(
                "همگام‌سازی صدا",
                "Audio synchronization"
            )
        )
        .setMessage(
            p(
                "مقدار مثبت یعنی صدا جلوتر تنظیم شود.",
                "A positive value shifts the audio forward."
            )
        )
        .setView(input)
        .setNegativeButton(
            p(
                "لغو",
                "Cancel"
            ),
            null
        )
        .setNeutralButton(
            p(
                "صفر",
                "Reset"
            )
        ) { _, _ ->

            AudioSyncManager.reset(
                this,
                uri
            )
        }
        .setPositiveButton(
            p(
                "ذخیره",
                "Save"
            )
        ) { _, _ ->

            val value =
                input.text
                    .toString()
                    .toLongOrNull()
                    ?: 0L

            AudioSyncManager.setOffset(
                this,
                uri,
                value
            )

            showTemporaryMessage(
                p(
                    "تنظیم همگام‌سازی ذخیره شد.",
                    "Audio synchronization saved."
                )
            )
        }
        .show()
}

internal fun PlayerActivity.showSubtitleSyncDialog() {

    val uri =
        getIncomingVideoUri()
            ?: return

    val current =
        SubtitleSyncManager.getOffset(
            this,
            uri
        )

    val input =
        EditText(this).apply {

            inputType =
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_SIGNED

            setText(
                current.toString()
            )

            hint =
                p(
                    "میلی‌ثانیه",
                    "Milliseconds"
                )
        }

    AlertDialog.Builder(this)
        .setTitle(
            p(
                "همگام‌سازی زیرنویس",
                "Subtitle synchronization"
            )
        )
        .setMessage(
            p(
                "مقدار مثبت یعنی زیرنویس دیرتر نمایش داده شود.",
                "A positive value delays the subtitle."
            )
        )
        .setView(input)
        .setNegativeButton(
            p(
                "لغو",
                "Cancel"
            ),
            null
        )
        .setNeutralButton(
            p(
                "صفر",
                "Reset"
            )
        ) { _, _ ->

            SubtitleSyncManager.reset(
                this,
                uri
            )
        }
        .setPositiveButton(
            p(
                "ذخیره",
                "Save"
            )
        ) { _, _ ->

            val value =
                input.text
                    .toString()
                    .toLongOrNull()
                    ?: 0L

            SubtitleSyncManager.setOffset(
                this,
                uri,
                value
            )

            showTemporaryMessage(
                p(
                    "تنظیم زیرنویس ذخیره شد.",
                    "Subtitle synchronization saved."
                )
            )
        }
        .show()
}

internal fun PlayerActivity.showVideoQualityDialog() {

    val currentPlayer =
        player

    if (currentPlayer == null) {

        showTemporaryMessage(
            p(
                "پخش‌کننده هنوز آماده نیست.",
                "The player is not ready yet."
            )
        )

        return
    }

    val videoGroups =
        currentPlayer.currentTracks.groups
            .filter {
                it.type ==
                    C.TRACK_TYPE_VIDEO
            }

    if (videoGroups.isEmpty()) {

        AlertDialog.Builder(this)
            .setTitle(
                p(
                    "کیفیت تصویر",
                    "Video quality"
                )
            )
            .setMessage(
                p(
                    "برای این ویدئو کیفیت‌های جداگانه قابل انتخاب نیست.",
                    "Separate video quality options are not available."
                )
            )
            .setPositiveButton(
                p(
                    "باشه",
                    "OK"
                ),
                null
            )
            .show()

        return
    }

    try {

        TrackSelectionDialogBuilder(
            this,
            p(
                "انتخاب کیفیت ویدئو",
                "Select video quality"
            ),
            currentPlayer,
            C.TRACK_TYPE_VIDEO
        )
            .setAllowAdaptiveSelections(true)
            .setShowDisableOption(false)
            .build()
            .show()

    } catch (_: Exception) {

        showTemporaryMessage(
            p(
                "انتخاب کیفیت برای این ویدئو در دسترس نیست.",
                "Quality selection is not available for this video."
            )
        )
    }
}

internal fun PlayerActivity.addCurrentToQueue() {

    val uri =
        getIncomingVideoUri()
            ?: return

    val added =
        PlaybackQueueManager.addAndSetCurrent(
            this,
            uri
        )

    showTemporaryMessage(
        if (added) {
            p(
                "ویدئو به صف پخش اضافه شد.",
                "Video added to playback queue."
            )
        } else {
            p(
                "ویدئو از قبل در صف پخش بود.",
                "Video is already in the playback queue."
            )
        }
    )
}

internal fun PlayerActivity.showQueue() {

    val queue =
        PlaybackQueueManager.getQueue(this)

    if (queue.isEmpty()) {

        AlertDialog.Builder(this)
            .setTitle(
                p(
                    "صف پخش",
                    "Playback queue"
                )
            )
            .setMessage(
                p(
                    "صف پخش خالی است.",
                    "The playback queue is empty."
                )
            )
            .setPositiveButton(
                p(
                    "باشه",
                    "OK"
                ),
                null
            )
            .show()

        return
    }

    val currentUri =
        getIncomingVideoUri()

    val names =
        queue.mapIndexed { index, uri ->

            val marker =
                if (
                    currentUri != null &&
                    currentUri.toString() ==
                    uri.toString()
                ) {
                    " ▶ "
                } else {
                    ""
                }

            "${index + 1}.$marker${getVideoName(uri)}"

        }.toTypedArray()

    AlertDialog.Builder(this)
        .setTitle(
            p(
                "صف پخش",
                "Playback queue"
            )
        )
        .setItems(names) { _, which ->

            if (which !in queue.indices) {
                return@setItems
            }

            val selectedUri =
                queue[which]

            PlaybackQueueManager.setCurrentIndex(
                this,
                which
            )

            openVideo(selectedUri)

            showTemporaryMessage(
                p(
                    "در حال پخش از صف",
                    "Playing from queue"
                )
            )
        }
        .setNegativeButton(
            p(
                "بستن",
                "Close"
            ),
            null
        )
        .show()
}

internal fun PlayerActivity.showAudioTrackDialog() {

    val currentPlayer =
        player ?: return

    val audioGroups =
        currentPlayer.currentTracks.groups
            .filter {
                it.type ==
                    C.TRACK_TYPE_AUDIO
            }

    if (audioGroups.isEmpty()) {

        AlertDialog.Builder(this)
            .setTitle(
                p(
                    "صدا",
                    "Audio"
                )
            )
            .setMessage(
                p(
                    "هیچ ترک صوتی جداگانه‌ای وجود ندارد.",
                    "No separate audio tracks are available."
                )
            )
            .setPositiveButton(
                p(
                    "باشه",
                    "OK"
                ),
                null
            )
            .show()

        return
    }

    TrackSelectionDialogBuilder(
        this,
        p(
            "ترک صوتی",
            "Audio track"
        ),
        currentPlayer,
        C.TRACK_TYPE_AUDIO
    )
        .setAllowAdaptiveSelections(false)
        .build()
        .show()
}

internal fun PlayerActivity.showSleepTimerDialog() {

    val input =
        EditText(this)

    input.inputType =
        InputType.TYPE_CLASS_NUMBER

    input.hint =
        p(
            "دقیقه",
            "Minutes"
        )

    input.setSingleLine(true)

    val container =
        LinearLayout(this)

    container.orientation =
        LinearLayout.VERTICAL

    val padding =
        (
            24 *
                resources.displayMetrics.density
            ).toInt()

    container.setPadding(
        padding,
        0,
        padding,
        0
    )

    container.addView(input)

    val dialog =
        AlertDialog.Builder(this)
            .setTitle(
                p(
                    "زمان‌سنج خواب",
                    "Sleep timer"
                )
            )
            .setView(container)
            .setPositiveButton(
                p(
                    "شروع",
                    "Start"
                ),
                null
            )
            .setNegativeButton(
                p(
                    "لغو",
                    "Cancel"
                ),
                null
            )
            .setNeutralButton(
                p(
                    "لغو زمان‌سنج",
                    "Cancel timer"
                ),
                null
            )
            .create()

    dialog.setOnShowListener {

        dialog.getButton(
            AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener {

            val minutes =
                input.text
                    .toString()
                    .trim()
                    .toLongOrNull()

            if (
                minutes == null ||
                minutes <= 0L
            ) {

                input.error =
                    p(
                        "زمان نامعتبر است.",
                        "Invalid time."
                    )

                return@setOnClickListener
            }

            startSleepTimer(minutes)
            dialog.dismiss()
        }

        dialog.getButton(
            AlertDialog.BUTTON_NEUTRAL
        ).setOnClickListener {

            cancelSleepTimer()
            dialog.dismiss()
        }
    }

    dialog.show()
}

internal fun PlayerActivity.startSleepTimer(
    minutes: Long
) {

    cancelSleepTimer()

    val delayMillis =
        minutes
            .coerceAtMost(
                Long.MAX_VALUE / 60000L
            )
            .times(60000L)

    sleepTimerRunnable =
        Runnable {

            player?.pause()

            updatePauseButton()
            updateCenterPlayButton()

            sleepTimerButton.text =
                p(
                    "خواب",
                    "Sleep"
                )

            sleepTimerRunnable = null
        }

    sleepTimerButton.text =
        if (isPersian()) {
            "خواب: $minutes دقیقه"
        } else {
            "Sleep: $minutes min"
        }

    sleepHandler.postDelayed(
        sleepTimerRunnable!!,
        delayMillis
    )
}

internal fun PlayerActivity.cancelSleepTimer() {

    sleepTimerRunnable?.let {
        sleepHandler.removeCallbacks(it)
    }

    sleepTimerRunnable = null

    if (::sleepTimerButton.isInitialized) {

        sleepTimerButton.text =
            p(
                "خواب",
                "Sleep"
            )
    }
}
