package com.aira.health.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenAccount: () -> Unit = {},
    onOpenDataConfidence: () -> Unit = {},
    onOpenCorrections: () -> Unit = {},
    onOpenPredictions: () -> Unit = {},
    onOpenWeeklyReport: () -> Unit = {},
    onOpenWhatIfSimulator: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkModeEnabled = uiState.forceOledDarkTheme ?: Theme.colors.isLight.not()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.colors.dominant,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAccount)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Theme.colors.surfaceContainerLow)
                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Theme.colors.primaryContainer.copy(alpha = 0.2f))
                                .border(2.dp, Theme.colors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Theme.colors.primaryContainer)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = uiState.profileName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Theme.colors.accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(uiState.planStatus, style = MaterialTheme.typography.labelSmall, color = Theme.colors.accent)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = "Data & Privacy (Local First)",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = Theme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
            }

            item {
                SettingsSection {
                    SettingsSwitchRow(
                        icon = Icons.Default.DarkMode,
                        iconColor = Theme.colors.accent,
                        title = "OLED Dark Theme",
                        subtitle = "Toggle app theme between Light and OLED Dark",
                        checked = isDarkModeEnabled,
                        onCheckedChange = viewModel::setForceOledDarkTheme
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Sync,
                        iconColor = Theme.colors.accent,
                        title = "Health Connect Sync",
                        subtitle = if (uiState.healthConnectSyncEnabled) {
                            "Core permissions granted"
                        } else {
                            "Permissions required"
                        },
                        onClick = onOpenDataConfidence
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.CloudDone,
                        iconColor = Theme.colors.secondaryColor,
                        title = "Cloud Backup Preference",
                        subtitle = "Stored on-device until cloud sync is wired",
                        checked = uiState.cloudBackupEnabled,
                        onCheckedChange = viewModel::setCloudBackupEnabled
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Security,
                        iconColor = Theme.colors.tertiaryColor,
                        title = "Privacy Report",
                        subtitle = uiState.confidencePercent?.let { "Data confidence: $it%" }
                            ?: "Open confidence diagnostics",
                        onClick = onOpenDataConfidence
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = "Aira Intelligence",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = Theme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
            }

            item {
                SettingsSection {
                    SettingsActionRow(
                        icon = Icons.Default.Computer,
                        iconColor = Theme.colors.primaryContainer,
                        title = "Local LLM Status",
                        subtitle = uiState.localModelStatus,
                        onClick = onOpenPredictions
                    )
                    SettingsActionRow(
                        icon = Icons.Default.VpnKey,
                        iconColor = Theme.colors.onSurfaceVariant,
                        title = "Correction Model",
                        subtitle = "Review local correction history",
                        onClick = onOpenCorrections
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = "Reports & Simulations",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = Theme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
            }

            item {
                SettingsSection {
                    SettingsActionRow(
                        icon = Icons.Default.CloudDone,
                        iconColor = Theme.colors.accent,
                        title = "Weekly Report",
                        subtitle = "Review your trend summary",
                        onClick = onOpenWeeklyReport
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Computer,
                        iconColor = Theme.colors.tertiaryColor,
                        title = "What-If Simulator",
                        subtitle = "Try scenario predictions",
                        onClick = onOpenWhatIfSimulator
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Theme.colors.surfaceContainer)
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Theme.colors.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Theme.colors.accent,
                uncheckedThumbColor = Theme.colors.onSurfaceVariant,
                uncheckedTrackColor = Theme.colors.surfaceContainerHighest
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Theme.colors.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Theme.colors.onSurfaceVariant)
    }
}
