package com.example.taskapp.data.images

import android.content.ContentValues
import android.content.Context
import com.example.taskapp.data.local.AppCacheDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class ImageCacheService(
    context: Context,
    private val databaseHelper: AppCacheDatabaseHelper
) {
    private val imagesDirectory: File = File(context.cacheDir, IMAGES_DIRECTORY_NAME).apply {
        mkdirs()
    }

    suspend fun findCachedFile(url: String, nowMillis: Long = System.currentTimeMillis()): File? =
        withContext(Dispatchers.IO) {
            val database = databaseHelper.writableDatabase
            var entry: ImageCacheEntry? = null

            database.query(
                AppCacheDatabaseHelper.TABLE_IMAGE_CACHE,
                arrayOf(
                    COLUMN_URL,
                    COLUMN_FILE_NAME,
                    COLUMN_MIME_TYPE,
                    COLUMN_SIZE_BYTES,
                    COLUMN_UPDATED_AT,
                    COLUMN_LAST_ACCESSED_AT
                ),
                "$COLUMN_URL = ?",
                arrayOf(url),
                null,
                null,
                null,
                "1"
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    entry = ImageCacheEntry(
                        url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)),
                        fileName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)),
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIME_TYPE)),
                        sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SIZE_BYTES)),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)),
                        lastAccessedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_ACCESSED_AT))
                    )
                }
            }

            val cachedEntry = entry ?: return@withContext null
            if (nowMillis - cachedEntry.updatedAt > MAX_IMAGE_AGE_MILLIS) {
                deleteEntry(cachedEntry)
                return@withContext null
            }

            val file = File(imagesDirectory, cachedEntry.fileName)
            if (file.exists().not()) {
                database.delete(
                    AppCacheDatabaseHelper.TABLE_IMAGE_CACHE,
                    "$COLUMN_URL = ?",
                    arrayOf(url)
                )
                return@withContext null
            }

            database.update(
                AppCacheDatabaseHelper.TABLE_IMAGE_CACHE,
                ContentValues().apply {
                    put(COLUMN_LAST_ACCESSED_AT, nowMillis)
                },
                "$COLUMN_URL = ?",
                arrayOf(url)
            )

            file
        }

    suspend fun saveImage(
        url: String,
        bytes: ByteArray,
        mimeType: String,
        nowMillis: Long = System.currentTimeMillis()
    ): File? = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) {
            return@withContext null
        }

        imagesDirectory.mkdirs()
        val fileName = buildFileName(url = url, mimeType = mimeType)
        val tempFile = File(imagesDirectory, "$fileName.tmp")
        tempFile.outputStream().use { stream ->
            stream.write(bytes)
            stream.flush()
        }

        val finalFile = File(imagesDirectory, fileName)
        if (finalFile.exists()) {
            finalFile.delete()
        }
        val renamed = tempFile.renameTo(finalFile)
        if (renamed.not()) {
            tempFile.copyTo(finalFile, overwrite = true)
            tempFile.delete()
        }

        val values = ContentValues().apply {
            put(COLUMN_URL, url)
            put(COLUMN_FILE_NAME, fileName)
            put(COLUMN_MIME_TYPE, mimeType)
            put(COLUMN_SIZE_BYTES, finalFile.length())
            put(COLUMN_UPDATED_AT, nowMillis)
            put(COLUMN_LAST_ACCESSED_AT, nowMillis)
        }

        databaseHelper.writableDatabase.insertWithOnConflict(
            AppCacheDatabaseHelper.TABLE_IMAGE_CACHE,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        trimCache(nowMillis)
        finalFile
    }

    suspend fun trimCache(nowMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val database = databaseHelper.writableDatabase
        val expiredBoundary = nowMillis - MAX_IMAGE_AGE_MILLIS

        val expiredEntries = mutableListOf<ImageCacheEntry>()
        database.query(
            AppCacheDatabaseHelper.TABLE_IMAGE_CACHE,
            arrayOf(
                COLUMN_URL,
                COLUMN_FILE_NAME,
                COLUMN_MIME_TYPE,
                COLUMN_SIZE_BYTES,
                COLUMN_UPDATED_AT,
                COLUMN_LAST_ACCESSED_AT
            ),
            "$COLUMN_UPDATED_AT < ?",
            arrayOf(expiredBoundary.toString()),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                expiredEntries += ImageCacheEntry(
                    url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)),
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)),
                    mimeType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIME_TYPE)),
                    sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SIZE_BYTES)),
                    updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)),
                    lastAccessedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_ACCESSED_AT))
                )
            }
        }
        expiredEntries.forEach { entry -> deleteEntry(entry) }

        val allEntries = mutableListOf<ImageCacheEntry>()
        database.query(
            AppCacheDatabaseHelper.TABLE_IMAGE_CACHE,
            arrayOf(
                COLUMN_URL,
                COLUMN_FILE_NAME,
                COLUMN_MIME_TYPE,
                COLUMN_SIZE_BYTES,
                COLUMN_UPDATED_AT,
                COLUMN_LAST_ACCESSED_AT
            ),
            null,
            null,
            null,
            null,
            "$COLUMN_LAST_ACCESSED_AT ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                allEntries += ImageCacheEntry(
                    url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)),
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)),
                    mimeType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIME_TYPE)),
                    sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SIZE_BYTES)),
                    updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)),
                    lastAccessedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_ACCESSED_AT))
                )
            }
        }

        var totalSizeBytes = allEntries.sumOf { entry -> entry.sizeBytes }
        var totalFiles = allEntries.size

        allEntries.forEach { entry ->
            val shouldDelete = totalSizeBytes > MAX_TOTAL_SIZE_BYTES || totalFiles > MAX_FILE_COUNT
            if (shouldDelete) {
                deleteEntry(entry)
                totalSizeBytes -= entry.sizeBytes
                totalFiles -= 1
            }
        }
    }

    private fun deleteEntry(entry: ImageCacheEntry) {
        val file = File(imagesDirectory, entry.fileName)
        if (file.exists()) {
            file.delete()
        }
        databaseHelper.writableDatabase.delete(
            AppCacheDatabaseHelper.TABLE_IMAGE_CACHE,
            "$COLUMN_URL = ?",
            arrayOf(entry.url)
        )
    }

    private fun buildFileName(url: String, mimeType: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(url.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(Locale.US, byte) }

        val extension = when {
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            mimeType.contains("jpeg", ignoreCase = true) -> "jpg"
            mimeType.contains("jpg", ignoreCase = true) -> "jpg"
            else -> "img"
        }

        return "$hash.$extension"
    }

    companion object {
        private const val IMAGES_DIRECTORY_NAME = "news_image_cache"
        private const val MAX_IMAGE_AGE_MILLIS = 7L * 24L * 60L * 60L * 1000L
        private const val MAX_TOTAL_SIZE_BYTES = 50L * 1024L * 1024L
        private const val MAX_FILE_COUNT = 120

        private const val COLUMN_URL = "url"
        private const val COLUMN_FILE_NAME = "file_name"
        private const val COLUMN_MIME_TYPE = "mime_type"
        private const val COLUMN_SIZE_BYTES = "size_bytes"
        private const val COLUMN_UPDATED_AT = "updated_at"
        private const val COLUMN_LAST_ACCESSED_AT = "last_accessed_at"
    }
}

data class ImageCacheEntry(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long
)
