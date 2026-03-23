package com.example.taskapp.di

import android.content.Context
import com.example.taskapp.data.images.ImageCacheService
import com.example.taskapp.data.images.ImageRepository
import com.example.taskapp.data.local.AppCacheDatabaseHelper
import com.example.taskapp.data.news.NewsRepository
import com.example.taskapp.data.news.cache.NewsCacheService
import com.example.taskapp.data.news.remote.NewsRemoteDataSource
import com.example.taskapp.network.news.DebugApiService
import com.example.taskapp.network.news.NewsApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = OkHttpClient.Builder().build()
    private val databaseHelper = AppCacheDatabaseHelper(appContext)

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

    private val newsRemoteDataSource = NewsRemoteDataSource(
        newsApi = newsApi,
        debugApi = debugApi
    )

    private val newsCacheService = NewsCacheService(databaseHelper)
    private val imageCacheService = ImageCacheService(
        context = appContext,
        databaseHelper = databaseHelper
    )

    val newsRepository: NewsRepository = NewsRepository(
        remoteDataSource = newsRemoteDataSource,
        cacheService = newsCacheService
    )

    val imageRepository: ImageRepository = ImageRepository(
        imageCacheService = imageCacheService,
        httpClient = httpClient
    )

    companion object {
        private const val NEWS_BASE_URL = "https://api.nytimes.com/"
        private const val DEBUG_BASE_URL = "https://jsonplaceholder.typicode.com/"
    }
}
