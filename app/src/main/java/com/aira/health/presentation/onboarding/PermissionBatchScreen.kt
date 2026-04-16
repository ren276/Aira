package com.aira.health.presentation.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aira.health.presentation.theme.Theme
import com.aira.health.util.permission.HealthConnectStatus
import com.aira.health.util.permission.HealthPermissionManager

@Composable
fun PermissionBatchScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHealthConnectAvailability()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.healthConnectStatus != HealthConnectStatus.Available) {
        HealthConnectInstallPrompt(
            isUpdate = uiState.healthConnectStatus == HealthConnectStatus.UpdateRequired,
            onInstallClick = {
                val marketIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.google.android.apps.healthdata")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(marketIntent)
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                        )
                    )
                }
            },
            onContinueWithoutHealthConnect = { viewModel.onUseLimitedModeTapped() }
        )
        return
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.onBatchPermissionsResult(grantedPermissions)
    }

    if (uiState.onboardingComplete) {
        LaunchedEffect(Unit) { onOnboardingComplete() }
        return
    }

    val batchUi = when (uiState.currentBatch) {
        HealthPermissionManager.PermissionBatch.CORE -> BatchUi(
            title = "Connect your\nwearables",
            subtitle = "Aira reads biometric data locally via Health Connect to build your physiological baseline.",
            stepIndex = 2
        )
        HealthPermissionManager.PermissionBatch.BODY -> BatchUi(
            title = "Connect your\nbody metrics",
            subtitle = "Aira uses body composition data locally via Health Connect to build your physiological baseline.",
            stepIndex = 3
        )
        HealthPermissionManager.PermissionBatch.ADVANCED -> BatchUi(
            title = "Connect\nclinical metrics",
            subtitle = "Aira reads clinical data locally via Health Connect to build your physiological baseline.",
            stepIndex = 4
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.dominant)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Aira",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                    color = Color.White
                )
                Text(
                    text = "STEP ${batchUi.stepIndex} OF ${OnboardingFlow.TOTAL_STEPS}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Theme.colors.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = batchUi.title,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 44.sp),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = batchUi.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Theme.colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            // Connection Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Watch
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Theme.colors.surfaceContainerLow)
                            .border(0.5.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Watch, contentDescription = null, tint = Theme.colors.accent, modifier = Modifier.size(32.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Theme.colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Phone
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Theme.colors.surfaceContainerLow)
                            .border(0.5.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Buttons
            Button(
                onClick = {
                    viewModel.onGrantAccessTapped()
                    permissionLauncher.launch(viewModel.getPermissionsForCurrentBatch())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Theme.colors.accent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Connect Health Data", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = {
                    when (uiState.currentBatch) {
                        HealthPermissionManager.PermissionBatch.CORE -> viewModel.onUseLimitedModeTapped()
                        HealthPermissionManager.PermissionBatch.BODY -> viewModel.advanceToAdvancedBatch()
                        HealthPermissionManager.PermissionBatch.ADVANCED -> viewModel.skipAdvancedBatch()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Skip for now", color = Theme.colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HealthConnectInstallPrompt(
    isUpdate: Boolean,
    onInstallClick: () -> Unit,
    onContinueWithoutHealthConnect: () -> Unit
) {
    Surface(color = Theme.colors.dominant) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "STEP 2 OF ${OnboardingFlow.TOTAL_STEPS}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Theme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (isUpdate) "Health Connect Update Required" else "Health Connect Required",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Aira requires Health Connect to securely read your biometric data locally.",
                style = MaterialTheme.typography.bodyLarge,
                color = Theme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = onInstallClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Theme.colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 32.dp)
            ) {
                Text(
                    if (isUpdate) "Update Health Connect" else "Install Health Connect",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                onClick = onContinueWithoutHealthConnect,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text("Continue without metrics", color = Theme.colors.onSurfaceVariant)
            }
        }
    }
}

private data class BatchUi(
    val title: String,
    val subtitle: String,
    val stepIndex: Int
)
