package com.example.taskapp.data.news.cache

import android.content.ContentValues
import com.example.taskapp.data.local.AppCacheDatabaseHelper
import com.example.taskapp.model.news.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsCacheService(
    private val databaseHelper: AppCacheDatabaseHelper
) {
    suspend fun readSnapshot(): CachedNewsSnapshot? = withContext(Dispatchers.IO) {
        val database = databaseHelper.readableDatabase
        val articles = mutableListOf<NewsArticle>()
        var cachedAtMillis: Long? = null

        database.query(
            AppCacheDatabaseHelper.TABLE_NEWS_CACHE,
            arrayOf(
                COLUMN_TITLE,
                COLUMN_ABSTRACT,
                COLUMN_SOURCE,
                COLUMN_SECTION,
                COLUMN_PUBLISHED_DATE,
                COLUMN_URL,
                COLUMN_IMAGE_URL,
                COLUMN_CACHED_AT
            ),
            null,
            null,
            null,
            null,
            "$COLUMN_CACHED_AT DESC, $COLUMN_PUBLISHED_DATE DESC"
        ).use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            val abstractIndex = cursor.getColumnIndexOrThrow(COLUMN_ABSTRACT)
            val sourceIndex = cursor.getColumnIndexOrThrow(COLUMN_SOURCE)
            val sectionIndex = cursor.getColumnIndexOrThrow(COLUMN_SECTION)
            val publishedDateIndex = cursor.getColumnIndexOrThrow(COLUMN_PUBLISHED_DATE)
            val urlIndex = cursor.getColumnIndexOrThrow(COLUMN_URL)
            val imageUrlIndex = cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL)
            val cachedAtIndex = cursor.getColumnIndexOrThrow(COLUMN_CACHED_AT)

            while (cursor.moveToNext()) {
                if (cachedAtMillis == null) {
                    cachedAtMillis = cursor.getLong(cachedAtIndex)
                }

                articles += NewsArticle(
                    title = cursor.getString(titleIndex),
                    abstract = cursor.getString(abstractIndex),
                    source = cursor.getString(sourceIndex),
                    section = cursor.getString(sectionIndex),
                    publishedDate = cursor.getString(publishedDateIndex),
                    url = cursor.getString(urlIndex),
                    imageUrl = cursor.getString(imageUrlIndex)
                )
            }
        }

        val snapshotCachedAt = cachedAtMillis
        if (articles.isEmpty() || snapshotCachedAt == null) {
            null
        } else {
            CachedNewsSnapshot(
                articles = articles,
                cachedAtMillis = snapshotCachedAt
            )
        }
    }

    suspend fun replaceAll(articles: List<NewsArticle>, cachedAtMillis: Long) =
        withContext(Dispatchers.IO) {
            val database = databaseHelper.writableDatabase
            database.beginTransaction()
            try {
                database.delete(AppCacheDatabaseHelper.TABLE_NEWS_CACHE, null, null)
                articles.forEach { article ->
                    val values = ContentValues().apply {
                        put(COLUMN_URL, article.url)
                        put(COLUMN_TITLE, article.title)
                        put(COLUMN_ABSTRACT, article.abstract)
                        put(COLUMN_SOURCE, article.source)
                        put(COLUMN_SECTION, article.section)
                        put(COLUMN_PUBLISHED_DATE, article.publishedDate)
                        put(COLUMN_IMAGE_URL, article.imageUrl)
                        put(COLUMN_CACHED_AT, cachedAtMillis)
                    }
                    database.insertWithOnConflict(
                        AppCacheDatabaseHelper.TABLE_NEWS_CACHE,
                        null,
                        values,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        databaseHelper.writableDatabase.delete(AppCacheDatabaseHelper.TABLE_NEWS_CACHE, null, null)
    }

    suspend fun removeExpired(maxAgeMillis: Long, nowMillis: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            val expirationBoundary = nowMillis - maxAgeMillis
            databaseHelper.writableDatabase.delete(
                AppCacheDatabaseHelper.TABLE_NEWS_CACHE,
                "$COLUMN_CACHED_AT < ?",
                arrayOf(expirationBoundary.toString())
            )
        }

    companion object {
        private const val COLUMN_URL = "url"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_ABSTRACT = "abstract_text"
        private const val COLUMN_SOURCE = "source"
        private const val COLUMN_SECTION = "section"
        private const val COLUMN_PUBLISHED_DATE = "published_date"
        private const val COLUMN_IMAGE_URL = "image_url"
        private const val COLUMN_CACHED_AT = "cached_at"
    }
}

data class CachedNewsSnapshot(
    val articles: List<NewsArticle>,
    val cachedAtMillis: Long
)
