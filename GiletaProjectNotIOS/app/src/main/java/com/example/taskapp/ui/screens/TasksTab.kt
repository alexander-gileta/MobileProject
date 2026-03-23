package com.example.taskapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.taskapp.viewmodel.TasksViewModel
import com.example.taskapp.components.TaskCard

@Composable
fun TasksTab(viewModel: TasksViewModel, modifier: Modifier = Modifier) {
    val tasks = viewModel.tasks

    if (tasks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Задач нет")
        }
    } else {
        val grouped = tasks
            .sortedWith(compareByDescending<com.example.taskapp.model.Task> { it.priority }.thenBy { it.title })
            .groupBy { it.priority }

        val priorities = grouped.keys.sortedDescending()

        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            priorities.forEach { priority ->
                val groupTasks = grouped[priority].orEmpty()
                if (groupTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Приоритет $priority",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(groupTasks) { task ->
                        TaskCard(task) { viewModel.toggleTaskCompletion(task) }
                    }
                }
            }
        }
    }
}
