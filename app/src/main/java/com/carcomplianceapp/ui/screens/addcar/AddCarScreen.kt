package com.carcomplianceapp.ui.screens.addcar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.carcomplianceapp.domain.model.FuelType
import com.carcomplianceapp.ui.theme.AppColors
import com.carcomplianceapp.ui.viewmodel.AddCarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    editCarId: Long? = null,
    onCarSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddCarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(editCarId) { editCarId?.let { viewModel.loadCar(it) } }

    val docPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> viewModel.addDocuments(uris.map { it.toString() }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (editCarId == null) "Add car" else "Edit car",
                            style = MaterialTheme.typography.titleMedium)
                        if (editCarId == null)
                            Text("Step 2 of 2", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (editCarId == null) {
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Teal400
                )
            }

            // ── Country ───────────────────────────────────────────────────────

            SectionLabel("Country / Region")
            var countryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = countryExpanded,
                onExpandedChange = { countryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.countryDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(countryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                    AddCarViewModel.COUNTRIES.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = { viewModel.setCountry(code, name); countryExpanded = false }
                        )
                    }
                }
            }

            // ── Make ──────────────────────────────────────────────────────────

            SectionLabel("Car manufacturer")
            OutlinedTextField(
                value = uiState.makeQuery,
                onValueChange = { viewModel.onMakeQueryChanged(it) },
                label = { Text("Search manufacturer") },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            if (uiState.filteredMakes.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.filteredMakes) { make ->
                        FilterChip(
                            selected = uiState.selectedMake == make,
                            onClick = { viewModel.selectMake(make) },
                            label = { Text(make, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Teal400,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // ── Model ─────────────────────────────────────────────────────────

            if (uiState.selectedMake.isNotBlank()) {
                SectionLabel("Model")
                var modelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                    OutlinedTextField(
                        value = uiState.selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        uiState.availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = { viewModel.selectModel(model); modelExpanded = false }
                            )
                        }
                    }
                }
            }

            // ── Year ──────────────────────────────────────────────────────────

            SectionLabel("Year of manufacture")
            val years = remember { (2025 downTo 1990).toList() }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(years) { year ->
                    FilterChip(
                        selected = uiState.selectedYear == year,
                        onClick = { viewModel.selectYear(year) },
                        label = { Text(year.toString(), fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Teal400,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ── Fuel type ─────────────────────────────────────────────────────

            SectionLabel("Fuel type")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(FuelType.entries.filter { it != FuelType.UNKNOWN }) { fuel ->
                    FilterChip(
                        selected = uiState.selectedFuel == fuel,
                        onClick = { viewModel.selectFuel(fuel) },
                        label = { Text(fuel.displayName, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Teal400,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            HorizontalDivider()

            // ── Optional fields ───────────────────────────────────────────────

            Text("Optional — improves accuracy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.lastServiceMonth,
                    onValueChange = { viewModel.setLastService(it) },
                    label = { Text("Last service") },
                    placeholder = { Text("YYYY-MM") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.insuranceExpiry,
                    onValueChange = { viewModel.setInsuranceExpiry(it) },
                    label = { Text("Insurance expiry") },
                    placeholder = { Text("YYYY-MM") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.registrationExpiry,
                    onValueChange = { viewModel.setRegistrationExpiry(it) },
                    label = { Text("Registration expiry") },
                    placeholder = { Text("YYYY-MM") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.odometerKm,
                    onValueChange = { viewModel.setOdometer(it) },
                    label = { Text("Odometer km") },
                    placeholder = { Text("e.g. 78000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // ── Documents ─────────────────────────────────────────────────────

            SectionLabel("Attach documents (optional)")
            Text("Documents are sent to AI for analysis to improve your obligation list.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedButton(
                onClick = { docPickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Attach documents")
            }

            uiState.documentPaths.forEach { path ->
                Surface(
                    color = AppColors.Teal50,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, AppColors.Teal400)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.InsertDriveFile, null,
                            modifier = Modifier.size(14.dp), tint = AppColors.Teal600)
                        Spacer(Modifier.width(8.dp))
                        Text(path.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f), color = AppColors.Teal800)
                        IconButton(onClick = { viewModel.removeDocument(path) },
                            modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp),
                                tint = AppColors.Teal600)
                        }
                    }
                }
            }

            // Error
            uiState.error?.let { err ->
                Surface(color = AppColors.Red50, shape = RoundedCornerShape(8.dp)) {
                    Text(err, color = AppColors.Red800,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            // Generate button
            Button(
                onClick = { viewModel.saveCar { onCarSaved() } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading && uiState.selectedMake.isNotBlank() && uiState.selectedYear != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teal400)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Generating obligation list...", fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate my obligation list", fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
