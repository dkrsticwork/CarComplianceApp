package com.carcomplianceapp.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcomplianceapp.ui.theme.AppColors

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkipToDemo: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // App icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(14.dp),
                color = AppColors.Teal50
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = AppColors.Teal600
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Car Compliance\nAssistant",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Know what your car needs next — legally and technically — without the guesswork.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

            // Trust callout
            Surface(
                color = AppColors.Blue50,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        null,
                        modifier = Modifier.size(16.dp).padding(top = 1.dp),
                        tint = AppColors.Blue600
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Trust-first: No ads, no paywalls. Every obligation comes with an explanation. Uncertain dates are shown as ranges, never fake precision.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Blue800,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teal400)
            ) {
                Text("Get started", fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onSkipToDemo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Skip to demo (no API key needed)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}
