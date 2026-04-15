package com.aira.health.presentation.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
            title = { Text("Delete nutrition entry") },
            text = { Text("Remove this meal log permanently? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nutrition") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Add Section
            OutlinedTextField(
                value = uiState.quickAddFoodName,
                onValueChange = viewModel::onFoodNameChange,
                label = { Text("Food Name") },
                isError = uiState.inputError != null,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = uiState.quickAddCalories,
                onValueChange = viewModel::onCaloriesChange,
                label = { Text("Calories") },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.inputError != null) {
                Text(
                    text = uiState.inputError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = viewModel::saveQuickAdd,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Entry")
                }

                IconButton(
                    onClick = { 
                        // Simulate scanner draft action
                        viewModel.onScannerDraftReceived("Scanned Food", 200f)
                    }
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Recent Entries", style = MaterialTheme.typography.titleMedium)
            
            // Dummy item for UI flow tests
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dummy Food")
                    Text("100 kcal")
                }
                Row {
                    IconButton(onClick = { onNavigateToEdit(1L) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { viewModel.initiateDelete(1L) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}
