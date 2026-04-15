package com.aira.health.presentation.train

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TrainEditScreen(
    workoutId: Long,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) {
    // Placeholder for actual Deep-Edit UI required by D-13.
    // Integrated during screen wiring phase or detailed separately.
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Train Edit Screen (Deep Edit) for Workout ID: $workoutId")
    }
}
