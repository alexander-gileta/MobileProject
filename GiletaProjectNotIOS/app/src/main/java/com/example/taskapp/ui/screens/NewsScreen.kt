package com.example.taskapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.taskapp.ui.components.NewsCard
import com.example.taskapp.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewsScreen(viewModel: NewsViewModel, modifier: Modifier = Modifier) {
    val uiState = viewModel.uiState
    val errorMessage = uiState.errorMessage

    when {
        uiState.isLoading && uiState.articles.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        errorMessage is String && uiState.articles.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = viewModel::refreshNow) {
                        Text("Повторить")
                    }
                }
            }
        }

        uiState.articles.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Новостей пока нет",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        else -> {
            Column(modifier = modifier.fillMaxSize()) {
                if (uiState.isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Лента новостей",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                uiState.lastUpdatedMillis?.let { lastUpdatedMillis ->
                                    Text(
                                        text = "Обновлено: ${formatLastUpdated(lastUpdatedMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedButton(onClick = viewModel::refreshNow) {
                                Text("Обновить")
                            }
                        }
                    }

                    if (uiState.isShowingCachedData) {
                        item {
                            StatusBanner(
                                text = if (uiState.isCacheStale) {
                                    "Показаны кэшированные данные. Сеть недоступна или ответ устарел, поэтому отображается последняя сохранённая версия."
                                } else {
                                    "Показаны кэшированные данные. Это ускоряет открытие экрана и убирает рывки интерфейса."
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (errorMessage is String && uiState.articles.isNotEmpty()) {
                        item {
                            StatusBanner(
                                text = errorMessage,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    items(
                        items = uiState.articles,
                        key = { article -> article.url }
                    ) { article ->
                        NewsCard(article = article)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

private fun formatLastUpdated(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}
