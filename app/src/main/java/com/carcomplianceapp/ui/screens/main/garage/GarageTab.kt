package com.carcomplianceapp.ui.screens.main.garage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcomplianceapp.domain.model.*
import com.carcomplianceapp.ui.components.CarPill
import com.carcomplianceapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageTab(
    cars: List<Car>,
    activeCar: Car?,
    tasks: List<ComplianceTask>,
    onSelectCar: (Car) -> Unit,
    onAddCar: () -> Unit,
    onEditCar: (Long) -> Unit,
    onDeleteCar: (Car) -> Unit
) {
    var carToDelete by remember { mutableStateOf<Car?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Garage", style = MaterialTheme.typography.titleMedium) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCar,
                containerColor = AppColors.Teal400
            ) {
                Icon(Icons.Default.Add, "Add car", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (cars.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.DirectionsCar, null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))
                        Text("No cars yet", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap + to add your first car",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@LazyColumn
            }

            item {
                Text("YOUR CARS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp))
            }

            items(cars, key = { it.id }) { car ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        CarPill(
                            car = car,
                            isActive = car.id == activeCar?.id,
                            onClick = { onSelectCar(car) }
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { onEditCar(car.id) }) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { carToDelete = car }) {
                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp),
                            tint = AppColors.Red400)
                    }
                }
            }

            // Stats for active car
            activeCar?.let {
                val overdue = tasks.count { t -> t.urgency == UrgencyLevel.CRITICAL }
                val soon = tasks.count { t -> t.urgency == UrgencyLevel.HIGH || t.urgency == UrgencyLevel.MEDIUM }
                val ok = tasks.count { t -> t.urgency == UrgencyLevel.LOW }

                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("TASK SUMMARY — ACTIVE CAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip(overdue.toString(), "Overdue", AppColors.Red50, AppColors.Red800, Modifier.weight(1f))
                        StatChip(soon.toString(), "Coming soon", AppColors.Amber50, AppColors.Amber800, Modifier.weight(1f))
                        StatChip(ok.toString(), "Planned", AppColors.Teal50, AppColors.Teal800, Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    carToDelete?.let { car ->
        AlertDialog(
            onDismissRequest = { carToDelete = null },
            title = { Text("Delete ${car.nickname}?") },
            text = { Text("This will delete the car and all its compliance tasks. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDeleteCar(car); carToDelete = null }) {
                    Text("Delete", color = AppColors.Red600)
                }
            },
            dismissButton = {
                TextButton(onClick = { carToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatChip(value: String, label: String, bg: androidx.compose.ui.graphics.Color,
                     fg: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, color = fg,
                style = MaterialTheme.typography.headlineMedium)
            Text(label, fontSize = 10.sp, color = fg,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
