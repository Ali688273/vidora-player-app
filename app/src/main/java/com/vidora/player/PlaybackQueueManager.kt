package com.vidora.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray

/**
 * مدیریت صف پخش Vidora
 *
 * مسئولیت‌ها:
 * - ذخیره دائمی صف
 * - افزودن و حذف ویدئو
 * - جلوگیری از آیتم تکراری
 * - مدیریت موقعیت فعلی
 * - جابه‌جایی آیتم‌ها
 * - پاک کردن صف
 *
 * این کلاس مستقل است و منطق اصلی PlayerActivity
 * را تغییر نمی‌دهد.
 */
object PlaybackQueueManager {

    private const val PREFS =
        "vidora_playback_queue"

    private const val KEY_QUEUE =
        "queue"

    private const val KEY_CURRENT_INDEX =
        "current_index"

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    /**
     * دریافت کل صف پخش.
     */
    fun getQueue(
        context: Context
    ): List<Uri> {

        val raw =
            prefs(context)
                .getString(
                    KEY_QUEUE,
                    null
                )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            val result =
                mutableListOf<Uri>()

            for (
                index in 0 until array.length()
            ) {

                val value =
                    array.optString(
                        index,
                        ""
                    )

                if (
                    value.isNotBlank()
                ) {

                    result.add(
                        Uri.parse(value)
                    )
                }
            }

            result

        } catch (_: Exception) {

            emptyList()
        }
    }

    /**
     * جایگزینی کامل صف.
     */
    fun setQueue(
        context: Context,
        queue: List<Uri>
    ) {

        val unique =
            LinkedHashSet<String>()

        queue.forEach { uri ->

            val value =
                uri.toString()

            if (
                value.isNotBlank()
            ) {
                unique.add(value)
            }
        }

        val array =
            JSONArray()

        unique.forEach { value ->
            array.put(value)
        }

        prefs(context)
            .edit()
            .putString(
                KEY_QUEUE,
                array.toString()
            )
            .apply()

        normalizeCurrentIndex(
            context
        )
    }

    /**
     * افزودن یک ویدئو به انتهای صف.
     *
     * خروجی:
     * true  = اضافه شد
     * false = قبلاً وجود داشت
     */
    fun add(
        context: Context,
        uri: Uri
    ): Boolean {

        val queue =
            getQueue(context)
                .toMutableList()

        if (
            queue.any {
                it.toString() ==
                    uri.toString()
            }
        ) {

            return false
        }

        queue.add(uri)

        setQueue(
            context,
            queue
        )

        return true
    }

    /**
     * افزودن چند ویدئو به صف.
     *
     * ویدئوهای تکراری نادیده گرفته می‌شوند.
     *
     * خروجی تعداد آیتم‌هایی است که واقعاً
     * به صف اضافه شده‌اند.
     */
    fun addAll(
        context: Context,
        uris: List<Uri>
    ): Int {

        if (uris.isEmpty()) {
            return 0
        }

        val queue =
            getQueue(context)
                .toMutableList()

        val existing =
            queue
                .map {
                    it.toString()
                }
                .toMutableSet()

        var addedCount =
            0

        uris.forEach { uri ->

            val value =
                uri.toString()

            if (
                value.isNotBlank() &&
                existing.add(value)
            ) {

                queue.add(uri)

                addedCount++
            }
        }

        if (
            addedCount > 0
        ) {

            setQueue(
                context,
                queue
            )
        }

        return addedCount
    }

    /**
     * حذف ویدئو از صف.
     */
    fun remove(
        context: Context,
        uri: Uri
    ): Boolean {

        val queue =
            getQueue(context)
                .toMutableList()

        val index =
            queue.indexOfFirst {
                it.toString() ==
                    uri.toString()
            }

        if (
            index < 0
        ) {
            return false
        }

        val currentIndex =
            getCurrentIndex(
                context
            )

        queue.removeAt(index)

        setQueue(
            context,
            queue
        )

        when {

            queue.isEmpty() -> {

                setCurrentIndex(
                    context,
                    -1
                )
            }

            currentIndex < 0 -> {

                setCurrentIndex(
                    context,
                    0
                )
            }

            index < currentIndex -> {

                setCurrentIndex(
                    context,
                    currentIndex - 1
                )
            }

            index == currentIndex &&
                currentIndex >= queue.size -> {

                setCurrentIndex(
                    context,
                    queue.lastIndex
                )
            }

            else -> {

                normalizeCurrentIndex(
                    context
                )
            }
        }

        return true
    }

    /**
     * حذف آیتم با شماره اندیس.
     */
    fun removeAt(
        context: Context,
        index: Int
    ): Boolean {

        val queue =
            getQueue(context)
                .toMutableList()

        if (
            index !in queue.indices
        ) {
            return false
        }

        val currentIndex =
            getCurrentIndex(
                context
            )

        queue.removeAt(index)

        setQueue(
            context,
            queue
        )

        when {

            queue.isEmpty() -> {

                setCurrentIndex(
                    context,
                    -1
                )
            }

            currentIndex < 0 -> {

                setCurrentIndex(
                    context,
                    0
                )
            }

            index < currentIndex -> {

                setCurrentIndex(
                    context,
                    currentIndex - 1
                )
            }

            index == currentIndex &&
                currentIndex >= queue.size -> {

                setCurrentIndex(
                    context,
                    queue.lastIndex
                )
            }

            else -> {

                normalizeCurrentIndex(
                    context
                )
            }
        }

        return true
    }

    /**
     * جابه‌جایی دو آیتم در صف.
     */
    fun move(
        context: Context,
        fromIndex: Int,
        toIndex: Int
    ): Boolean {

        val queue =
            getQueue(context)
                .toMutableList()

        if (
            fromIndex !in queue.indices ||
            toIndex !in queue.indices
        ) {

            return false
        }

        if (
            fromIndex ==
            toIndex
        ) {

            return false
        }

        val currentIndex =
            getCurrentIndex(
                context
            )

        val item =
            queue.removeAt(
                fromIndex
            )

        queue.add(
            toIndex,
            item
        )

        setQueue(
            context,
            queue
        )

        val newCurrentIndex =
            when {

                currentIndex < 0 ->
                    currentIndex

                currentIndex == fromIndex ->
                    toIndex

                fromIndex < currentIndex &&
                    toIndex >= currentIndex ->
                    currentIndex - 1

                fromIndex > currentIndex &&
                    toIndex <= currentIndex ->
                    currentIndex + 1

                else ->
                    currentIndex
            }

        setCurrentIndex(
            context,
            newCurrentIndex
        )

        return true
    }

    /**
     * انتقال یک آیتم به ابتدای صف.
     */
    fun moveToTop(
        context: Context,
        index: Int
    ): Boolean {

        if (
            index <= 0
        ) {
            return false
        }

        return move(
            context,
            index,
            0
        )
    }

    /**
     * انتقال یک آیتم به انتهای صف.
     */
    fun moveToBottom(
        context: Context,
        index: Int
    ): Boolean {

        val queue =
            getQueue(context)

        if (
            index < 0 ||
            index >= queue.lastIndex
        ) {
            return false
        }

        return move(
            context,
            index,
            queue.lastIndex
        )
    }

    /**
     * پاک کردن کامل صف.
     */
    fun clear(
        context: Context
    ) {

        prefs(context)
            .edit()
            .remove(KEY_QUEUE)
            .putInt(
                KEY_CURRENT_INDEX,
                -1
            )
            .apply()
    }

    /**
     * آیا صف خالی است؟
     */
    fun isEmpty(
        context: Context
    ): Boolean {

        return getQueue(
            context
        ).isEmpty()
    }

    /**
     * تعداد آیتم‌های صف.
     */
    fun size(
        context: Context
    ): Int {

        return getQueue(
            context
        ).size
    }

    /**
     * بررسی وجود ویدئو در صف.
     */
    fun contains(
        context: Context,
        uri: Uri
    ): Boolean {

        return getQueue(
            context
        ).any {

            it.toString() ==
                uri.toString()
        }
    }

    /**
     * دریافت اندیس ویدئوی فعلی.
     */
    fun getCurrentIndex(
        context: Context
    ): Int {

        val queue =
            getQueue(context)

        if (
            queue.isEmpty()
        ) {

            return -1
        }

        val stored =
            prefs(context)
                .getInt(
                    KEY_CURRENT_INDEX,
                    0
                )

        return stored.coerceIn(
            0,
            queue.lastIndex
        )
    }

    /**
     * تعیین اندیس ویدئوی فعلی.
     */
    fun setCurrentIndex(
        context: Context,
        index: Int
    ) {

        val queue =
            getQueue(context)

        val normalized =
            if (
                queue.isEmpty()
            ) {
                -1
            } else {
                index.coerceIn(
                    0,
                    queue.lastIndex
                )
            }

        prefs(context)
            .edit()
            .putInt(
                KEY_CURRENT_INDEX,
                normalized
            )
            .apply()
    }

    /**
     * دریافت ویدئوی فعلی صف.
     */
    fun getCurrent(
        context: Context
    ): Uri? {

        val queue =
            getQueue(context)

        if (
            queue.isEmpty()
        ) {

            return null
        }

        val index =
            getCurrentIndex(
                context
            )

        if (
            index !in queue.indices
        ) {

            return null
        }

        return queue[index]
    }

    /**
     * دریافت ویدئوی بعدی صف.
     *
     * wrapAround:
     * اگر true باشد، بعد از آخرین آیتم
     * دوباره به اولین آیتم می‌رود.
     */
    fun getNext(
        context: Context,
        wrapAround: Boolean = false
    ): Uri? {

        val queue =
            getQueue(context)

        if (
            queue.isEmpty()
        ) {

            return null
        }

        val current =
            getCurrentIndex(
                context
            )

        if (
            current < 0
        ) {

            return queue.first()
        }

        val next =
            current + 1

        if (
            next <= queue.lastIndex
        ) {

            return queue[next]
        }

        return if (
            wrapAround
        ) {
            queue.first()
        } else {
            null
        }
    }

    /**
     * دریافت ویدئوی قبلی صف.
     */
    fun getPrevious(
        context: Context,
        wrapAround: Boolean = false
    ): Uri? {

        val queue =
            getQueue(context)

        if (
            queue.isEmpty()
        ) {

            return null
        }

        val current =
            getCurrentIndex(
                context
            )

        if (
            current <= 0
        ) {

            return if (
                wrapAround
            ) {
                queue.last()
            } else {
                null
            }
        }

        return queue[
            current - 1
        ]
    }

    /**
     * حرکت به آیتم بعدی.
     *
     * خروجی:
     * Uri ویدئوی بعدی
     * یا null در انتهای صف.
     */
    fun moveToNext(
        context: Context,
        wrapAround: Boolean = false
    ): Uri? {

        val next =
            getNext(
                context,
                wrapAround
            )
                ?: return null

        val queue =
            getQueue(context)

        val index =
            queue.indexOfFirst {
                it.toString() ==
                    next.toString()
            }

        if (
            index >= 0
        ) {

            setCurrentIndex(
                context,
                index
            )
        }

        return next
    }

    /**
     * حرکت به آیتم قبلی.
     */
    fun moveToPrevious(
        context: Context,
        wrapAround: Boolean = false
    ): Uri? {

        val previous =
            getPrevious(
                context,
                wrapAround
            )
                ?: return null

        val queue =
            getQueue(context)

        val index =
            queue.indexOfFirst {
                it.toString() ==
                    previous.toString()
            }

        if (
            index >= 0
        ) {

            setCurrentIndex(
                context,
                index
            )
        }

        return previous
    }

    /**
     * قرار دادن یک ویدئو به عنوان ویدئوی فعلی.
     */
    fun setCurrentVideo(
        context: Context,
        uri: Uri
    ): Boolean {

        val queue =
            getQueue(context)

        val index =
            queue.indexOfFirst {
                it.toString() ==
                    uri.toString()
            }

        if (
            index < 0
        ) {

            return false
        }

        setCurrentIndex(
            context,
            index
        )

        return true
    }

    /**
     * افزودن ویدئو و تعیین آن به عنوان فعلی.
     */
    fun addAndSetCurrent(
        context: Context,
        uri: Uri
    ): Boolean {

        val added =
            add(
                context,
                uri
            )

        setCurrentVideo(
            context,
            uri
        )

        return added
    }

    /**
     * مرتب‌سازی صف بر اساس ترتیب نام.
     */
    fun sortByName(
        context: Context
    ) {

        val queue =
            getQueue(context)

        if (
            queue.isEmpty()
        ) {
            return
        }

        val current =
            getCurrent(context)

        val sorted =
            queue.sortedBy {
                it.lastPathSegment
                    ?.lowercase()
                    ?: it.toString()
                        .lowercase()
            }

        setQueue(
            context,
            sorted
        )

        if (
            current != null
        ) {

            setCurrentVideo(
                context,
                current
            )
        }
    }

    /**
     * حذف آیتم‌های تکراری از صف.
     */
    fun removeDuplicates(
        context: Context
    ) {

        val queue =
            getQueue(context)

        val current =
            getCurrent(context)

        val unique =
            LinkedHashSet<String>()

        val cleaned =
            mutableListOf<Uri>()

        queue.forEach { uri ->

            if (
                unique.add(
                    uri.toString()
                )
            ) {

                cleaned.add(uri)
            }
        }

        setQueue(
            context,
            cleaned
        )

        if (
            current != null
        ) {

            setCurrentVideo(
                context,
                current
            )
        }
    }

    private fun normalizeCurrentIndex(
        context: Context
    ) {

        val queue =
            getQueue(context)

        if (
            queue.isEmpty()
        ) {

            setCurrentIndex(
                context,
                -1
            )

            return
        }

        val stored =
            prefs(context)
                .getInt(
                    KEY_CURRENT_INDEX,
                    0
                )

        val normalized =
            stored.coerceIn(
                0,
                queue.lastIndex
            )

        if (
            normalized != stored
        ) {

            setCurrentIndex(
                context,
                normalized
            )
        }
    }
}
