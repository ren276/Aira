package com.aira.health.presentation.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.data.local.model.NutritionLog
import com.aira.health.presentation.common.components.ConcentricRingsChart
import com.aira.health.presentation.common.components.GlassContainer
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = hiltViewModel(),
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showDeleteConfirmationForId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete meal log") },
            text = { Text("Remove this log permanently?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Delete", color = Theme.colors.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel", color = Theme.colors.onSurfaceVariant)
                }
            },
            containerColor = Theme.colors.surfaceContainerHigh
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                MacroCard(
                    consumedCalories = uiState.totalCalories,
                    targetCalories = uiState.calorieTarget,
                    totalProtein = uiState.totalProteinG,
                    totalCarbs = uiState.totalCarbsG,
                    totalFat = uiState.totalFatG
                )
            }

            item {
                NutritionActionBar(
                    onScanClick = viewModel::requestScannerDraft,
                    onBarcodeClick = { /* TODO */ },
                    onTellClick = { /* AI Voice TODO */ },
                    onFindClick = { /* Search TODO */ }
                )
            }

            item {
                RecoveryCatalystCard(bonusPct = 12)
            }

            item {
                Text(
                    text = "Recent Logs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isHistoryLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Theme.colors.accent)
                    }
                }
            } else if (uiState.history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No nutrition logs yet.", color = Theme.colors.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.history, key = { it.id }) { log ->
                    FoodLogItem(
                        log = log,
                        onEdit = { onNavigateToEdit(log.id) },
                        onDelete = { viewModel.initiateDelete(log.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MacroCard(
    consumedCalories: Float,
    targetCalories: Int,
    totalProtein: Float,
    totalCarbs: Float,
    totalFat: Float
) {
    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Calories Consumed",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = Theme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = consumedCalories.toInt().toString(),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Text(
                        text = " / $targetCalories kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = Theme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                MacroRow("Protein", totalProtein.toInt(), 160, Theme.colors.tertiaryColor)
                MacroRow("Carbs", totalCarbs.toInt(), 250, Theme.colors.secondaryColor)
                MacroRow("Fats", totalFat.toInt(), 70, Theme.colors.accent)
            }
            
            Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                ConcentricRingsChart(
                    proteinPct = (totalProtein / 160f).coerceIn(0f, 1f),
                    carbsPct = (totalCarbs / 250f).coerceIn(0f, 1f),
                    fatPct = (totalFat / 70f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxSize(),
                    thickness = 8.dp
                )
            }
        }
    }
}

@Composable
private fun MacroRow(label: String, value: Int, max: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Theme.colors.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Text(text = "${value}g / ${max}g", style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun NutritionActionBar(
    onScanClick: () -> Unit,
    onBarcodeClick: () -> Unit,
    onTellClick: () -> Unit,
    onFindClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionIconButton(icon = Icons.Default.QrCodeScanner, label = "SCAN", onClick = onScanClick, modifier = Modifier.weight(1f))
        ActionIconButton(icon = Icons.Default.RadioButtonChecked, label = "CODE", onClick = onBarcodeClick, modifier = Modifier.weight(1f))
        ActionIconButton(icon = Icons.Default.Mic, label = "TELL", onClick = onTellClick, modifier = Modifier.weight(1f))
        ActionIconButton(icon = Icons.Default.Search, label = "FIND", onClick = onFindClick, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassContainer(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Theme.colors.accent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Theme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecoveryCatalystCard(bonusPct: Int) {
    GlassContainer(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Theme.colors.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Theme.colors.accent)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "RECOVERY CATALYST",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = Theme.colors.accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your high-protein lunch adds a +$bonusPct% recovery bonus to tonight's sleep.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun FoodLogItem(log: NutritionLog, onEdit: () -> Unit, onDelete: () -> Unit) {
    GlassContainer(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = log.foodName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${log.calories.toInt()} kcal", style = MaterialTheme.typography.labelSmall, color = Theme.colors.accent)
                    Text("P: ${log.proteinG?.toInt() ?: 0}g", style = MaterialTheme.typography.labelSmall, color = Theme.colors.tertiaryColor)
                    Text("C: ${log.carbsG?.toInt() ?: 0}g", style = MaterialTheme.typography.labelSmall, color = Theme.colors.secondaryColor)
                    Text("F: ${log.fatG?.toInt() ?: 0}g", style = MaterialTheme.typography.labelSmall, color = Theme.colors.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Theme.colors.destructive)
            }
        }
    }
}
