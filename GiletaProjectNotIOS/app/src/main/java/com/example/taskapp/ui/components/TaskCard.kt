package com.example.taskapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskapp.model.Task

@Composable
fun TaskCard(task: Task, onCheckedChange: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onCheckedChange() }
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                task.description?.let { Text(it) }
                Text("Приоритет: ${task.priority}")
                if (task.flag) Text("Флаг")
                task.dueDate?.let { Text("Срок: $it") }
            }
        }
    }
}

