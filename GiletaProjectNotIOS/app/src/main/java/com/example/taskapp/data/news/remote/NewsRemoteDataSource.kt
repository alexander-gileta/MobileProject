package com.example.taskapp.data.news.remote

import android.util.Log
import com.example.taskapp.BuildConfig
import com.example.taskapp.model.news.NewsArticle
import com.example.taskapp.network.news.DebugApiService
import com.example.taskapp.network.news.DebugRequestDto
import com.example.taskapp.network.news.NewsApiService
import com.example.taskapp.network.news.NewsItemDto

class NewsRemoteDataSource(
    private val newsApi: NewsApiService,
    private val debugApi: DebugApiService
) {
    suspend fun fetchLatestNews(): List<NewsArticle> {
        val apiKey = BuildConfig.NYT_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException("NYT_API_KEY is empty")
        }

        return newsApi
            .getRecentNews(apiKey = apiKey)
            .results
            .orEmpty()
            .mapNotNull { item -> item.toDomain() }
    }

    suspend fun sendDebugRequest() {
        val response = debugApi.sendDebugEvent(
            body = DebugRequestDto(
                title = "news_refresh_debug",
                body = "POST request with JSON body and headers completed",
                userId = 1
            )
        )
        Log.d(TAG, "Debug POST completed with id=${response.id ?: -1}")
    }

    private fun NewsItemDto.toDomain(): NewsArticle? {
        val articleTitle = title?.takeIf { value -> value.isNotBlank() } ?: return null
        val articleUrl = url?.takeIf { value -> value.isNotBlank() } ?: return null
        val articleSource = source?.takeIf { value -> value.isNotBlank() } ?: "Unknown source"
        val articleAbstract = abstract.orEmpty()
        val articleSection = section.orEmpty()
        val articlePublishedDate = published_date.orEmpty()

        val preferredFormats = setOf(
            "mediumThreeByTwo210",
            "Normal",
            "Large Thumbnail",
            "Standard Thumbnail"
        )

        val image = multimedia
            .orEmpty()
            .firstOrNull { media ->
                val currentUrl = media.url
                val currentFormat = media.format
                currentUrl.isNullOrBlank().not() && preferredFormats.contains(currentFormat)
            }
            ?.url
            ?: multimedia
                .orEmpty()
                .firstOrNull { media -> media.url.isNullOrBlank().not() }
                ?.url

        return NewsArticle(
            title = articleTitle,
            abstract = articleAbstract,
            source = articleSource,
            section = articleSection,
            publishedDate = articlePublishedDate,
            url = articleUrl,
            imageUrl = image
        )
    }

    companion object {
        private const val TAG = "NewsRemoteDataSource"
    }
}
