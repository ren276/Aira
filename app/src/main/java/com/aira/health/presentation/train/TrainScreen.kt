package com.aira.health.presentation.train

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TrainScreen(
    modifier: Modifier = Modifier,
    viewModel: TrainViewModel = hiltViewModel(),
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showDeleteConfirmationForId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete this workout? This action cannot be undone and will recalculate your daily strain.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    modifier = Modifier.semantics { contentDescription = "confirm delete" }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AiraSpacing.md)
    ) {
        QuickAddCard(
            quickAddExercise = uiState.quickAddExercise,
            quickAddDurationMin = uiState.quickAddDurationMin,
            inputError = uiState.inputError,
            onExerciseChange = viewModel::onExerciseChange,
            onDurationChange = viewModel::onDurationChange,
            onSaveQuickAdd = viewModel::saveQuickAdd
        )

        Spacer(modifier = Modifier.height(AiraSpacing.lg))

        Text(
            text = "History",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(AiraSpacing.sm))

        if (uiState.isHistoryLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.history.isEmpty()) {
            Text(
                "No workouts logged recently.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AiraSpacing.sm),
                contentPadding = PaddingValues(bottom = AiraSpacing.xxl)
            ) {
                items(uiState.history, key = { it.id }) { session ->
                    HistoryItem(
                        session = session,
                        onEdit = { onNavigateToEdit(session.id) },
                        onDelete = { viewModel.initiateDelete(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    quickAddExercise: String,
    quickAddDurationMin: String,
    inputError: String?,
    onExerciseChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onSaveQuickAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Quick Add Card" },
        colors = CardDefaults.cardColors(containerColor = Theme.colors.secondary)
    ) {
        Column(
            modifier = Modifier.padding(AiraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AiraSpacing.sm)
        ) {
            Text(
                text = "Log Workout",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = quickAddExercise,
                onValueChange = onExerciseChange,
                label = { Text("Exercise") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "exercise input" },
                singleLine = true
            )

            OutlinedTextField(
                value = quickAddDurationMin,
                onValueChange = onDurationChange,
                label = { Text("Duration (min)") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "duration input" },
                singleLine = true
            )

            if (inputError != null) {
                Text(
                    text = inputError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onSaveQuickAdd,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "save quick add" }
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun HistoryItem(
    session: WorkoutSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val dateStr = Instant.ofEpochMilli(session.startTime)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "history item ${session.exerciseType}" },
        colors = CardDefaults.cardColors(containerColor = Theme.colors.secondary)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AiraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.exerciseType,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$dateStr • ${session.durationMin} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.semantics { contentDescription = "edit ${session.exerciseType}" }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Workout", tint = Theme.colors.accent)
            }
            IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = "delete ${session.exerciseType}" }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Workout", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
