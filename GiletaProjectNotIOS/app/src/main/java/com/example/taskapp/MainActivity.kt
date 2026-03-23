package com.example.taskapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.taskapp.ui.navigation.AppNav
import com.example.taskapp.ui.theme.TaskAppTheme
import com.example.taskapp.viewmodel.NewsViewModel
import com.example.taskapp.viewmodel.TasksViewModel

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { (application as TaskApp).appContainer }

    private val tasksViewModel: TasksViewModel by viewModels()
    private val newsViewModel: NewsViewModel by viewModels {
        NewsViewModel.factory(appContainer.newsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskAppTheme {
                AppNav(
                    tasksViewModel = tasksViewModel,
                    newsViewModel = newsViewModel
                )
            }
        }
    }
}
