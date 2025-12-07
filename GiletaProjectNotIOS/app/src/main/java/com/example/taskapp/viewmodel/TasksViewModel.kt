package com.example.taskapp.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.taskapp.model.Task

class TasksViewModel : ViewModel() {
    val tasks = mutableStateListOf<Task>()

    fun addTask(task: Task) {
        tasks.add(task)
    }

    fun toggleTaskCompletion(task: Task) {
        val index = tasks.indexOf(task)
        if (index != -1) {
            tasks[index] = tasks[index].copy(isCompleted = !task.isCompleted)
        }
    }
}
