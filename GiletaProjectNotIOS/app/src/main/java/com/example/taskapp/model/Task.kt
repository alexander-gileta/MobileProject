package com.example.taskapp.model

import java.util.*

data class Task(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String? = null,
    val priority: Int = 1,
    val flag: Boolean = false,
    val dueDate: Date? = null,
    var isCompleted: Boolean = false
)
