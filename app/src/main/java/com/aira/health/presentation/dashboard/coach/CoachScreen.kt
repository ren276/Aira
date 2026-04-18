package com.aira.health.presentation.dashboard.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.dashboard.coach.components.GuidanceNarrativeCard
import com.aira.health.presentation.dashboard.coach.components.PredictionProjectionCard
import com.aira.health.presentation.dashboard.coach.components.WhatIfScenarioCard
import com.aira.health.presentation.dashboard.coach.components.WeeklyPlanDraftCard
import com.aira.health.presentation.theme.Theme

@Composable
fun CoachScreen(
    modifier: Modifier = Modifier,
    viewModel: CoachViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        CoachUiState.Loading -> CoachLoadingState(modifier = modifier)
        is CoachUiState.Error -> CoachErrorState(
            state = state,
            modifier = modifier,
            onSleepDeltaChanged = viewModel::onSleepDeltaChanged,
            onTrainingLoadDeltaChanged = viewModel::onTrainingLoadDeltaChanged,
            onRecalculate = viewModel::onRecalculateRequested,
        )

        is CoachUiState.Ready -> CoachReadyContent(
            state = state,
            modifier = modifier,
            onSleepDeltaChanged = viewModel::onSleepDeltaChanged,
            onTrainingLoadDeltaChanged = viewModel::onTrainingLoadDeltaChanged,
            onRecalculate = viewModel::onRecalculateRequested,
        )
    }
}

@Composable
fun CoachReadyContent(
    state: CoachUiState.Ready,
    onSleepDeltaChanged: (Float) -> Unit,
    onTrainingLoadDeltaChanged: (Float) -> Unit,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
    ) {
        Text(
            text = "Coach Weekly Planning",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                WhatIfScenarioCard(
                    scenario = state.scenario,
                    onSleepDeltaChanged = onSleepDeltaChanged,
                    onTrainingLoadDeltaChanged = onTrainingLoadDeltaChanged,
                    onRecalculate = onRecalculate,
                )
            }
            item {
                PredictionProjectionCard(model = state.projection)
            }
            item {
                GuidanceNarrativeCard(model = state.guidance)
            }
            item {
                WeeklyPlanDraftCard(model = state.weeklyDraft)
            }
            item {
                if (state.isRefreshing) {
                    Text(
                        text = "Recomputing projections...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Theme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Theme.colors.accent)
    }
}

@Composable
private fun CoachErrorState(
    state: CoachUiState.Error,
    onSleepDeltaChanged: (Float) -> Unit,
    onTrainingLoadDeltaChanged: (Float) -> Unit,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Coach planning is temporarily unavailable",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.colors.onSurfaceVariant,
        )
        WhatIfScenarioCard(
            modifier = Modifier.fillMaxWidth(),
            scenario = state.scenario,
            onSleepDeltaChanged = onSleepDeltaChanged,
            onTrainingLoadDeltaChanged = onTrainingLoadDeltaChanged,
            onRecalculate = onRecalculate,
        )
    }
}
