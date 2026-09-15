package com.vidora.player

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.setPadding

class SettingsActivity : ComponentActivity() {

    private lateinit var root:
        LinearLayout

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        buildUi()
    }

    private fun buildUi() {

        root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(28)

                setBackgroundColor(
                    getColor(
                        R.color.vidora_background
                    )
                )
            }

        val scroll =
            android.widget.ScrollView(
                this
            )

        scroll.addView(
            root
        )

        setContentView(
            scroll
        )

        addTitle(
            "تنظیمات Vidora Player"
        )

        addSection(
            "ظاهر"
        )

        addSwitch(
            "حالت تاریک",
            VidoraSettings.isDarkMode(this)
        ) { checked ->

            VidoraSettings.setDarkMode(
                this,
                checked
            )
        }

        addSwitch(
            "نمایش ویدئوهای مخفی",
            VidoraSettings.showHidden(this)
        ) { checked ->

            VidoraSettings.setShowHidden(
                this,
                checked
            )
        }

        addSection(
            "پخش"
        )

        addSwitch(
            "ادامه پخش از آخرین موقعیت",
            VidoraSettings.autoResume(this)
        ) { checked ->

            VidoraSettings.setAutoResume(
                this,
                checked
            )
        }

        addSwitch(
            "پخش خودکار ویدئوی بعدی",
            PlaybackSettings.autoPlayNext(this)
        ) { checked ->

            PlaybackSettings.setAutoPlayNext(
                this,
                checked
            )
        }

        addDefaultSpeed()

        addSection(
            "زیرنویس"
        )

        addSubtitleSize()

        addSwitch(
            "نادیده گرفتن اندازه داخلی زیرنویس",
            true
        ) {
            getSharedPreferences(
                "vidora_player_preferences",
                MODE_PRIVATE
            )
                .edit()
                .putBoolean(
                    "subtitle_ignore_embedded",
                    it
                )
                .apply()
        }

        addSection(
            "زبان"
        )

        addLanguageSwitch()

        addSection(
            "امکانات"
        )

        addButton(
            "🌐 پخش ویدئوی آنلاین"
        ) {

            startActivity(
                Intent(
                    this,
                    OnlineVideoActivity::class.java
                )
            )
        }

        addButton(
            "🔒 پوشه خصوصی"
        ) {

            startActivity(
                Intent(
                    this,
                    PrivateVaultActivity::class.java
                )
            )
        }

        addButton(
            "📂 انتخاب پوشه رسانه"
        ) {

            startActivity(
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT_TREE
                ).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                }
            )
        }

        addButton(
            "📋 مدیریت پلی‌لیست‌ها"
        ) {
            showPlaylists()
        }

        addButton(
            "⚙️ تنظیمات دسترسی سیستم"
        ) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_ACCESSIBILITY_SETTINGS
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun addTitle(
        text: String
    ) {

        val title =
            TextView(this).apply {

                this.text =
                    text

                textSize =
                    25f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    20,
                    0,
                    28
                )
            }

        root.addView(
            title
        )
    }

    private fun addSection(
        text: String
    ) {

        val view =
            TextView(this).apply {

                this.text =
                    text

                textSize =
                    19f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    26,
                    0,
                    12
                )
            }

        root.addView(
            view
        )
    }

    private fun addSwitch(
        text: String,
        checked: Boolean,
        listener: (Boolean) -> Unit
    ) {

        val switch =
            Switch(this).apply {

                this.text =
                    text

                textSize =
                    16f

                isChecked =
                    checked

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    10,
                    0,
                    10
                )

                setOnCheckedChangeListener {
                        _,
                        value ->
                    listener(
                        value
                    )
                }
            }

        root.addView(
            switch
        )
    }

    private fun addDefaultSpeed() {

        val label =
            TextView(this).apply {

                text =
                    "سرعت پیش‌فرض"

                textSize =
                    16f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )
            }

        root.addView(
            label
        )

        val seek =
            SeekBar(this).apply {

                max =
                    40

                progress =
                    (
                        PlaybackSettings
                            .getDefaultSpeed(
                                this@SettingsActivity
                            ) * 10f
                        ).toInt()
                        .coerceIn(
                            5,
                            50
                        )

                setOnSeekBarChangeListener(
                    object :
                        SeekBar.OnSeekBarChangeListener {

                        override fun onProgressChanged(
                            bar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {

                            if (!fromUser) {
                                return
                            }

                            val speed =
                                (
                                    progress
                                        .coerceIn(
                                            5,
                                            40
                                        )
                                    ) / 10f

                            PlaybackSettings
                                .setDefaultSpeed(
                                    this@SettingsActivity,
                                    speed
                                )
                        }

                        override fun onStartTrackingTouch(
                            bar: SeekBar?
                        ) {
                        }

                        override fun onStopTrackingTouch(
                            bar: SeekBar?
                        ) {
                        }
                    }
                )
            }

        root.addView(
            seek
        )
    }

    private fun addSubtitleSize() {

        val label =
            TextView(this).apply {

                text =
                    "اندازه زیرنویس"

                textSize =
                    16f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )
            }

        root.addView(
            label
        )

        val seek =
            SeekBar(this).apply {

                max =
                    80

                progress =
                    (
                        getSharedPreferences(
                            "vidora_player_preferences",
                            MODE_PRIVATE
                        )
                            .getFloat(
                                "subtitle_size",
                                0.0533f
                            ) * 1000f
                        ).toInt()
                        .coerceIn(
                            20,
                            100
                        )

                setOnSeekBarChangeListener(
                    object :
                        SeekBar.OnSeekBarChangeListener {

                        override fun onProgressChanged(
                            bar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {

                            if (!fromUser) {
                                return
                            }

                            val value =
                                (
                                    progress
                                        .coerceIn(
                                            20,
                                            100
                                        )
                                    / 1000f
                                    )

                            getSharedPreferences(
                                "vidora_player_preferences",
                                MODE_PRIVATE
                            )
                                .edit()
                                .putFloat(
                                    "subtitle_size",
                                    value
                                )
                                .apply()
                        }

                        override fun onStartTrackingTouch(
                            bar: SeekBar?
                        ) {
                        }

                        override fun onStopTrackingTouch(
                            bar: SeekBar?
                        ) {
                        }
                    }
                )
            }

        root.addView(
            seek
        )
    }

    private fun addLanguageSwitch() {

        val languageSwitch =
            Switch(this).apply {

                text =
                    "English"

                textSize =
                    16f

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
    }

    private fun addButton(
        text: String,
        action: () -> Unit
    ) {

        val button =
            Button(this).apply {

                this.text =
                    text

                textSize =
                    15f

                isAllCaps =
                    false

                minHeight =
                    52

                setOnClickListener {
                    action()
                }
            }

        root.addView(
            button
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
