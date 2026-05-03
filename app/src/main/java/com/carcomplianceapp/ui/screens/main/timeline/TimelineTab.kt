package com.carcomplianceapp.ui.screens.main.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carcomplianceapp.domain.model.*
import com.carcomplianceapp.ui.components.*
import com.carcomplianceapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTab(
    tasks: List<ComplianceTask>,
    activeCar: Car?,
    apiError: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMarkDone: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onEdit: (ComplianceTask) -> Unit,
    onGoToApiKey: () -> Unit
) {
    val sorted = remember(tasks) {
        tasks.sortedWith(compareBy(
            { it.urgency.ordinal },
            { it.dueDate ?: java.time.LocalDate.MAX }
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activeCar?.nickname ?: "My Garage",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Obligation timeline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // API error banner
            apiError?.let { err ->
                item {
                    ApiErrorBanner(
                        errorMessage = err,
                        onGoToSettings = onGoToApiKey
                    )
                }
            }

            if (activeCar == null) {
                item {
                    EmptyState(
                        icon = Icons.Default.DirectionsCar,
                        title = "No car selected",
                        subtitle = "Add a car from the Garage tab"
                    )
                }
                return@LazyColumn
            }

            if (tasks.isEmpty() && !isRefreshing) {
                item {
                    EmptyState(
                        icon = Icons.Default.CheckCircleOutline,
                        title = "No tasks yet",
                        subtitle = "Tap refresh to generate your obligation list"
                    )
                }
                return@LazyColumn
            }

            if (isRefreshing && tasks.isEmpty()) {
                item { LoadingTaskPlaceholder() }
                return@LazyColumn
            }

            // Group by urgency
            val overdue = sorted.filter { it.urgency == UrgencyLevel.CRITICAL }
            val upcoming = sorted.filter { it.urgency != UrgencyLevel.CRITICAL && it.status != TaskStatus.DONE }
            val done = sorted.filter { it.status == TaskStatus.DONE }

            if (overdue.isNotEmpty()) {
                item { SectionHeader("Overdue") }
                items(overdue, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onMarkDone = { onMarkDone(task.id) },
                        onSnooze = { onSnooze(task.id) },
                        onEdit = { onEdit(task) }
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item { SectionHeader("Upcoming") }
                items(upcoming, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onMarkDone = { onMarkDone(task.id) },
                        onSnooze = { onSnooze(task.id) },
                        onEdit = { onEdit(task) }
                    )
                }
            }

            if (done.isNotEmpty()) {
                item { SectionHeader("Completed") }
                items(done, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onMarkDone = { onMarkDone(task.id) },
                        onSnooze = { onSnooze(task.id) },
                        onEdit = { onEdit(task) }
                    )
                }
            }
        }
    }
}
