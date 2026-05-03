package com.carcomplianceapp.ui.screens.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.carcomplianceapp.ui.components.ApiErrorBanner
import com.carcomplianceapp.ui.components.NotifToggleRow
import com.carcomplianceapp.ui.theme.AppColors
import com.carcomplianceapp.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    onGoToApiKey: () -> Unit,
    apiError: String?,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notifPrefs by viewModel.notifPrefs.collectAsState()
    val apiKeyConfig by viewModel.apiKeyConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.titleMedium) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API error banner (trust-critical)
            apiError?.let {
                ApiErrorBanner(errorMessage = it, onGoToSettings = onGoToApiKey)
            }

            // AI Engine card
            SectionLabel("AI Engine")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (bg, fg, icon, statusText) = when {
                            apiError != null -> listOf(AppColors.Red50, AppColors.Red800,
                                Icons.Default.Warning, "Key issue — see above")
                            apiKeyConfig != null -> listOf(AppColors.Teal50, AppColors.Teal800,
                                Icons.Default.CheckCircle, "Connected · ${apiKeyConfig!!.provider.displayName}")
                            else -> listOf(AppColors.Amber50, AppColors.Amber800,
                                Icons.Default.Warning, "No key set")
                        }
                        @Suppress("UNCHECKED_CAST")
                        val bgC = bg as androidx.compose.ui.graphics.Color
                        val fgC = fg as androidx.compose.ui.graphics.Color
                        Surface(color = bgC, shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null,
                                    modifier = Modifier.size(16.dp), tint = fgC)
                                Text(statusText as String, style = MaterialTheme.typography.bodySmall, color = fgC)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    apiKeyConfig?.let { config ->
                        Text("Current key", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "${config.rawKey.take(8)}••••••••••••",
                                modifier = Modifier.padding(10.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    OutlinedButton(
                        onClick = onGoToApiKey,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Key, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Change API key", fontSize = 13.sp)
                    }
                }
            }

            // What happens when key fails — always visible trust box
            Surface(color = AppColors.Blue50, shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp),
                            tint = AppColors.Blue600)
                        Text("What happens if my key expires?",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Blue800, fontStyle = FontStyle.Normal)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "If your API key expires or runs out of credits, the app will show a clear error banner and your existing saved tasks will remain available. You will NOT lose your compliance data. Simply update the key in Settings to restore AI task generation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Blue800, lineHeight = 16.sp, fontStyle = FontStyle.Italic
                    )
                }
            }

            // Notifications
            SectionLabel("Notifications")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    notifPrefs?.let { prefs ->
                        NotifToggleRow("30 days before deadline", prefs.thirtyDays) {
                            viewModel.updateNotifPrefs(prefs.copy(thirtyDays = it))
                        }
                        NotifToggleRow("7 days before deadline", prefs.sevenDays) {
                            viewModel.updateNotifPrefs(prefs.copy(sevenDays = it))
                        }
                        NotifToggleRow("1 day before deadline", prefs.oneDay) {
                            viewModel.updateNotifPrefs(prefs.copy(oneDay = it))
                        }
                    }
                }
            }

            // About
            SectionLabel("About")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Car Compliance Assistant · MVP v1.0",
                        style = MaterialTheme.typography.bodySmall)
                    Text("No ads · No telemetry · No paywalls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("All data stored locally on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Uncertain obligation dates are always shown as ranges, never false precision.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
