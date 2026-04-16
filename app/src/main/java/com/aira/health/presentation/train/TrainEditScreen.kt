package com.aira.health.presentation.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainEditScreen(
    workoutId: Long,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    viewModel: TrainEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    LaunchedEffect(uiState.closeScreen) {
        if (uiState.closeScreen) {
            viewModel.consumeCloseRequest()
            onNavigateUp()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.colors.dominant,
        topBar = {
            TopAppBar(
                title = { Text("Edit Workout", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::deleteWorkout) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete workout", tint = Theme.colors.destructive)
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Theme.colors.accent)
                }
            }

            uiState.notFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Workout entry not found.", color = Theme.colors.onSurfaceVariant)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Theme.colors.surfaceContainerLow, RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Workout Details",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = uiState.exercise,
                                onValueChange = viewModel::onExerciseChange,
                                label = { Text("Exercise") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Theme.colors.surfaceContainerHighest,
                                    unfocusedContainerColor = Theme.colors.surfaceContainerHighest,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Theme.colors.onSurfaceVariant,
                                    unfocusedLabelColor = Theme.colors.onSurfaceVariant,
                                    cursorColor = Theme.colors.accent
                                )
                            )

                            OutlinedTextField(
                                value = uiState.durationMin,
                                onValueChange = viewModel::onDurationChange,
                                label = { Text("Duration (min)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Theme.colors.surfaceContainerHighest,
                                    unfocusedContainerColor = Theme.colors.surfaceContainerHighest,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Theme.colors.onSurfaceVariant,
                                    unfocusedLabelColor = Theme.colors.onSurfaceVariant,
                                    cursorColor = Theme.colors.accent
                                )
                            )

                            uiState.inputError?.let { error ->
                                Text(
                                    text = error,
                                    color = Theme.colors.destructive,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = viewModel::saveChanges,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Theme.colors.accent)
                        ) {
                            Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        TextButton(
                            onClick = viewModel::deleteWorkout,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Workout", color = Theme.colors.destructive)
                        }
                    }
                }
            }
        }
    }
}
