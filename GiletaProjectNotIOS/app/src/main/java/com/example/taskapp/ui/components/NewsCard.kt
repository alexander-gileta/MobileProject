package com.example.taskapp.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.taskapp.model.news.NewsArticle
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
private fun NewsImage(imageUrl: String?, modifier: Modifier = Modifier) {
    if (imageUrl.isNullOrBlank()) {
        NewsImagePlaceholder(modifier = modifier)
        return
    }

    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Превью новости",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "...")
            }
        },
        error = {
            NewsImagePlaceholder(modifier = Modifier.matchParentSize())
        }
    )
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
