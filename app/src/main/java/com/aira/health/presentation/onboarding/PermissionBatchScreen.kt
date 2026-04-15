package com.aira.health.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.util.permission.HealthConnectStatus
import com.aira.health.util.permission.HealthPermissionManager

/**
 * Displays an individual rationale screen for each permission batch.
 * Conforms to Phase 1 CONTEXT decision: individual screen per batch, hard block on Core denial.
 *
 * Full visual polish (Aira design tokens, animations) delivered in Phase 4.
 */
@Composable
fun PermissionBatchScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Health Connect install prompt — for Android 10-13 users
    if (uiState.healthConnectStatus == HealthConnectStatus.NotInstalled) {
        HealthConnectInstallPrompt()
        return
    }

    // Permission launcher for current batch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.onBatchPermissionsResult(grantedPermissions)
    }

    if (uiState.onboardingComplete) {
        LaunchedEffect(Unit) { onOnboardingComplete() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rationale content varies by batch
        val (title, subtitle, items) = when (uiState.currentBatch) {
            HealthPermissionManager.PermissionBatch.CORE -> Triple(
                "Core Health Access",
                "Required to compute your recovery, sleep, and strain scores — the heart of Aira.",
                listOf(
                    "Heart rate — recovery and strain scoring",
                    "HRV — stress and readiness detection",
                    "Sleep — sleep quality analysis",
                    "Steps & Activity — daily movement tracking"
                )
            )
            HealthPermissionManager.PermissionBatch.BODY -> Triple(
                "Body Metrics",
                "Adds SpO2, nutrition, and body composition to your insights.",
                listOf(
                    "Blood oxygen — sleep quality refinement",
                    "Weight & Body Fat — body composition tracking",
                    "Nutrition & Hydration — energy balance scoring",
                    "Skin Temperature — illness and cycle detection"
                )
            )
            HealthPermissionManager.PermissionBatch.ADVANCED -> Triple(
                "Advanced Tracking",
                "Optional — enables glucose, blood pressure, and cycle tracking.",
                listOf(
                    "Blood glucose — CGM integration (optional)",
                    "Blood pressure — cardiovascular monitoring",
                    "Cycle tracking — hormonal pattern recognition",
                    "Respiratory rate — sleep apnea indicators"
                )
            )
        }

        Text(text = "Aira runs on your device", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))

        // Permission items list
        items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(text = "•  $item", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.onGrantAccessTapped()
                permissionLauncher.launch(viewModel.getPermissionsForCurrentBatch())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant access")
        }

        // Core batch: hard block — show "Use limited mode" only after system denial
        if (uiState.currentBatch == HealthPermissionManager.PermissionBatch.CORE &&
            !uiState.showRationaleScreen && !uiState.isCoreGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { viewModel.onUseLimitedModeTapped() }) {
                Text("Use limited mode")
            }
        }

        // Body batch: skip is allowed
        if (uiState.currentBatch == HealthPermissionManager.PermissionBatch.BODY) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { viewModel.advanceToAdvancedBatch() }) {
                Text("Skip for now")
            }
        }

        // Advanced batch: always skippable
        if (uiState.currentBatch == HealthPermissionManager.PermissionBatch.ADVANCED) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { viewModel.skipAdvancedBatch() }) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun HealthConnectInstallPrompt() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Health Connect Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "For the most accurate health data, please install Health Connect from the Play Store. " +
            "It's free and ensures your data stays private on your device.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* Launch Play Store intent for Health Connect */ }) {
            Text("Install Health Connect")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { /* Proceed with Google Fit fallback */ }) {
            Text("Continue without Health Connect")
        }
    }
}
