package com.example.taskapp.network.news

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface NewsApiService {
    @GET("svc/news/v3/content/all/all.json")
    suspend fun getRecentNews(
        @Query("api-key") apiKey: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): NewsResponseDto
}

interface DebugApiService {
    @Headers(
        "Content-Type: application/json; charset=UTF-8",
        "X-Debug-Source: TaskApp"
    )
    @POST("posts")
    suspend fun sendDebugEvent(@Body body: DebugRequestDto): DebugResponseDto
}

data class NewsResponseDto(
    val results: List<NewsItemDto>?
)

data class NewsItemDto(
    val title: String?,
    val abstract: String?,
    val source: String?,
    val section: String?,
    val published_date: String?,
    val url: String?,
    val multimedia: List<NewsMultimediaDto>?
)

data class NewsMultimediaDto(
    val url: String?,
    val format: String?
)

data class DebugRequestDto(
    val title: String,
    val body: String,
    val userId: Int
)

data class DebugResponseDto(
    val id: Int?,
    val title: String?,
    val body: String?,
    val userId: Int?
)
