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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Theme.colors.dominant,
        topBar = {
            TopAppBar(
                title = { Text("Nutrition", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
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
                QuickAddFoodCard(
                    quickAddFoodName = uiState.quickAddFoodName,
                    quickAddCalories = uiState.quickAddCalories,
                    inputError = uiState.inputError,
                    onFoodNameChange = viewModel::onFoodNameChange,
                    onCaloriesChange = viewModel::onCaloriesChange,
                    onSaveQuickAdd = viewModel::saveQuickAdd,
                    onScanClick = viewModel::requestScannerDraft
                )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surfaceContainerLow)
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(
                        text = "Calories Consumed",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = Theme.colors.onSurfaceVariant
                    )
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
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroColumn("Protein", totalProtein.toInt(), 160, Theme.colors.accent)
                MacroColumn("Carbs", totalCarbs.toInt(), 250, Theme.colors.secondaryColor)
                MacroColumn("Fats", totalFat.toInt(), 70, Theme.colors.tertiaryColor)
            }
        }
    }
}

@Composable
private fun MacroColumn(label: String, value: Int, max: Int, color: Color) {
    val pct = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Theme.colors.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "${value}g", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Theme.colors.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddFoodCard(
    quickAddFoodName: String,
    quickAddCalories: String,
    inputError: String?,
    onFoodNameChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onSaveQuickAdd: () -> Unit,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surfaceContainer)
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = Theme.colors.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Log Food",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                IconButton(onClick = onScanClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Theme.colors.accent)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quickAddFoodName,
                    onValueChange = onFoodNameChange,
                    placeholder = { Text("Food name", color = Theme.colors.onSurfaceVariant) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Theme.colors.surfaceContainerHighest,
                        unfocusedContainerColor = Theme.colors.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Theme.colors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = quickAddCalories,
                    onValueChange = onCaloriesChange,
                    placeholder = { Text("kcal", color = Theme.colors.onSurfaceVariant) },
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Theme.colors.surfaceContainerHighest,
                        unfocusedContainerColor = Theme.colors.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Theme.colors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (inputError != null) {
                Text(
                    text = inputError,
                    color = Theme.colors.destructive,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onSaveQuickAdd,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Theme.colors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Entry", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FoodLogItem(log: NutritionLog, onEdit: () -> Unit, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surfaceContainerLow)
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = log.foodName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${log.calories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Theme.colors.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Theme.colors.destructive)
                }
            }
        }
    }
}
