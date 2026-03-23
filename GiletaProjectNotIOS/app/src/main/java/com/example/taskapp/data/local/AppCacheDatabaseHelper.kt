package com.example.taskapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppCacheDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NEWS_CACHE (
                url TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                abstract_text TEXT NOT NULL,
                source TEXT NOT NULL,
                section TEXT NOT NULL,
                published_date TEXT NOT NULL,
                image_url TEXT,
                cached_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX idx_news_cached_at ON $TABLE_NEWS_CACHE(cached_at)"
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_IMAGE_CACHE (
                url TEXT PRIMARY KEY,
                file_name TEXT NOT NULL,
                mime_type TEXT NOT NULL,
                size_bytes INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                last_accessed_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX idx_image_last_accessed ON $TABLE_IMAGE_CACHE(last_accessed_at)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NEWS_CACHE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGE_CACHE")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "task_app_cache.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NEWS_CACHE = "news_cache"
        const val TABLE_IMAGE_CACHE = "image_cache"
    }
}
