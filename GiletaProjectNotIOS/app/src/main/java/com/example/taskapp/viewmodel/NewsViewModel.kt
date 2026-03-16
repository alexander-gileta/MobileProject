package com.example.taskapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapp.data.news.NewsRepository
import com.example.taskapp.model.news.NewsArticle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class NewsViewModel : ViewModel() {
    private val repository = NewsRepository()

    var uiState by mutableStateOf(NewsUiState(isLoading = true))
        private set

    init {
        startAutoRefresh()
    }

    fun refreshNow() {
        viewModelScope.launch {
            fetchNews(initialLoad = uiState.articles.isEmpty())
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            var initialLoad = true
            while (currentCoroutineContext().isActive) {
                fetchNews(initialLoad = initialLoad)
                if (initialLoad) {
                    launch {
                        runCatching { repository.sendDebugRequest() }
                    }
                    initialLoad = false
                }
                delay(AUTO_REFRESH_DELAY_MS)
            }
        }
    }

    private suspend fun fetchNews(initialLoad: Boolean) {
        uiState = uiState.copy(
            isLoading = initialLoad,
            isRefreshing = initialLoad == false,
            errorMessage = null
        )

        runCatching { repository.getLatestNews() }
            .onSuccess { news ->
                uiState = uiState.copy(
                    articles = news,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
            .onFailure { error ->
                val message = error.message ?: "Не удалось загрузить новости"
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = message
                )
            }
    }

    companion object {
        private const val AUTO_REFRESH_DELAY_MS = 120_000L
    }
}

data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
