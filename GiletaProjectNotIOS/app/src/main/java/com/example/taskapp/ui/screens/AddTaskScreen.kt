package com.example.taskapp.ui.screens

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.taskapp.model.Task
import com.example.taskapp.viewmodel.TasksViewModel
import java.util.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(navController: NavHostController, viewModel: TasksViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) }
    var flag by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<Date?>(null) }
    var enableDate by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать задачу") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название задачи") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание задачи (опционально)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Приоритет")
                Row {
                    (1..3).forEach { p ->
                        val isSelected = priority == p

                        Button(
                            onClick = { priority = p },
                            modifier = Modifier.alpha(if (isSelected) 1f else 0.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("$p")
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = flag, onCheckedChange = { flag = it })
                    Text("Флаг")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Срок выполнения")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = enableDate, onCheckedChange = { enableDate = it })
                }
                if (enableDate) {
                    Button(onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(year, month, dayOfMonth)
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        dueDate = calendar.time
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Text(dueDate?.let { dateFormat.format(it) } ?: "Выбрать дату")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(
                                Task(
                                    title = title,
                                    description = description.ifBlank { null },
                                    priority = priority,
                                    flag = flag,
                                    dueDate = if (enableDate) dueDate else null
                                )
                            )
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }
            }
        }
    )
}
