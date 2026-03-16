package com.example.taskapp.data.news

import android.util.Log
import com.example.taskapp.BuildConfig
import com.example.taskapp.model.news.NewsArticle
import com.example.taskapp.network.news.DebugApiService
import com.example.taskapp.network.news.DebugRequestDto
import com.example.taskapp.network.news.NewsApiService
import com.example.taskapp.network.news.NewsItemDto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewsRepository {
    private val httpClient = OkHttpClient.Builder().build()

    private val newsApi: NewsApiService = Retrofit.Builder()
        .baseUrl(NEWS_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NewsApiService::class.java)

    private val debugApi: DebugApiService = Retrofit.Builder()
        .baseUrl(DEBUG_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DebugApiService::class.java)

    suspend fun getLatestNews(): List<NewsArticle> {
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
                currentUrl.isNullOrBlank() == false && preferredFormats.contains(currentFormat)
            }
            ?.url
            ?: multimedia
                .orEmpty()
                .firstOrNull { media -> media.url.isNullOrBlank() == false }
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
        private const val NEWS_BASE_URL = "https://api.nytimes.com/"
        private const val DEBUG_BASE_URL = "https://jsonplaceholder.typicode.com/"
        private const val TAG = "NewsRepository"
    }
}
