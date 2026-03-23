package com.example.taskapp.data.news

import com.example.taskapp.data.news.cache.CachedNewsSnapshot
import com.example.taskapp.data.news.cache.NewsCacheService
import com.example.taskapp.data.news.remote.NewsRemoteDataSource
import com.example.taskapp.model.news.NewsArticle

class NewsRepository(
    private val remoteDataSource: NewsRemoteDataSource,
    private val cacheService: NewsCacheService
) {
    suspend fun getCachedNews(): NewsLoadResult? {
        cleanupExpiredNews()
        val snapshot = cacheService.readSnapshot() ?: return null
        return snapshot.toLoadResult(source = NewsDataSource.CACHE)
    }

    suspend fun getLatestNews(forceRefresh: Boolean = false): NewsLoadResult {
        cleanupExpiredNews()

        val cachedSnapshot = cacheService.readSnapshot()
        if (!forceRefresh && cachedSnapshot?.isFresh() == true) {
            return cachedSnapshot.toLoadResult(source = NewsDataSource.CACHE)
        }

        return runCatching { remoteDataSource.fetchLatestNews() }
            .fold(
                onSuccess = { articles ->
                    val cachedAtMillis = System.currentTimeMillis()
                    cacheService.replaceAll(
                        articles = articles,
                        cachedAtMillis = cachedAtMillis
                    )
                    NewsLoadResult(
                        articles = articles,
                        source = NewsDataSource.NETWORK,
                        cachedAtMillis = cachedAtMillis,
                        isStale = false
                    )
                },
                onFailure = { error ->
                    if (cachedSnapshot != null) {
                        cachedSnapshot.toLoadResult(
                            source = NewsDataSource.CACHE,
                            isStaleOverride = true
                        )
                    } else {
                        throw error
                    }
                }
            )
    }

    suspend fun sendDebugRequest() {
        remoteDataSource.sendDebugRequest()
    }

    suspend fun cleanupExpiredNews() {
        cacheService.removeExpired(maxAgeMillis = MAX_CACHE_AGE_MILLIS)
    }

    private fun CachedNewsSnapshot.toLoadResult(
        source: NewsDataSource,
        isStaleOverride: Boolean? = null
    ): NewsLoadResult {
        val ageMillis = System.currentTimeMillis() - cachedAtMillis
        return NewsLoadResult(
            articles = articles,
            source = source,
            cachedAtMillis = cachedAtMillis,
            isStale = isStaleOverride ?: (ageMillis > FRESH_CACHE_AGE_MILLIS)
        )
    }

    private fun CachedNewsSnapshot?.isFresh(): Boolean {
        val snapshot = this ?: return false
        val ageMillis = System.currentTimeMillis() - snapshot.cachedAtMillis
        return ageMillis <= FRESH_CACHE_AGE_MILLIS
    }

    companion object {
        const val FRESH_CACHE_AGE_MILLIS = 5L * 60L * 1000L
        const val MAX_CACHE_AGE_MILLIS = 24L * 60L * 60L * 1000L
    }
}

data class NewsLoadResult(
    val articles: List<NewsArticle>,
    val source: NewsDataSource,
    val cachedAtMillis: Long,
    val isStale: Boolean
)

enum class NewsDataSource {
    NETWORK,
    CACHE
}
