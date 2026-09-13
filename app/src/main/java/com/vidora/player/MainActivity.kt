package com.vidora.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var videoContainer: LinearLayout
    private lateinit var countText: TextView
    private lateinit var searchInput: EditText
    private lateinit var sortButton: Button

    private val allVideos = mutableListOf<VideoItem>()

    private var sortMode = SortMode.NEWEST

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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        videoContainer =
            findViewById(R.id.videoContainer)

        countText =
            findViewById(R.id.countText)

        searchInput =
            findViewById(R.id.searchInput)

        sortButton =
            findViewById(R.id.sortButton)

        setupSearch()

        setupSortButton()

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

    private fun setupSearch() {

        searchInput.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterVideos(
                        s?.toString().orEmpty()
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun setupSortButton() {

        sortButton.setOnClickListener {

            sortMode =
                when (sortMode) {
                    SortMode.NEWEST ->
                        SortMode.NAME

                    SortMode.NAME ->
                        SortMode.SIZE

                    SortMode.SIZE ->
                        SortMode.NEWEST
                }

            updateSortButtonText()

            filterVideos(
                searchInput.text.toString()
            )
        }
    }

    private fun updateSortButtonText() {

        sortButton.text =
            when (sortMode) {

                SortMode.NEWEST ->
                    getString(R.string.sort_newest)

                SortMode.NAME ->
                    getString(R.string.sort_name)

                SortMode.SIZE ->
                    getString(R.string.sort_size)
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

        allVideos.clear()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )

            } else {

                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

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

            val dateColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DATE_ADDED
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

                val dateAdded =
                    cursor.getLong(dateColumn)

                val uri =
                    Uri.withAppendedPath(
                        collection,
                        id.toString()
                    )

                allVideos.add(
                    VideoItem(
                        uri = uri,
                        name = name,
                        duration = duration,
                        size = size,
                        dateAdded = dateAdded
                    )
                )
            }
        }

        filterVideos(
            searchInput.text.toString()
        )
    }

    private fun filterVideos(
        query: String
    ) {

        val normalizedQuery =
            query.trim().lowercase(Locale.getDefault())

        val filtered =
            if (normalizedQuery.isEmpty()) {

                allVideos.toList()

            } else {

                allVideos.filter {
                    it.name
                        .lowercase(Locale.getDefault())
                        .contains(normalizedQuery)
                }
            }

        val sorted =
            when (sortMode) {

                SortMode.NEWEST ->
                    filtered.sortedByDescending {
                        it.dateAdded
                    }

                SortMode.NAME ->
                    filtered.sortedBy {
                        it.name.lowercase(
                            Locale.getDefault()
                        )
                    }

                SortMode.SIZE ->
                    filtered.sortedByDescending {
                        it.size
                    }
            }

        displayVideos(sorted)
    }

    private fun displayVideos(
        videos: List<VideoItem>
    ) {

        videoContainer.removeAllViews()

        countText.text =
            videos.size.toString()

        if (videos.isEmpty()) {

            showMessage(
                if (
                    searchInput.text
                        .toString()
                        .trim()
                        .isEmpty()
                ) {
                    getString(
                        R.string.no_videos
                    )
                } else {
                    getString(
                        R.string.no_videos
                    )
                }
            )

            return
        }

        videos.forEach { video ->
            addVideoItem(video)
        }
    }

    private fun addVideoItem(
        video: VideoItem
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_video,
                    videoContainer,
                    false
                )

        val thumbnailImage =
            view.findViewById<ImageView>(
                R.id.thumbnailImage
            )

        val nameText =
            view.findViewById<TextView>(
                R.id.nameText
            )

        val infoText =
            view.findViewById<TextView>(
                R.id.infoText
            )

        nameText.text =
            video.name

        infoText.text =
            buildString {

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

        loadThumbnail(
            video.uri,
            thumbnailImage
        )

        view.setOnClickListener {

            val intent =
                Intent(
                    this,
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

        videoContainer.addView(view)
    }

    private fun loadThumbnail(
        uri: Uri,
        imageView: ImageView
    ) {

        try {

            val retriever =
                MediaMetadataRetriever()

            retriever.setDataSource(
                this,
                uri
            )

            val bitmap =
                retriever.getFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever
                        .OPTION_CLOSEST_SYNC
                )

            retriever.release()

            if (bitmap != null) {
                imageView.setImageBitmap(
                    bitmap
                )
            }

        } catch (_: Exception) {
            imageView.setImageResource(
                android.R.color.transparent
            )
        }
    }

    private fun showMessage(
        message: String
    ) {

        videoContainer.removeAllViews()

        val text =
            TextView(this).apply {

                this.text = message

                textSize = 16f

                gravity = android.view.Gravity.CENTER

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
        val size: Long,
        val dateAdded: Long
    )

    enum class SortMode {
        NEWEST,
        NAME,
        SIZE
    }
}
