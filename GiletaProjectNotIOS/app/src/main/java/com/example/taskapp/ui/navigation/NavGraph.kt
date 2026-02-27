package com.example.taskapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskapp.ui.screens.AddTaskScreen
import com.example.taskapp.ui.screens.MainScreen
import com.example.taskapp.viewmodel.TasksViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object AddTask : Screen("add_task")
}

@Composable
fun AppNav(viewModel: TasksViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) { MainScreen(navController, viewModel) }
        composable(Screen.AddTask.route) { AddTaskScreen(navController, viewModel) }
    }
}
