package com.example.taskapp.model.news

data class NewsArticle(
    val title: String,
    val abstract: String,
    val source: String,
    val section: String,
    val publishedDate: String,
    val url: String,
    val imageUrl: String?
)
