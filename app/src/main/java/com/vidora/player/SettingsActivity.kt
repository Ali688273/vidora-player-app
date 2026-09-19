package com.vidora.player

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.setPadding

class SettingsActivity : ComponentActivity() {

    private lateinit var root: LinearLayout

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
            ScrollView(this)

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

            PlaybackSettings.setResumePlayback(
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

        addSwitch(
            "پخش در پس‌زمینه",
            PlaybackSettings.backgroundPlayback(this)
        ) { checked ->

            PlaybackSettings.setBackgroundPlayback(
                this,
                checked
            )
        }

        addSwitch(
            "روشن ماندن صفحه هنگام پخش",
            PlaybackSettings.keepScreenOn(this)
        ) { checked ->

            PlaybackSettings.setKeepScreenOn(
                this,
                checked
            )
        }

        addSwitch(
            "کنترل‌های حرکتی",
            PlaybackSettings.gestureControls(this)
        ) { checked ->

            PlaybackSettings.setGestureControls(
                this,
                checked
            )
        }

        addDefaultSpeed()

        addSection(
            "نسبت تصویر"
        )

        addAspectOptions()

        addSection(
            "زیرنویس"
        )

        addSubtitleSize()

        addSwitch(
            "نادیده گرفتن اندازه داخلی زیرنویس",
            getSharedPreferences(
                "vidora_player_preferences",
                MODE_PRIVATE
            ).getBoolean(
                "subtitle_ignore_embedded",
                true
            )
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

        addSwitch(
            "پس‌زمینه زیرنویس",
            SubtitleSettings.useBackground(this)
        ) { checked ->

            SubtitleSettings.setUseBackground(
                this,
                checked
            )
        }

        addSwitch(
            "زیرنویس ضخیم",
            SubtitleSettings.bold(this)
        ) { checked ->

            SubtitleSettings.setBold(
                this,
                checked
            )
        }

        addSubtitleDelay()

        addSection(
            "زبان برنامه"
        )

        addLanguageSelector()

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

            } catch (
                _: Exception
            ) {
            }
        }

        addButton(
            "🔄 بازگردانی تنظیمات"
        ) {

            resetSettings()
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

                gravity =
                    Gravity.RIGHT

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

                gravity =
                    Gravity.RIGHT

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

        val valueText =
            TextView(this).apply {

                textSize =
                    15f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )
            }

        root.addView(
            valueText
        )

        val seek =
            SeekBar(this).apply {

                max =
                    19

                val current =
                    PlaybackSettings
                        .getDefaultSpeed(
                            this@SettingsActivity
                        )

                progress =
                    (
                        (current - 0.25f) /
                            0.25f
                        )
                            .toInt()
                            .coerceIn(
                                0,
                                19
                            )

                valueText.text =
                    "سرعت: %.2fx"
                        .format(
                            current
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
                                    0.25f +
                                        progress *
                                        0.25f
                                    )
                                    .coerceIn(
                                        0.25f,
                                        5.0f
                                    )

                            PlaybackSettings
                                .setDefaultSpeed(
                                    this@SettingsActivity,
                                    speed
                                )

                            valueText.text =
                                "سرعت: %.2fx"
                                    .format(
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

    private fun addAspectOptions() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER
            }

        val aspects =
            listOf(
                "تطبیق" to 0,
                "کامل" to 1,
                "16:9" to 2,
                "4:3" to 3
            )

        aspects.forEach { item ->

            val button =
                Button(this).apply {

                    text =
                        item.first

                    isAllCaps =
                        false

                    setOnClickListener {

                        PlaybackSettings
                            .setDefaultAspect(
                                this@SettingsActivity,
                                item.second
                            )

                        updateAspectButtons(
                            container
                        )
                    }
                }

            container.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }

        root.addView(
            container
        )

        updateAspectButtons(
            container
        )
    }

    private fun updateAspectButtons(
        container: LinearLayout
    ) {

        val selected =
            PlaybackSettings
                .getDefaultAspect(
                    this
                )

        for (
            index in
            0 until container.childCount
        ) {

            val button =
                container.getChildAt(
                    index
                ) as? Button
                    ?: continue

            button.alpha =
                if (
                    index == selected
                ) {
                    1f
                } else {
                    0.65f
                }
        }
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

        val valueText =
            TextView(this).apply {

                textSize =
                    15f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )
            }

        root.addView(
            valueText
        )

