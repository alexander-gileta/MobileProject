package com.example.taskapp.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskapp.TaskApp
import com.example.taskapp.data.images.ImageRepository
import com.example.taskapp.model.news.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NewsCard(article: NewsArticle, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dateText = formatNewsDate(article.publishedDate)
    val sourceText = buildString {
        append(article.source)
        if (article.section.isNotBlank()) {
            append(" • ")
            append(article.section)
        }
        if (dateText.isNotBlank()) {
            append(" • ")
            append(dateText)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            NewsImage(
                imageUrl = article.imageUrl,
                imageRepository = (context.applicationContext as TaskApp).appContainer.imageRepository,
                modifier = Modifier
                    .size(width = 110.dp, height = 110.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (article.abstract.isNotBlank()) {
                    Text(
                        text = article.abstract,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NewsImage(
    imageUrl: String?,
    imageRepository: ImageRepository,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) {
        NewsImagePlaceholder(modifier = modifier)
        return
    }

    val imageState by produceState<NewsImageState>(
        initialValue = NewsImageState.Loading,
        key1 = imageUrl
    ) {
        value = withContext(Dispatchers.IO) {
            val bitmap = imageRepository.loadImage(imageUrl)
            if (bitmap != null) {
                NewsImageState.Success(bitmap)
            } else {
                NewsImageState.Error
            }
        }
    }

    when (val currentState = imageState) {
        NewsImageState.Error -> NewsImagePlaceholder(modifier = modifier)
        NewsImageState.Loading -> NewsImageLoading(modifier = modifier)
        is NewsImageState.Success -> {
            Image(
                bitmap = currentState.bitmap.asImageBitmap(),
                contentDescription = "Превью новости",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun NewsImageLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp)
    }
}

@Composable
private fun NewsImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = "Нет изображения",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNewsDate(rawDate: String): String {
    if (rawDate.isBlank()) {
        return ""
    }

    val inputPattern = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    val outputPattern = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val parsedDate = runCatching { inputPattern.parse(rawDate) }.getOrNull()

    return parsedDate?.let { date -> outputPattern.format(date) } ?: rawDate
}

private sealed interface NewsImageState {
    data object Loading : NewsImageState
    data object Error : NewsImageState
    data class Success(val bitmap: Bitmap) : NewsImageState
}
