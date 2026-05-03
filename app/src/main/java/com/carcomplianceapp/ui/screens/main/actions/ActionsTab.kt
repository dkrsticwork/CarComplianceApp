package com.carcomplianceapp.ui.screens.main.actions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcomplianceapp.domain.model.*
import com.carcomplianceapp.ui.components.EmptyState
import com.carcomplianceapp.ui.components.UrgencyBadge
import com.carcomplianceapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsTab(
    tasks: List<ComplianceTask>,
    onMarkDone: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onEdit: (ComplianceTask) -> Unit
) {
    val urgent = remember(tasks) {
        tasks
            .filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SNOOZED }
            .sortedBy { it.urgency.ordinal }
            .take(3)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Next actions", style = MaterialTheme.typography.titleMedium) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (urgent.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.Teal50,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                modifier = Modifier.size(44.dp), tint = AppColors.Teal400)
                            Spacer(Modifier.height(10.dp))
                            Text("All caught up!", style = MaterialTheme.typography.titleMedium,
                                color = AppColors.Teal800)
                            Spacer(Modifier.height(4.dp))
                            Text("No urgent actions right now. Check your timeline for upcoming items.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Teal600)
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                Surface(color = AppColors.Blue50, shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = AppColors.Blue600)
                        Text("Focus on these first. The app updates your timeline as you complete them.",
                            style = MaterialTheme.typography.bodySmall, color = AppColors.Blue800,
                            lineHeight = 16.sp)
                    }
                }
            }

            itemsIndexed(urgent) { index, task ->
                ActionCard(
                    number = index + 1,
                    task = task,
                    onMarkDone = { onMarkDone(task.id) },
                    onSnooze = { onSnooze(task.id) },
                    onEdit = { onEdit(task) }
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    number: Int,
    task: ComplianceTask,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit
) {
    val numBg = when (task.urgency) {
        UrgencyLevel.CRITICAL -> AppColors.Red50
        UrgencyLevel.HIGH     -> AppColors.Amber50
        else                  -> AppColors.Teal50
    }
    val numFg = when (task.urgency) {
        UrgencyLevel.CRITICAL -> AppColors.Red800
        UrgencyLevel.HIGH     -> AppColors.Amber800
        else                  -> AppColors.Teal800
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Number bubble
            Surface(color = numBg, shape = CircleShape, modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("$number", color = numFg, fontSize = 13.sp,
                        style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    UrgencyBadge(task.urgency)
                }
                Text(task.dueDateWindow, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(task.why, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp, fontStyle = FontStyle.Italic)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onMarkDone,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teal400)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Done", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Snooze, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Snooze 7d", fontSize = 12.sp)
                    }
                    if (task.isUserEditable) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
