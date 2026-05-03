package com.carcomplianceapp.ui.screens.apikey

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.carcomplianceapp.domain.model.AiProvider
import com.carcomplianceapp.ui.theme.AppColors
import com.carcomplianceapp.ui.viewmodel.ApiKeyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    viewModel: ApiKeyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var keyVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Engine Setup", style = MaterialTheme.typography.titleMedium)
                        Text("Step 1 of 2", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
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
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Teal400
            )

            Text(
                "Paste your AI API key",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Your key is stored only on this device and used solely to generate your car obligation list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Key input
            OutlinedTextField(
                value = uiState.rawKey,
                onValueChange = { viewModel.onKeyChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("sk-... or similar") },
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (keyVisible) "Hide" else "Show"
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Detected provider badge
            AnimatedVisibility(visible = uiState.detectedProvider != null) {
                uiState.detectedProvider?.let { provider ->
                    Surface(
                        color = AppColors.Teal50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = AppColors.Teal400, modifier = Modifier.size(16.dp))
                            Text("Detected: ${provider.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Teal800)
                        }
                    }
                }
            }

            // Supported providers card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Supported providers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    AiProvider.entries.forEach { provider ->
                        val isDetected = uiState.detectedProvider == provider
                        val bg = if (isDetected) AppColors.Teal50 else MaterialTheme.colorScheme.surface
                        val fg = if (isDetected) AppColors.Teal800 else MaterialTheme.colorScheme.onSurfaceVariant
                        Surface(color = bg, shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                provider.displayName,
                                fontSize = 11.sp,
                                color = fg,
                                fontFamily = if (isDetected) FontFamily.Default else FontFamily.Default,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Trust note
            Surface(color = AppColors.Blue50, shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp),
                        tint = AppColors.Blue600)
                    Text(
                        "Your key never leaves your device. We do not log, store, or share it with any third party.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Blue800,
                        lineHeight = 16.sp
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.saveKey { onContinue() }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = uiState.rawKey.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teal400)
            ) {
                Text("Continue", fontSize = 15.sp)
            }
        }
    }
}
