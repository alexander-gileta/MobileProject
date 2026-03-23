package com.example.taskapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskapp.data.news.NewsDataSource
import com.example.taskapp.data.news.NewsRepository
import com.example.taskapp.model.news.NewsArticle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {
    var uiState by mutableStateOf(NewsUiState(isLoading = true))
        private set

    private var debugRequestSent = false

    init {
        viewModelScope.launch {
            repository.cleanupExpiredNews()
            showCachedNewsIfAvailable()
            startAutoRefreshLoop()
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            fetchNews(forceRefresh = true)
        }
    }

    private suspend fun startAutoRefreshLoop() {
        while (currentCoroutineContext().isActive) {
            fetchNews(forceRefresh = false)
            if (debugRequestSent.not()) {
                debugRequestSent = true
                viewModelScope.launch {
                    runCatching { repository.sendDebugRequest() }
                }
            }
            delay(AUTO_REFRESH_DELAY_MS)
        }
    }

    private suspend fun showCachedNewsIfAvailable() {
        val cachedNews = repository.getCachedNews() ?: return
        uiState = uiState.copy(
            articles = cachedNews.articles,
            isLoading = false,
            isShowingCachedData = true,
            isCacheStale = cachedNews.isStale,
            lastUpdatedMillis = cachedNews.cachedAtMillis,
            errorMessage = null
        )
    }

    private suspend fun fetchNews(forceRefresh: Boolean) {
        val currentState = uiState
        uiState = currentState.copy(
            isLoading = currentState.articles.isEmpty(),
            isRefreshing = currentState.articles.isNotEmpty(),
            errorMessage = null
        )

        runCatching {
            repository.getLatestNews(forceRefresh = forceRefresh)
        }.onSuccess { result ->
            uiState = uiState.copy(
                articles = result.articles,
                isLoading = false,
                isRefreshing = false,
                isShowingCachedData = result.source == NewsDataSource.CACHE,
                isCacheStale = result.isStale,
                lastUpdatedMillis = result.cachedAtMillis,
                errorMessage = null
            )
        }.onFailure { error ->
            val fallbackMessage = error.message ?: "Не удалось загрузить новости"
            uiState = uiState.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = fallbackMessage
            )
        }
    }

    companion object {
        private const val AUTO_REFRESH_DELAY_MS = 120_000L

        fun factory(repository: NewsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                        return NewsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isShowingCachedData: Boolean = false,
    val isCacheStale: Boolean = false,
    val lastUpdatedMillis: Long? = null,
    val errorMessage: String? = null
)
