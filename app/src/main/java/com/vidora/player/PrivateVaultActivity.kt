package com.vidora.player

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File

class PrivateVaultActivity :
    ComponentActivity() {

    private lateinit var folder: File

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        folder =
            File(
                filesDir,
                "private_videos"
            )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        showPassword()
    }

    private fun showPassword() {

        val preferences =
            getSharedPreferences(
                "vidora_private",
                MODE_PRIVATE
            )

        val hasPassword =
            preferences.contains(
                "password"
            )

        val input =
            android.widget.EditText(
                this
            ).apply {

                hint =
                    if (hasPassword) {
                        "رمز ورود"
                    } else {
                        "رمز جدید"
                    }

                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

        AlertDialog.Builder(this)
            .setTitle(
                if (hasPassword) {
                    "پوشه خصوصی"
                } else {
                    "ساخت رمز پوشه خصوصی"
                }
            )
            .setMessage(
                if (hasPassword) {
                    "رمز را وارد کنید."
                } else {
                    "یک رمز برای پوشه خصوصی انتخاب کنید."
                }
            )
            .setView(input)
            .setNegativeButton(
                "لغو",
                null
            )
            .setPositiveButton(
                "ورود"
            ) { _, _ ->

                val password =
                    input.text
                        .toString()

                if (password.length < 4) {

                    android.widget.Toast
                        .makeText(
                            this,
                            "رمز باید حداقل ۴ کاراکتر باشد.",
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()

                    return@setPositiveButton
                }

                if (!hasPassword) {

                    preferences
                        .edit()
                        .putString(
                            "password",
                            password
                        )
                        .apply()

                    openVault()

                } else {

                    val saved =
                        preferences
                            .getString(
                                "password",
                                ""
                            )

                    if (saved == password) {

                        openVault()

                    } else {

                        android.widget.Toast
                            .makeText(
                                this,
                                "رمز اشتباه است.",
                                android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
            .show()
    }

    private fun openVault() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )

                setBackgroundColor(
                    getColor(
                        R.color.vidora_background
                    )
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "🔒 پوشه خصوصی Vidora"

                textSize =
                    23f

                setTextColor(
                    getColor(
                        R.color.vidora_text
                    )
                )

                setPadding(
                    0,
                    20,
                    0,
                    20
                )
            }

        root.addView(title)

        val add =
            Button(this).apply {

                text =
                    "➕ افزودن ویدئو"

                isAllCaps =
                    false

                setOnClickListener {

                    startActivityForResult(
                        Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                        ).apply {

                            type =
                                "video/*"

                            addCategory(
                                Intent.CATEGORY_OPENABLE
                            )
                        },
                        REQUEST_PICK
                    )
                }
            }

        root.addView(add)

        val files =
            folder.listFiles()
                ?.sortedBy {
                    it.name.lowercase()
                }
                .orEmpty()

        files.forEach { file ->

            val button =
                Button(this).apply {

                    text =
                        "▶ ${file.name}"

                    isAllCaps =
                        false

                    setOnClickListener {

                        val uri =
                            android.net.Uri
                                .fromFile(file)

                        startActivity(
                            Intent(
                                this@PrivateVaultActivity,
                                PlayerActivity::class.java
                            ).apply {

                                putExtra(
                                    PlayerActivity.EXTRA_VIDEO_URI,
                                    uri.toString()
                                )

                                putExtra(
                                    PlayerActivity.EXTRA_VIDEO_NAME,
                                    file.name
                                )
                            }
                        )
                    }

                    setOnLongClickListener {

                        AlertDialog.Builder(
                            this@PrivateVaultActivity
                        )
                            .setTitle(
                                file.name
                            )
                            .setItems(
                                arrayOf(
                                    "حذف"
                                )
                            ) { _, _ ->

                                file.delete()

                                openVault()
                            }
                            .show()

                        true
                    }
                }

            root.addView(button)
        }

        setContentView(root)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode ==
            REQUEST_PICK &&
            resultCode ==
            RESULT_OK
        ) {

            val uri =
                data?.data
                    ?: return

            try {

                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->

                        val name =
                            "video_${System.currentTimeMillis()}.mp4"

                        val destination =
                            File(
                                folder,
                                name
                            )

                        destination
                            .outputStream()
                            .use { output ->

                                input.copyTo(
                                    output,
                                    1024 * 1024
                                )
                            }
                    }

                openVault()

            } catch (_: Exception) {

                android.widget.Toast
                    .makeText(
                        this,
                        "انتقال ویدئو انجام نشد.",
                        android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }
    }

    companion object {

        private const val REQUEST_PICK =
            9001
    }
}
