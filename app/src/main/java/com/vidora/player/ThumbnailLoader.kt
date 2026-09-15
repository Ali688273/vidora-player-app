package com.vidora.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

object ThumbnailLoader {

    private val executor = Executors.newFixedThreadPool(2)

    private val memoryCache =
        object : LruCache<String, Bitmap>(20 * 1024 * 1024) {

            override fun sizeOf(
                key: String,
                bitmap: Bitmap
            ): Int {
                return bitmap.byteCount
            }
        }

    fun load(
        context: Context,
        uri: Uri,
        imageView: ImageView
    ) {
        val key = uri.toString()

        val cached = memoryCache.get(key)

        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        imageView.tag = key

        executor.execute {
            val bitmap = createThumbnail(
                context,
                uri
            )

            if (bitmap != null) {
                memoryCache.put(
                    key,
                    bitmap
                )
            }

            imageView.post {
                if (imageView.tag == key) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(
                            android.R.color.transparent
                        )
                    }
                }
            }
        }
    }

    private fun createThumbnail(
        context: Context,
        uri: Uri
    ): Bitmap? {

        val retriever =
            MediaMetadataRetriever()

        return try {

            retriever.setDataSource(
                context,
                uri
            )

            retriever.getFrameAtTime(
                1_000_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

        } catch (_: Exception) {

            null

        } finally {

            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