        val seek =
            SeekBar(this).apply {

                max =
                    19

                val current =
                    SubtitleSettings
                        .getSize(
                            this@SettingsActivity
                        )

                progress =
                    (
                        (
                            current -
                                0.025f
                            ) /
                            0.005f
                        )
                            .toInt()
                            .coerceIn(
                                0,
                                19
                            )

                valueText.text =
                    "اندازه: %.1f%%"
                        .format(
                            current * 100f
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
                                    0.025f +
                                        progress *
                                        0.005f
                                    )
                                    .coerceIn(
                                        0.025f,
                                        0.12f
                                    )

                            SubtitleSettings
                                .setSize(
                                    this@SettingsActivity,
                                    value
                                )

                            PlaybackSettings
                                .setSubtitleSize(
                                    this@SettingsActivity,
                                    value
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

                            valueText.text =
                                "اندازه: %.1f%%"
                                    .format(
                                        value * 100f
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

    private fun addSubtitleDelay() {

        val label =
            TextView(this).apply {

                text =
                    "تأخیر زیرنویس برای ویدئوی فعلی"

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

        val value =
            TextView(this).apply {

                text =
                    "برای تغییر تأخیر، از کنترل زیرنویس داخل پخش‌کننده استفاده کنید."

                textSize =
                    14f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    8,
                    0,
                    8
                )
            }

        root.addView(
            value
        )
    }

    private fun addLanguageSelector() {

        val currentLanguage =
            VidoraLanguageManager
                .getSelectedLanguage(
                    this
                )

        val currentName =
            VidoraLanguageManager
                .languageName(
                    this
                )

        val languageButton =
            Button(this).apply {

                text =
                    "زبان: $currentName"

                textSize =
                    16f

                isAllCaps =
                    false

                setOnClickListener {

                    showLanguageDialog(
                        this
                    )
                }
            }

        root.addView(
            languageButton
        )
    }

    private fun showLanguageDialog(
        button: Button
    ) {

        val languages =
            arrayOf(
                "خودکار — زبان گوشی",
                "فارسی",
                "English"
            )

        val selected =
            when (
                VidoraLanguageManager
                    .getSelectedLanguage(
                        this
                    )
            ) {

                VidoraLanguageManager.FA ->
                    1

                VidoraLanguageManager.EN ->
                    2

                else ->
                    0
            }

        android.app.AlertDialog.Builder(
            this
        )
            .setTitle(
                "زبان برنامه"
            )
            .setSingleChoiceItems(
                languages,
                selected
            ) { dialog, which ->

                when (which) {

                    0 -> {

                        VidoraLanguageManager
                            .useAutomaticLanguage(
                                this
                            )
                    }

                    1 -> {

                        VidoraLanguageManager
                            .setLanguage(
                                this,
                                VidoraLanguageManager.FA
                            )
                    }

                    2 -> {

                        VidoraLanguageManager
                            .setLanguage(
                                this,
                                VidoraLanguageManager.EN
                            )
                    }
                }

                dialog.dismiss()

                recreate()
            }
            .setNegativeButton(
                "لغو",
                null
            )
            .show()
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
            if (
                playlists.isEmpty()
            ) {

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

    private fun resetSettings() {

        PlaybackSettings
            .setDefaultSpeed(
                this,
                1.0f
            )

        PlaybackSettings
            .setDefaultAspect(
                this,
                0
            )

        PlaybackSettings
            .setAutoPlayNext(
                this,
                true
            )

        PlaybackSettings
            .setResumePlayback(
                this,
                true
            )

        PlaybackSettings
            .setBackgroundPlayback(
                this,
                true
            )

        PlaybackSettings
            .setKeepScreenOn(
                this,
                true
            )

        PlaybackSettings
            .setGestureControls(
                this,
                true
            )

        PlaybackSettings
            .setSubtitleSize(
                this,
                0.0533f
            )

        SubtitleSettings
            .setSize(
                this,
                0.0533f
            )

        SubtitleSettings
            .setUseBackground(
                this,
                true
            )

        SubtitleSettings
            .setBold(
                this,
                false
            )

        getSharedPreferences(
            "vidora_player_preferences",
            MODE_PRIVATE
        )
            .edit()
            .putFloat(
                "subtitle_size",
                0.0533f
            )
            .putBoolean(
                "subtitle_ignore_embedded",
                true
            )
            .apply()

        recreate()
    }
}
