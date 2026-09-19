package com.vidora.player

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var videoContainer: LinearLayout
    private lateinit var countText: TextView
    private lateinit var searchInput: EditText
    private lateinit var sortButton: Button
    private lateinit var viewModeButton: Button
    private lateinit var backFolderButton: Button
    private lateinit var locationText: TextView

    private val allVideos =
        mutableListOf<VideoItem>()

    private var currentFolder: String? = null

    private var sortMode =
        SortMode.LAST_ACCESS

    private var gridMode =
        false

    private enum class SortMode {
        LAST_ACCESS,
        NAME,
        SIZE,
        FAVORITES
    }

    private val permissionRequestCode =
        7001

    override fun attachBaseContext(
        newBase: android.content.Context
    ) {
        super.attachBaseContext(
            VidoraLocaleManager.apply(
                newBase
            )
        )
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

        viewModeButton =
            findViewById(
                R.id.viewModeButton
            )

        backFolderButton =
            findViewById(
                R.id.backFolderButton
            )

        locationText =
            findViewById(
                R.id.locationText
            )

        VidoraLocaleManager.applyDirection(
            window.decorView,
            this
        )

        loadSavedDisplayMode()

        restoreScreenState(
            savedInstanceState
        )

        setupSearch()

        sortButton.setOnClickListener {
            showSortMenu()
        }

        viewModeButton.setOnClickListener {
            gridMode = !gridMode
            saveDisplayMode()
            updateViewModeButton()
            displayVideos()
        }

        backFolderButton.setOnClickListener {
            openRoot()
        }

        updateViewModeButton()
        updateSortButton()

        if (hasVideoPermission()) {
            loadVideos()
        } else {
            requestVideoPermission()
        }
    }

    private fun t(
        text: String
    ): String {

        return VidoraTextTranslator.translate(
            this,
            text
        )
    }

    fun openOnlinePlayer(
        view: View
    ) {

        startActivity(
            Intent(
                this,
                OnlineVideoActivity::class.java
            )
        )
    }

    fun openVidoraSettings(
        view: View
    ) {

        startActivity(
            Intent(
                this,
                SettingsActivity::class.java
            )
        )
    }

    fun openPrivateVault(
        view: View
    ) {

        startActivity(
            Intent(
                this,
                PrivateVaultActivity::class.java
            )
        )
    }

    private fun restoreScreenState(
        savedInstanceState: Bundle?
    ) {

        if (savedInstanceState == null) {
            return
        }

        currentFolder =
            savedInstanceState.getString(
                KEY_CURRENT_FOLDER
            )

        val savedSort =
            savedInstanceState.getInt(
                KEY_SORT_MODE,
                SortMode.LAST_ACCESS.ordinal
            )

        sortMode =
            SortMode.entries.getOrElse(
                savedSort
            ) {
                SortMode.LAST_ACCESS
            }

        val savedSearch =
            savedInstanceState.getString(
                KEY_SEARCH_TEXT
            )

        if (!savedSearch.isNullOrEmpty()) {
            searchInput.setText(
                savedSearch
            )
        }
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        outState.putString(
            KEY_CURRENT_FOLDER,
            currentFolder
        )

        outState.putInt(
            KEY_SORT_MODE,
            sortMode.ordinal
        )

        outState.putString(
            KEY_SEARCH_TEXT,
            searchInput.text.toString()
        )

        super.onSaveInstanceState(
            outState
        )
    }

    private fun loadSavedDisplayMode() {

        gridMode =
            getSharedPreferences(
                DISPLAY_PREFS,
                MODE_PRIVATE
            )
                .getBoolean(
                    KEY_GRID_MODE,
                    false
                )
    }

    private fun saveDisplayMode() {

        getSharedPreferences(
            DISPLAY_PREFS,
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                KEY_GRID_MODE,
                gridMode
            )
            .apply()
    }

    private fun updateViewModeButton() {

        viewModeButton.text =
            if (gridMode) {
                "☰"
            } else {
                "▦"
            }

        viewModeButton.contentDescription =
            if (gridMode) {
                t("نمایش فهرستی")
            } else {
                t("نمایش شبکه‌ای")
            }
    }

    private fun updateSortButton() {

        sortButton.text =
            when (sortMode) {

                SortMode.LAST_ACCESS ->
                    t("جدیدترین")

                SortMode.NAME ->
                    t("نام")

                SortMode.SIZE ->
                    t("حجم")

                SortMode.FAVORITES ->
                    t("علاقه‌مندی‌ها")
            }
    }

    private fun setupSearch() {

        searchInput.hint =
            if (
                VidoraLanguageManager.isPersian(
                    this
                )
            ) {
                "جستجوی ویدئو"
            } else {
                "Search videos"
            }

        searchInput.contentDescription =
            searchInput.hint

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
                    displayVideos()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun hasVideoPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            requiredVideoPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestVideoPermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                requiredVideoPermission()
            ),
            permissionRequestCode
        )
    }

    private fun requiredVideoPermission(): String {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            permissionRequestCode
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                loadVideos()

            } else {

                countText.text =
                    t(
                        "مجوز دسترسی به ویدئوها داده نشد."
                    )
            }
        }
    }

    private fun loadVideos() {

        allVideos.clear()

        if (!hasVideoPermission()) {
            return
        }

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

        val projection =
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

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

            val folderColumn =
                cursor.getColumnIndex(
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                )

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(
                        idColumn
                    )

                val uri =
                    Uri.withAppendedPath(
                        collection,
                        id.toString()
                    )

                if (
                    HiddenVideoManager.isHidden(
                        this,
                        uri
                    ) &&
                    !VidoraSettings.showHidden(
                        this
                    )
                ) {
                    continue
                }

                val name =
                    cursor.getString(
                        nameColumn
                    ) ?: t("ویدئو")

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
                    if (folderColumn >= 0) {
                        cursor.getString(
                            folderColumn
                        ) ?: t("سایر")
                    } else {
                        t("سایر")
                    }

                allVideos.add(
                    VideoItem(
                        uri = uri,
                        name = name,
                        duration = duration,
                        size = size,
                        dateAdded = dateAdded,
                        folderName =
                            folderName.ifBlank {
                                t("سایر")
                            }
                    )
                )
            }
        }

        displayVideos()
    }

    private fun displayVideos() {

        videoContainer.removeAllViews()

        val query =
            searchInput.text
                .toString()
                .trim()
                .lowercase()

        if (currentFolder == null) {

            displayFolders(
                query
            )

        } else {

            displayFolderVideos(
                currentFolder!!,
                query
            )
        }
    }

    private fun displayFolders(
        query: String
    ) {

        val folders =
            allVideos
                .groupBy {
                    it.folderName
                }
                .toSortedMap(
                    compareBy {
                        it.lowercase()
                    }
                )

        val visibleFolders =
            mutableListOf<Pair<String, Int>>()

        var visibleVideoCount = 0

        folders.forEach { (folderName, videos) ->

            val matchingVideos =
                if (query.isBlank()) {
                    videos
                } else {
                    videos.filter {
                        it.name
                            .lowercase()
                            .contains(query)
                    }
                }

            if (
                query.isNotBlank() &&
                matchingVideos.isEmpty()
            ) {
                return@forEach
            }

            visibleFolders.add(
                folderName to matchingVideos.size
            )

            visibleVideoCount +=
                matchingVideos.size
        }

        if (visibleFolders.isEmpty()) {

            showEmptyMessage(
                if (folders.isEmpty()) {
                    t("هیچ ویدئویی پیدا نشد.")
                } else {
                    t("نتیجه‌ای برای جستجو پیدا نشد.")
                }
            )

        } else if (gridMode) {

            displayFolderGrid(
                visibleFolders
            )

        } else {

            visibleFolders.forEach { item ->

                addFolderItem(
                    item.first,
                    item.second
                )
            }
        }

        locationText.text =
            t("همه پوشه‌ها")

        backFolderButton.visibility =
            View.GONE

        countText.text =
            if (query.isBlank()) {
                folderSummaryText(
                    folders.size,
                    allVideos.size
                )
            } else {
                searchFolderSummaryText(
                    visibleFolders.size,
                    visibleVideoCount
                )
            }
    }

    private fun folderSummaryText(
        folderCount: Int,
        videoCount: Int
    ): String {

        return if (
            VidoraLanguageManager.isPersian(
                this
            )
        ) {
            "$folderCount پوشه • $videoCount ویدئو"
        } else {
            "$folderCount ${if (folderCount == 1) "folder" else "folders"} • " +
                "$videoCount ${if (videoCount == 1) "video" else "videos"}"
        }
    }

    private fun searchFolderSummaryText(
        folderCount: Int,
        videoCount: Int
    ): String {

        return if (
            VidoraLanguageManager.isPersian(
                this
            )
        ) {
            "$folderCount پوشه • $videoCount ویدئو"
        } else {
            "$folderCount ${if (folderCount == 1) "folder" else "folders"} • " +
                "$videoCount ${if (videoCount == 1) "video" else "videos"}"
        }
    }

    private fun displayFolderGrid(
        folders: List<Pair<String, Int>>
    ) {

        displayResponsiveGrid(
            itemCount = folders.size
        ) { row, index ->

            val folder =
                folders[index]

            addGridFolderItem(
                row,
                folder.first,
                folder.second
            )
        }
    }

    private fun displayFolderVideos(
        folderName: String,
        query: String
    ) {

        val videos =
            allVideos
                .filter {
                    it.folderName == folderName
                }
                .filter {
                    query.isBlank() ||
                        it.name
                            .lowercase()
                            .contains(query)
                }
                .let {
                    sortVideos(it)
                }

        locationText.text =
            "📁 $folderName"

        backFolderButton.visibility =
            View.VISIBLE

        countText.text =
            videoCountText(
                videos.size
            )

        if (videos.isEmpty()) {

            showEmptyMessage(
                if (query.isBlank()) {
                    t("این پوشه ویدئویی ندارد.")
                } else {
                    t("نتیجه‌ای برای جستجو پیدا نشد.")
                }
            )

            return
        }

        if (gridMode) {

            displayGrid(
                videos
            )

        } else {

            videos.forEach {
                addVideoItem(
                    it,
                    false
                )
            }
        }
    }

    private fun videoCountText(
        count: Int
    ): String {

        return if (
            VidoraLanguageManager.isPersian(
                this
            )
        ) {
            "$count ویدئو"
        } else {
            "$count ${if (count == 1) "video" else "videos"}"
        }
    }

    private fun sortVideos(
        videos: List<VideoItem>
    ): List<VideoItem> {

        return when (sortMode) {

            SortMode.LAST_ACCESS ->
                videos.sortedByDescending {
                    it.dateAdded
                }

            SortMode.NAME ->
                videos.sortedBy {
                    it.name.lowercase()
                }

            SortMode.SIZE ->
                videos.sortedByDescending {
                    it.size
                }

            SortMode.FAVORITES ->
                videos.sortedWith(
                    compareByDescending<VideoItem> {
                        FavoriteManager.isFavorite(
                            this,
                            it.uri
                        )
                    }.thenBy {
                        it.name.lowercase()
                    }
                )
        }
    }

    private fun displayGrid(
        videos: List<VideoItem>
    ) {

        displayResponsiveGrid(
            itemCount = videos.size
        ) { row, index ->

            addGridVideoItem(
                row,
                videos[index]
            )
        }
    }

    private fun displayResponsiveGrid(
        itemCount: Int,
        addItem: (
            LinearLayout,
            Int
        ) -> Unit
    ) {

        if (itemCount <= 0) {
            return
        }

        val columnCount =
            calculateGridColumnCount()

        var row: LinearLayout? =
            null

        repeat(itemCount) { index ->

            if (
                index % columnCount == 0
            ) {

                row =
                    LinearLayout(this).apply {

                        orientation =
                            LinearLayout.HORIZONTAL

                        gravity =
                            Gravity.TOP

                        layoutParams =
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {

                                topMargin =
                                    dp(2)

                                bottomMargin =
                                    dp(2)
                            }
                    }

                videoContainer.addView(
                    row
                )
            }

            addItem(
                row!!,
                index
            )
        }
    }

    private fun calculateGridColumnCount(): Int {

        val widthPixels =
            resources.displayMetrics.widthPixels

        val density =
            resources.displayMetrics.density

        val widthDp =
            widthPixels / density

        val minimumItemWidthDp =
            if (
                widthDp >= 600f
            ) {
                180f
            } else {
                160f
            }

        val availableWidthDp =
            widthDp - 16f

        val calculated =
            (
                availableWidthDp /
                    minimumItemWidthDp
                ).toInt()

        return calculated.coerceIn(
            2,
            4
        )
    }

    private fun addFolderItem(
        folderName: String,
        videoCount: Int
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_folder,
                    videoContainer,
                    false
                )

        val nameText =
            view.findViewById<TextView>(
                R.id.folderNameText
            )

        val infoText =
            view.findViewById<TextView>(
                R.id.folderInfoText
            )

        nameText.text =
            folderName

        infoText.text =
            folderCountText(
                videoCount
            )

        view.setOnClickListener {

            currentFolder =
                folderName

            searchInput.text.clear()

            displayVideos()
        }

        videoContainer.addView(
            view
        )
    }

    private fun addGridFolderItem(
        parent: LinearLayout,
        folderName: String,
        videoCount: Int
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_folder_grid,
                    parent,
                    false
                )

        val nameText =
            view.findViewById<TextView>(
                R.id.folderNameText
            )

        val infoText =
            view.findViewById<TextView>(
                R.id.folderInfoText
            )

        nameText.text =
            folderName

        infoText.text =
            folderCountText(
                videoCount
            )

        view.setOnClickListener {

            currentFolder =
                folderName

            searchInput.text.clear()

            displayVideos()
        }

        val params =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {

                marginStart =
                    dp(2)

                marginEnd =
                    dp(2)
            }

        view.layoutParams =
            params

        parent.addView(
            view
        )
    }

    private fun folderCountText(
        count: Int
    ): String {

        return if (
            VidoraLanguageManager.isPersian(
                this
            )
        ) {

            if (count == 1) {
                "۱ ویدئو"
            } else {
                "$count ویدئو"
            }

        } else {

            "$count ${
                if (count == 1) {
                    "video"
                } else {
                    "videos"
                }
            }"
        }
    }

    private fun addGridVideoItem(
        parent: LinearLayout,
        video: VideoItem
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_video_grid,
                    parent,
                    false
                )

        bindVideoView(
            view,
            video
        )

        val params =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {

                marginStart =
                    dp(2)

                marginEnd =
                    dp(2)
            }

        view.layoutParams =
            params

        parent.addView(
            view
        )

        val thumbnail =
            view.findViewById<ImageView>(
                R.id.thumbnailImage
            )

        view.post {

            val width =
                thumbnail.width

            if (width > 0) {

                thumbnail.layoutParams =
                    thumbnail.layoutParams.apply {

                        height =
                            (
                                width * 9 / 16
                            ).coerceAtLeast(
                                dp(70)
                            )
                    }

                thumbnail.requestLayout()
            }
        }
    }

    private fun addVideoItem(
        video: VideoItem,
        includeFolderName: Boolean
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.item_video,
                    videoContainer,
                    false
                )

        bindVideoView(
            view,
            video
        )

        if (includeFolderName) {

            val info =
                view.findViewById<TextView>(
                    R.id.infoText
                )

            info.text =
                "${video.folderName} • ${
                    formatDuration(
                        video.duration
                    )
                } • ${
                    formatFileSize(
                        video.size
                    )
                }"
        }

        videoContainer.addView(
            view
        )
    }

    private fun bindVideoView(
        view: View,
        video: VideoItem
    ) {

        val thumbnail =
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

        val favorite =
            FavoriteManager.isFavorite(
                this,
                video.uri
            )

        infoText.text =
            "${if (favorite) "★ " else ""}${
                formatDuration(
                    video.duration
                )
            } • ${
                formatFileSize(
                    video.size
                )
            }"

        thumbnail.setImageResource(
            android.R.drawable.ic_media_play
        )

        loadThumbnail(
            video.uri,
            thumbnail
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
    }

    private fun loadThumbnail(
        uri: Uri,
        imageView: ImageView
    ) {

        Thread {

            val bitmap =
                try {

                    val retriever =
                        MediaMetadataRetriever()

                    retriever.setDataSource(
                        this,
                        uri
                    )

                    val frame =
                        retriever.getFrameAtTime(
                            0,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )

                    retriever.release()

                    frame

                } catch (_: Exception) {

                    null
                }

            imageView.post {

                if (bitmap != null) {

                    imageView.setImageBitmap(
                        bitmap
                    )
                }
            }

        }.start()
    }

    private fun showVideoMenu(
        video: VideoItem
    ) {

        val favorite =
            FavoriteManager.isFavorite(
                this,
                video.uri
            )

        val options =
            arrayOf(
                t("پخش"),
                if (favorite) {
                    t("حذف از علاقه‌مندی‌ها")
                } else {
                    t("افزودن به علاقه‌مندی‌ها")
                },
                t("مخفی کردن"),
                t("افزودن به پلی‌لیست"),
                t("تغییر نام"),
                t("اشتراک‌گذاری"),
                t("حذف")
            )

        AlertDialog.Builder(this)
            .setTitle(
                video.name
            )
            .setItems(
                options
            ) { _, which ->

                when (which) {

                    0 ->
                        openVideo(
                            video
                        )

                    1 -> {

                        FavoriteManager.toggle(
                            this,
                            video.uri
                        )

                        displayVideos()
                    }

                    2 -> {

                        HiddenVideoManager.hide(
                            this,
                            video.uri
                        )

                        loadVideos()
                    }

                    3 ->
                        showPlaylistMenu(
                            video
                        )

                    4 ->
                        renameVideo(
                            video
                        )

                    5 ->
                        VideoShareManager.share(
                            this,
                            video.uri
                        )

                    6 ->
                        deleteVideo(
                            video
                        )
                }
            }
            .show()
    }

    private fun openVideo(
        video: VideoItem
    ) {

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

    private fun showPlaylistMenu(
        video: VideoItem
    ) {

        try {

            PlaylistDialogManager.showAddToPlaylistDialog(
                this,
                video.uri
            )

        } catch (_: Exception) {

            showTemporaryMessage(
                t("مدیریت پلی‌لیست در دسترس نیست.")
            )
        }
    }

    private fun renameVideo(
        video: VideoItem
    ) {

        val input =
            EditText(this)

        input.setText(
            video.name
        )

        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle(
                t("تغییر نام ویدئو")
            )
            .setView(
                input
            )
            .setNegativeButton(
                t("لغو"),
                null
            )
            .setPositiveButton(
                t("ذخیره")
            ) { _, _ ->

                val newName =
                    input.text
                        .toString()
                        .trim()

                if (newName.isBlank()) {
                    return@setPositiveButton
                }

                try {

                    contentResolver.update(
                        video.uri,
                        android.content.ContentValues().apply {
                            put(
                                MediaStore.Video.Media.DISPLAY_NAME,
                                newName
                            )
                        },
                        null,
                        null
                    )

                    loadVideos()

                } catch (_: Exception) {

                    showTemporaryMessage(
                        t("تغییر نام انجام نشد.")
                    )
                }
            }
            .show()
    }

    private fun deleteVideo(
        video: VideoItem
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                t("حذف ویدئو")
            )
            .setMessage(
                t(
                    "آیا از حذف این ویدئو مطمئن هستید؟"
                )
            )
            .setNegativeButton(
                t("لغو"),
                null
            )
            .setPositiveButton(
                t("حذف")
            ) { _, _ ->

                try {

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.R
                    ) {

                        val request =
                            MediaStore.createDeleteRequest(
                                contentResolver,
                                listOf(video.uri)
                            )

                        startIntentSenderForResult(
                            request.intentSender,
                            8001,
                            null,
                            0,
                            0,
                            0,
                            null
                        )

                    } else {

                        contentResolver.delete(
                            video.uri,
                            null,
                            null
                        )

                        loadVideos()
                    }

                } catch (_: Exception) {

                    showTemporaryMessage(
                        t("حذف ویدئو انجام نشد.")
                    )
                }
            }
            .show()
    }

    private fun showSortMenu() {

        val options =
            arrayOf(
                t("جدیدترین"),
                t("نام"),
                t("حجم"),
                t("علاقه‌مندی‌ها")
            )

        AlertDialog.Builder(this)
            .setTitle(
                t("مرتب‌سازی")
            )
            .setSingleChoiceItems(
                options,
                sortMode.ordinal
            ) { dialog, which ->

                sortMode =
                    when (which) {

                        1 ->
                            SortMode.NAME

                        2 ->
                            SortMode.SIZE

                        3 ->
                            SortMode.FAVORITES

                        else ->
                            SortMode.LAST_ACCESS
                    }

                updateSortButton()

                dialog.dismiss()

                displayVideos()
            }
            .show()
    }

    private fun openRoot() {

        currentFolder =
            null

        searchInput.text.clear()

        displayVideos()
    }

    private fun showEmptyMessage(
        message: String
    ) {

        val text =
            TextView(this)

        text.text =
            message

        text.textSize =
            15f

        text.gravity =
            Gravity.CENTER

        text.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.vidora_text_secondary
            )
        )

        text.setPadding(
            16,
            60,
            16,
            60
        )

        videoContainer.addView(
            text
        )
    }

    private fun showTemporaryMessage(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun formatDuration(
        duration: Long
    ): String {

        if (duration <= 0L) {
            return "00:00"
        }

        val totalSeconds =
            duration / 1000L

        val seconds =
            totalSeconds % 60L

        val minutes =
            (totalSeconds / 60L) % 60L

        val hours =
            totalSeconds / 3600L

        return if (hours > 0L) {

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

    private fun formatFileSize(
        size: Long
    ): String {

        if (size <= 0L) {
            return "0 MB"
        }

        val mb =
            size / 1024.0 / 1024.0

        return if (mb >= 1024.0) {

            String.format(
                Locale.US,
                "%.1f GB",
                mb / 1024.0
            )

        } else {

            String.format(
                Locale.US,
                "%.1f MB",
                mb
            )
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onResume() {

        super.onResume()

        if (hasVideoPermission()) {
            loadVideos()
        }
    }

    companion object {

        private const val DISPLAY_PREFS =
            "vidora_home_settings"

        private const val KEY_GRID_MODE =
            "grid_mode"

        private const val KEY_CURRENT_FOLDER =
            "current_folder"

        private const val KEY_SORT_MODE =
            "sort_mode"

        private const val KEY_SEARCH_TEXT =
            "search_text"
    }

    data class VideoItem(
        val uri: Uri,
        val name: String,
        val duration: Long,
        val size: Long,
        val dateAdded: Long,
        val folderName: String
    )
}
