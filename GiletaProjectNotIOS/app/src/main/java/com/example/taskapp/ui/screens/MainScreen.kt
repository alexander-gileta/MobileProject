package com.example.taskapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.taskapp.viewmodel.TasksViewModel
import com.example.taskapp.ui.screens.TasksTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: TasksViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Главная", "Задачи", "Записи")

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when (selectedTab) {
                0 -> Text("Главная", modifier = Modifier.padding(16.dp))
                1 -> TasksTab(viewModel, Modifier.fillMaxSize().padding(top = 72.dp))
                2 -> Text("Записи", modifier = Modifier.padding(16.dp))
            }

            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_task") },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Text("+")
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.height(60.dp),
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                }
            }
        }
    }
}
