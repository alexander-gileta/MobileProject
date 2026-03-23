package com.example.taskapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.taskapp.ui.navigation.Screen
import com.example.taskapp.viewmodel.NewsViewModel
import com.example.taskapp.viewmodel.TasksViewModel

@Composable
fun MainScreen(
    navController: NavHostController,
    tasksViewModel: TasksViewModel,
    newsViewModel: NewsViewModel
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Новости", "Задачи", "Записи")

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { navController.navigate(Screen.AddTask.route) }) {
                    Text("+")
                }
            }
        },
        bottomBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> NewsScreen(
                viewModel = newsViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            1 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text(
                    text = "Задачи",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                TasksTab(
                    viewModel = tasksViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Записи",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
