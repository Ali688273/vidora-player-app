package com.vidora.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var videoContainer: LinearLayout
    private lateinit var countText: TextView

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadVideos()
            } else {
                showMessage(
                    getString(R.string.permission_denied)
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        videoContainer =
            findViewById(R.id.videoContainer)

        countText =
            findViewById(R.id.countText)

        checkPermissionAndLoad()
    }

    override fun onResume() {
        super.onResume()

        if (::videoContainer.isInitialized &&
            hasVideoPermission()
        ) {
            loadVideos()
        }
    }

    private fun checkPermissionAndLoad() {

        if (hasVideoPermission()) {
            loadVideos()
        } else {
            permissionLauncher.launch(
                requiredPermission()
            )
        }
    }

    private fun requiredPermission(): String {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun hasVideoPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            requiredPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadVideos() {

        videoContainer.removeAllViews()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )

            } else {

                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        val videos = mutableListOf<VideoItem>()

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media._ID
                )

            val nameColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DISPLAY_NAME
                )

            val durationColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DURATION
                )

            val sizeColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.SIZE
                )

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(idColumn)

                val name =
                    cursor.getString(nameColumn)

                val duration =
                    cursor.getLong(durationColumn)

                val size =
                    cursor.getLong(sizeColumn)

                val uri =
                    Uri.withAppendedPath(
                        collection,
                        id.toString()
                    )

                videos.add(
                    VideoItem(
                        uri = uri,
                        name = name,
                        duration = duration,
                        size = size
                    )
                )
            }
        }

        countText.text = videos.size.toString()

        if (videos.isEmpty()) {

            showMessage(
                getString(R.string.no_videos)
            )

            return
        }

        videos.forEach { video ->
            addVideoItem(video)
        }
    }

    private fun addVideoItem(video: VideoItem) {

        val item = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_VERTICAL

            setPadding(
                18,
                18,
                18,
                18
            )

            setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.vidora_surface
                )
            )

            setOnClickListener {

                val intent =
                    Intent(
                        this@MainActivity,
                        PlayerActivity::class.java
                    )

                intent.putExtra(
                    PlayerActivity.EXTRA_VIDEO_URI,
                    video.uri.toString()
                )

                intent.putExtra(
                    PlayerActivity.EXTRA_VIDEO_NAME,
                    video.name
                )

                startActivity(intent)
            }
        }

        val nameText =
            TextView(this).apply {

                text = video.name

                textSize = 17f

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.vidora_text
                    )
                )

                maxLines = 2
            }

        val infoText =
            TextView(this).apply {

                text = buildString {

                    append(
                        formatDuration(
                            video.duration
                        )
                    )

                    append("  •  ")

                    append(
                        formatSize(
                            video.size
                        )
                    )
                }

                textSize = 13f

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.vidora_text_secondary
                    )
                )

                maxLines = 1
            }

        item.addView(nameText)

        item.addView(infoText)

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.bottomMargin = 8

        videoContainer.addView(
            item,
            params
        )
    }

    private fun showMessage(message: String) {

        videoContainer.removeAllViews()

        val text =
            TextView(this).apply {

                this.text = message

                textSize = 16f

                gravity = Gravity.CENTER

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.vidora_text_secondary
                    )
                )

                setPadding(
                    32,
                    80,
                    32,
                    80
                )
            }

        videoContainer.addView(text)

        countText.text = "0"
    }

    private fun formatDuration(
        milliseconds: Long
    ): String {

        if (milliseconds <= 0L) {
            return "--:--"
        }

        val totalSeconds =
            milliseconds / 1000

        val seconds =
            totalSeconds % 60

        val minutes =
            (totalSeconds / 60) % 60

        val hours =
            totalSeconds / 3600

        return if (hours > 0) {

            String.format(
                Locale.US,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )

        } else {

            String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }

    private fun formatSize(
        bytes: Long
    ): String {

        if (bytes <= 0L) {
            return "0 MB"
        }

        val megabytes =
            bytes / 1024.0 / 1024.0

        return if (megabytes >= 1024.0) {

            String.format(
                Locale.US,
                "%.1f GB",
                megabytes / 1024.0
            )

        } else {

            String.format(
                Locale.US,
                "%.0f MB",
                megabytes
            )
        }
    }

    data class VideoItem(
        val uri: Uri,
        val name: String,
        val duration: Long,
        val size: Long
    )
}
