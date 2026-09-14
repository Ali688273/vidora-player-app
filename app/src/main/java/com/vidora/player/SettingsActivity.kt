package com.vidora.player

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.setPadding

class SettingsActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(32)

                setBackgroundColor(
                    getColor(
                        R.color.vidora_background
                    )
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "تنظیمات Vidora Player"

                textSize = 25f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    20,
                    0,
                    30
                )
            }

        root.addView(title)

        val darkMode =
            Switch(this).apply {

                text =
                    "حالت تاریک"

                textSize = 17f

                isChecked =
                    VidoraSettings.isDarkMode(
                        this@SettingsActivity
                    )

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setOnCheckedChangeListener {
                        _,
                        checked ->

                    VidoraSettings.setDarkMode(
                        this@SettingsActivity,
                        checked
                    )
                }
            }

        root.addView(
            darkMode
        )

        val resume =
            Switch(this).apply {

                text =
                    "ادامه پخش از آخرین موقعیت"

                textSize = 17f

                isChecked =
                    VidoraSettings.autoResume(
                        this@SettingsActivity
                    )

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setOnCheckedChangeListener {
                        _,
                        checked ->

                    VidoraSettings.setAutoResume(
                        this@SettingsActivity,
                        checked
                    )
                }
            }

        root.addView(
            resume
        )

        val hidden =
            Switch(this).apply {

                text =
                    "نمایش ویدئوهای مخفی"

                textSize = 17f

                isChecked =
                    VidoraSettings.showHidden(
                        this@SettingsActivity
                    )

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setOnCheckedChangeListener {
                        _,
                        checked ->

                    VidoraSettings.setShowHidden(
                        this@SettingsActivity,
                        checked
                    )
                }
            }

        root.addView(
            hidden
        )

        val languageTitle =
            TextView(this).apply {

                text =
                    "زبان برنامه"

                textSize = 18f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                gravity =
                    Gravity.START

                setPadding(
                    0,
                    35,
                    0,
                    12
                )
            }

        root.addView(
            languageTitle
        )

        val languageSwitch =
            Switch(this).apply {

                text =
                    "English"

                textSize = 17f

                isChecked =
                    VidoraSettings.getLanguage(
                        this@SettingsActivity
                    ) == "en"

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setOnCheckedChangeListener {
                        _,
                        checked ->

                    VidoraSettings.setLanguage(
                        this@SettingsActivity,
                        if (checked) {
                            "en"
                        } else {
                            "fa"
                        }
                    )
                }
            }

        root.addView(
            languageSwitch
        )

        val playlistsButton =
            TextView(this).apply {

                text =
                    "مدیریت پلی‌لیست‌ها"

                textSize = 18f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    35,
                    0,
                    25
                )

                setOnClickListener {

                    showPlaylists()
                }
            }

        root.addView(
            playlistsButton
        )

        setContentView(
            root
        )
    }

    private fun showPlaylists() {

        val playlists =
            PlaylistManager.getPlaylists(
                this
            )

        val message =
            if (playlists.isEmpty()) {
                "هنوز پلی‌لیستی ساخته نشده است."
            } else {
                playlists.joinToString(
                    separator = "\n"
                ) {
                    "• ${it.name} (${it.videos.size} ویدئو)"
                }
            }

        android.app.AlertDialog.Builder(
            this
        )
            .setTitle(
                "پلی‌لیست‌ها"
            )
            .setMessage(
                message
            )
            .setPositiveButton(
                "باشه",
                null
            )
            .show()
    }
}
