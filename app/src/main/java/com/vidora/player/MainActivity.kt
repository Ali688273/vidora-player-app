package com.vidora.player

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
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

    private val allVideos =
        mutableListOf<VideoItem>()

    private var sortMode =
        SortMode.LAST_ACCESS

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadVideos()
            } else {
                showMessage(
                    "برای نمایش ویدئوها، اجازه دسترسی به ویدئوها لازم است."
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        videoContainer =
            findViewById(
                R.id.videoContainer
            )

        countText =
            findViewById(
                R.id.countText
            )

        searchInput =
            findViewById(
                R.id.searchInput
            )

        sortButton =
            findViewById(
                R.id.sortButton
            )

        setupSearch()
        setupSortButton()
        setupExtraMenu()

        checkPermissionAndLoad()
    }

    override fun onResume() {

        super.onResume()

        if (
            ::videoContainer.isInitialized &&
            hasVideoPermission()
        ) {
            loadVideos()
        }
    }

    private fun setupSearch() {

        searchInput.addTextChangedListener(
            object :
                android.text.TextWatcher {

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
                    s: android.text.Editable?
                ) {
                }
            }
        )
    }

    private fun setupSortButton() {

        updateSortButtonText()

        sortButton.setOnClickListener {

            sortMode =
                when (sortMode) {

                    SortMode.LAST_ACCESS ->
                        SortMode.NAME

                    SortMode.NAME ->
                        SortMode.SIZE

                    SortMode.SIZE ->
                        SortMode.FAVORITES

                    SortMode.FAVORITES ->
                        SortMode.LAST_ACCESS
                }

            updateSortButtonText()

            filterVideos(
                searchInput.text.toString()
            )
        }
    }

    private fun setupExtraMenu() {

        sortButton.setOnLongClickListener {

            showMainMenu()

            true
        }
    }

    private fun showMainMenu() {

        val popup =
            PopupMenu(
                this,
                sortButton
            )

        popup.menu.add(
            "تنظیمات"
        )

        popup.menu.add(
            "پلی‌لیست‌ها"
        )

        popup.menu.add(
            "نمایش ویدئوهای مخفی"
        )

        popup.menu.add(
            "ساخت پلی‌لیست جدید"
        )

        popup.setOnMenuItemClickListener { item ->

            when (item.title.toString()) {

                "تنظیمات" -> {

                    startActivity(
                        Intent(
                            this,
                            SettingsActivity::class.java
                        )
                    )
                }

                "پلی‌لیست‌ها" -> {

                    showPlaylists()
                }

                "نمایش ویدئوهای مخفی" -> {

                    VidoraSettings.setShowHidden(
                        this,
                        !VidoraSettings.showHidden(
                            this
                        )
                    )

                    loadVideos()
                }

                "ساخت پلی‌لیست جدید" -> {

                    createPlaylist()
                }
            }

            true
        }

        popup.show()
    }

    private fun createPlaylist() {

        val input =
            EditText(this).apply {

                hint =
                    "نام پلی‌لیست"
            }

        AlertDialog.Builder(this)
            .setTitle(
                "پلی‌لیست جدید"
            )
            .setView(input)
            .setNegativeButton(
                "لغو",
                null
            )
            .setPositiveButton(
                "ساختن"
            ) { _, _ ->

                val success =
                    PlaylistManager.create(
                        this,
                        input.text.toString()
                    )

                showMessage(
                    if (success) {
                        "پلی‌لیست ساخته شد."
                    } else {
                        "این نام قبلاً استفاده شده یا نام خالی است."
                    }
                )
            }
            .show()
    }

    private fun showPlaylists() {

        val playlists =
            PlaylistManager.getPlaylists(
                this
            )

        if (playlists.isEmpty()) {

            AlertDialog.Builder(this)
                .setTitle(
                    "پلی‌لیست‌ها"
                )
                .setMessage(
                    "هنوز پلی‌لیستی ساخته نشده است."
                )
                .setPositiveButton(
                    "ساخت پلی‌لیست"
                ) { _, _ ->
                    createPlaylist()
                }
                .setNegativeButton(
                    "بستن",
                    null
                )
                .show()

            return
        }

        val names =
            playlists.map {
                "${it.name} (${it.videos.size})"
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                "پلی‌لیست‌ها"
            )
            .setItems(
                names
            ) { _, which ->

                val playlist =
                    playlists[which]

                showPlaylistOptions(
                    playlist.name
                )
            }
            .setPositiveButton(
                "بستن",
                null
            )
            .show()
    }

    private fun showPlaylistOptions(
        playlistName: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                playlistName
            )
            .setItems(
                arrayOf(
                    "حذف پلی‌لیست"
                )
            ) { _, _ ->

                AlertDialog.Builder(this)
                    .setTitle(
                        "حذف پلی‌لیست"
                    )
                    .setMessage(
                        "آیا این پلی‌لیست حذف شود؟"
                    )
                    .setNegativeButton(
                        "لغو",
                        null
                    )
                    .setPositiveButton(
                        "حذف"
                    ) { _, _ ->

                        PlaylistManager.delete(
                            this,
                            playlistName
                        )

                        showMessage(
                            "پلی‌لیست حذف شد."
                        )
                    }
                    .show()
            }
            .setNegativeButton(
                "بستن",
                null
            )
            .show()
    }

    private fun updateSortButtonText() {

        sortButton.text =
            when (sortMode) {

                SortMode.LAST_ACCESS ->
                    "آخرین دسترسی"

                SortMode.NAME ->
                    "نام"

                SortMode.SIZE ->
                    "حجم"

                SortMode.FAVORITES ->
                    "علاقه‌مندی‌ها"
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
        ) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun loadVideos() {

        allVideos.clear()

        val projection =
            mutableListOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            projection.add(
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )
        }

        val collection =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )

            } else {

                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        contentResolver.query(
            collection,
            projection.toTypedArray(),
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

            val bucketColumn =
                cursor.getColumnIndex(
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                )

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(
                        idColumn
                    )

                val name =
                    cursor.getString(
                        nameColumn
                    ) ?: "ویدئوی ناشناس"

                val duration =
                    cursor.getLong(
                        durationColumn
                    )

                val size =
                    cursor.getLong(
                        sizeColumn
                    )

                val dateAdded =
                    cursor.getLong(
                        dateColumn
                    )

                val folderName =
                    if (bucketColumn >= 0) {

                        cursor.getString(
                            bucketColumn
                        )?.takeIf {
                            it.isNotBlank()
                        } ?: "پوشه ناشناس"

                    } else {

                        "پوشه ناشناس"
                    }

                val uri =
                    Uri.withAppendedPath(
                        collection,
                        id.toString()
                    )

                if (
                    !HiddenVideoManager.isHidden(
                        this,
                        uri
                    ) ||
                    VidoraSettings.showHidden(
                        this
                    )
                ) {

                    allVideos.add(
                        VideoItem(
                            uri = uri,
                            name = name,
                            duration = duration,
                            size = size,
                            dateAdded = dateAdded,
                            folderName = folderName
                        )
                    )
                }
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
            query.trim()
                .lowercase(
                    Locale.getDefault()
                )

        var filtered =

            if (
                normalizedQuery.isEmpty()
            ) {

                allVideos.toList()

            } else {

                allVideos.filter {

                    it.name
                        .lowercase(
                            Locale.getDefault()
                        )
                        .contains(
                            normalizedQuery
                        ) ||
                        it.folderName
                            .lowercase(
                                Locale.getDefault()
                            )
                            .contains(
                                normalizedQuery
                            )
                }
            }

        if (
            sortMode ==
            SortMode.FAVORITES
        ) {

            filtered =
                filtered.filter {

                    FavoriteManager.isFavorite(
                        this,
                        it.uri
                    )
                }
        }

        val sorted =
            when (sortMode) {

                SortMode.LAST_ACCESS ->
                    filtered.sortedWith(
                        compareByDescending<VideoItem> {
                            getLastAccess(
                                it.uri
                            )
                        }.thenBy {
                            it.name.lowercase(
                                Locale.getDefault()
                            )
                        }
                    )

                SortMode.NAME ->
                    filtered.sortedWith(
                        compareBy<VideoItem> {
                            it.name.lowercase(
                                Locale.getDefault()
                            )
                        }.thenBy {
                            it.folderName.lowercase(
                                Locale.getDefault()
                            )
                        }
                    )

                SortMode.SIZE ->
                    filtered.sortedWith(
                        compareByDescending<VideoItem> {
                            it.size
                        }.thenBy {
                            it.name.lowercase(
                                Locale.getDefault()
                            )
                        }
                    )

                SortMode.FAVORITES ->
                    filtered.sortedWith(
                        compareBy<VideoItem> {
                            it.name.lowercase(
                                Locale.getDefault()
                            )
                        }
                    )
            }

        displayVideos(
            sorted
        )
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
                    sortMode ==
                    SortMode.FAVORITES
                ) {
                    "هنوز ویدئوی مورد علاقه‌ای وجود ندارد."
                } else {
                    "ویدئویی پیدا نشد."
                }
            )

            return
        }

        var currentFolder: String? =
            null

        videos.forEach { video ->

            if (
                currentFolder !=
                video.folderName
            ) {

                addFolderHeader(
                    video.folderName
                )

                currentFolder =
                    video.folderName
            }

            addVideoItem(
                video
            )
        }
    }

    private fun addFolderHeader(
        folderName: String
    ) {

        val folderText =
            TextView(this).apply {

                text =
                    "📁 $folderName"

                textSize =
                    17f

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.vidora_text
                    )
                )

                setPadding(
                    20,
                    28,
                    20,
                    12
                )

                gravity =
                    Gravity.START or
                        Gravity.CENTER_VERTICAL
            }

        videoContainer.addView(
            folderText
        )
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

        val favoriteMark =
            if (
                FavoriteManager.isFavorite(
                    this,
                    video.uri
                )
            ) {
                "  ⭐"
            } else {
                ""
            }

        infoText.text =
            buildString {

                append(
                    formatDuration(
                        video.duration
                    )
                )

                append(
                    "  •  "
                )

                append(
                    formatSize(
                        video.size
                    )
                )

                append(
                    favoriteMark
                )
            }

        loadThumbnail(
            video.uri,
            thumbnailImage
        )

        view.setOnClickListener {

            saveLastAccess(
                video.uri
            )

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

            startActivity(
                intent
            )
        }

        view.setOnLongClickListener {

            showVideoMenu(
                video
            )

            true
        }

        videoContainer.addView(
            view
        )
    }

    private fun showVideoMenu(
        video: VideoItem
    ) {

        val options =
            arrayOf(
                "پخش ویدئو",
                "⭐ تغییر علاقه‌مندی",
                "🙈 مخفی کردن",
                "📋 افزودن به پلی‌لیست",
                "✏️ تغییر نام",
                "📤 اشتراک‌گذاری",
                "🗑 حذف"
            )

        AlertDialog.Builder(this)
            .setTitle(
                video.name
            )
            .setItems(
                options
            ) { _, which ->

                when (which) {

                    0 -> openVideo(
                        video
                    )

                    1 -> toggleFavoriteFromLibrary(
                        video
                    )

                    2 -> hideVideo(
                        video
                    )

                    3 -> addToPlaylist(
                        video
                    )

                    4 -> renameVideo(
                        video
                    )

                    5 -> shareVideo(
                        video
                    )

                    6 -> deleteVideo(
                        video
                    )
                }
            }
            .show()
    }

    private fun openVideo(
        video: VideoItem
    ) {

        saveLastAccess(
            video.uri
        )

        startActivity(
            Intent(
                this,
                PlayerActivity::class.java
            ).apply {

                putExtra(
                    PlayerActivity.EXTRA_VIDEO_URI,
                    video.uri.toString()
                )

                putExtra(
                    PlayerActivity.EXTRA_VIDEO_NAME,
                    video.name
                )
            }
        )
    }

    private fun hideVideo(
        video: VideoItem
    ) {

        HiddenVideoManager.hide(
            this,
            video.uri
        )

        showMessage(
            "ویدئو مخفی شد."
        )

        loadVideos()
    }

    private fun addToPlaylist(
        video: VideoItem
    ) {

        val playlists =
            PlaylistManager.getPlaylists(
                this
            )

        if (playlists.isEmpty()) {

            createPlaylist()

            return
        }

        val names =
            playlists.map {
                it.name
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                "افزودن به پلی‌لیست"
            )
            .setItems(
                names
            ) { _, which ->

                val success =
                    PlaylistManager.addVideo(
                        this,
                        playlists[which].name,
                        video.uri
                    )

                showMessage(
                    if (success) {
                        "به پلی‌لیست اضافه شد."
                    } else {
                        "این ویدئو قبلاً در پلی‌لیست وجود دارد."
                    }
                )
            }
            .show()
    }

    private fun renameVideo(
        video: VideoItem
    ) {

        val input =
            EditText(this).apply {

                setText(
                    video.name
                )

                selectAll()
            }

        AlertDialog.Builder(this)
            .setTitle(
                "تغییر نام ویدئو"
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

                if (
                    newName.isBlank()
                ) {
                    showMessage(
                        "نام نمی‌تواند خالی باشد."
                    )

                    return@setPositiveButton
                }

                try {

                    val values =
                        android.content.ContentValues()
                            .apply {
                                put(
                                    MediaStore.Video.Media.DISPLAY_NAME,
                                    newName
                                )
                            }

                    val changed =
                        contentResolver.update(
                            video.uri,
                            values,
                            null,
                            null
                        )

                    if (changed > 0) {

                        showMessage(
                            "نام ویدئو تغییر کرد."
                        )

                        loadVideos()

                    } else {

                        showMessage(
                            "تغییر نام انجام نشد."
                        )
                    }

                } catch (_: Exception) {

                    showMessage(
                        "تغییر نام این فایل امکان‌پذیر نیست."
                    )
                }
            }
            .show()
    }

    private fun shareVideo(
        video: VideoItem
    ) {

        val intent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "video/*"

                putExtra(
                    Intent.EXTRA_STREAM,
                    video.uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "اشتراک‌گذاری ویدئو"
            )
        )
    }

    private fun deleteVideo(
        video: VideoItem
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "حذف ویدئو"
            )
            .setMessage(
                "آیا مطمئن هستید که این ویدئو حذف شود؟"
            )
            .setNegativeButton(
                "لغو",
                null
            )
            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                try {

                    val deleted =
                        contentResolver.delete(
                            video.uri,
                            null,
                            null
                        )

                    if (deleted > 0) {

                        showMessage(
                            "ویدئو حذف شد."
                        )

                        loadVideos()

                    } else {

                        showMessage(
                            "حذف انجام نشد."
                        )
                    }

                } catch (_: Exception) {

                    showMessage(
                        "امکان حذف این ویدئو وجود ندارد."
                    )
                }
            }
            .show()
    }

    private fun toggleFavoriteFromLibrary(
        video: VideoItem
    ) {

        val favorite =
            FavoriteManager.toggle(
                this,
                video.uri
            )

        showMessage(
            if (favorite) {
                "به علاقه‌مندی‌ها اضافه شد."
            } else {
                "از علاقه‌مندی‌ها حذف شد."
            }
        )

        filterVideos(
            searchInput.text.toString()
        )
    }

    private fun getLastAccess(
        uri: Uri
    ): Long {

        return getSharedPreferences(
            "vidora_library_preferences",
            MODE_PRIVATE
        ).getLong(
            "access_${uri}",
            0L
        )
    }

    private fun saveLastAccess(
        uri: Uri
    ) {

        getSharedPreferences(
            "vidora_library_preferences",
            MODE_PRIVATE
        )
            .edit()
            .putLong(
                "access_${uri}",
                System.currentTimeMillis()
            )
            .apply()
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

            val bitmap: Bitmap? =
                retriever.getFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
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

        android.widget.Toast
            .makeText(
                this,
                message,
                android.widget.Toast.LENGTH_SHORT
            )
            .show()
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

        return if (
            megabytes >= 1024.0
        ) {

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
        val dateAdded: Long,
        val folderName: String
    )

    enum class SortMode {
        LAST_ACCESS,
        NAME,
        SIZE,

    FAVORITES
  fun openOnlinePlayer(
    view: android.view.View
) {
    startActivity(
        Intent(
            this,
            OnlineVideoActivity::class.java
        )
    )
}

fun openVidoraSettings(
    view: android.view.View
) {
    startActivity(
        Intent(
            this,
            SettingsActivity::class.java
        )
    )
}

fun openPrivateVault(
    view: android.view.View
) {
    startActivity(
        Intent(
            this,
            PrivateVaultActivity::class.java
        )
    )
}  }
}
