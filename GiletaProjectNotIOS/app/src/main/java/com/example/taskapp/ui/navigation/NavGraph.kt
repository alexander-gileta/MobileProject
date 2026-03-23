package com.example.taskapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskapp.ui.screens.AddTaskScreen
import com.example.taskapp.ui.screens.MainScreen
import com.example.taskapp.viewmodel.NewsViewModel
import com.example.taskapp.viewmodel.TasksViewModel

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object AddTask : Screen("add_task")
}

@Composable
fun AppNav(tasksViewModel: TasksViewModel, newsViewModel: NewsViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                navController = navController,
                tasksViewModel = tasksViewModel,
                newsViewModel = newsViewModel
            )
        }
        composable(Screen.AddTask.route) {
            AddTaskScreen(navController = navController, viewModel = tasksViewModel)
        }
    }
}
