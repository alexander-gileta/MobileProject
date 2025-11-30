package com.example.taskapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskapp.viewmodel.TasksViewModel
import com.example.taskapp.components.TaskCard

@Composable
fun TasksTab(viewModel: TasksViewModel, modifier: Modifier = Modifier) {
    if (viewModel.tasks.isEmpty()) {
        Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Задач нет")
        }
    } else {
        LazyColumn(
            modifier.fillMaxSize().padding(16.dp)
        ) {
            items(viewModel.tasks) { task ->
                TaskCard(task) { viewModel.toggleTaskCompletion(task) }
            }
        }
    }
}
