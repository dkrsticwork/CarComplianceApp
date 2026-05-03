package com.carcomplianceapp.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.carcomplianceapp.ui.screens.main.actions.ActionsTab
import com.carcomplianceapp.ui.screens.main.garage.GarageTab
import com.carcomplianceapp.ui.screens.main.settings.SettingsTab
import com.carcomplianceapp.ui.screens.main.timeline.TimelineTab
import com.carcomplianceapp.ui.viewmodel.MainViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    TIMELINE("Timeline", Icons.Default.FormatListBulleted),
    ACTIONS("Actions",   Icons.Default.CheckCircle),
    GARAGE("Garage",    Icons.Default.Garage),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddCar: () -> Unit,
    onEditCar: (Long) -> Unit,
    onGoToApiKey: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(Tab.TIMELINE) }
    val cars by viewModel.cars.collectAsState()
    val activeCar by viewModel.activeCar.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                Tab.TIMELINE -> TimelineTab(
                    tasks = tasks,
                    activeCar = activeCar,
                    apiError = apiError,
                    isRefreshing = isRefreshing,
                    onRefresh = { activeCar?.let { viewModel.refreshTasks(it) } },
                    onMarkDone = { viewModel.markDone(it) },
                    onSnooze = { viewModel.snoozeTask(it) },
                    onEdit = { viewModel.editTask(it) },
                    onGoToApiKey = onGoToApiKey
                )
                Tab.ACTIONS -> ActionsTab(
                    tasks = tasks,
                    onMarkDone = { viewModel.markDone(it) },
                    onSnooze = { viewModel.snoozeTask(it) },
                    onEdit = { viewModel.editTask(it) }
                )
                Tab.GARAGE -> GarageTab(
                    cars = cars,
                    activeCar = activeCar,
                    tasks = tasks,
                    onSelectCar = { viewModel.selectCar(it) },
                    onAddCar = onAddCar,
                    onEditCar = onEditCar,
                    onDeleteCar = { viewModel.deleteCar(it) }
                )
                Tab.SETTINGS -> SettingsTab(
                    onGoToApiKey = onGoToApiKey,
                    apiError = apiError
                )
            }
        }
    }
}
