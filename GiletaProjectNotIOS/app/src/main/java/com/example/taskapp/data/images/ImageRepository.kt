package com.example.taskapp.data.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ImageRepository(
    private val imageCacheService: ImageCacheService,
    private val httpClient: OkHttpClient
) {
    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    suspend fun loadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(url)?.let { bitmap ->
            return@withContext bitmap
        }

        imageCacheService.findCachedFile(url)?.let { file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                memoryCache.put(url, bitmap)
                return@withContext bitmap
            }
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        runCatching {
            httpClient.newCall(request).execute()
        }.getOrNull()?.use { response ->
            if (response.isSuccessful.not()) {
                return@withContext null
            }

            val responseBody = response.body ?: return@withContext null
            val bytes = responseBody.bytes()
            if (bytes.isEmpty()) {
                return@withContext null
            }

            val mimeType = responseBody.contentType()?.toString().orEmpty()
            imageCacheService.saveImage(url = url, bytes = bytes, mimeType = mimeType)

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                memoryCache.put(url, bitmap)
            }
            return@withContext bitmap
        }

        null
    }

    suspend fun trimCache() {
        imageCacheService.trimCache()
    }

    companion object {
        private const val MEMORY_CACHE_SIZE_KB = 12 * 1024
    }
}
