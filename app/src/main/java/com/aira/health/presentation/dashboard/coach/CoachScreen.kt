package com.aira.health.presentation.dashboard.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
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
        CoachSectionHeader(
            title = "Coach Weekly Planning",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                CoachCardsSection(
                    state = state,
                    onSleepDeltaChanged = onSleepDeltaChanged,
                    onTrainingLoadDeltaChanged = onTrainingLoadDeltaChanged,
                    onRecalculate = onRecalculate,
                )
            }
        }
    }
}

@Composable
fun CoachInlineSection(
    state: CoachUiState,
    onSleepDeltaChanged: (Float) -> Unit,
    onTrainingLoadDeltaChanged: (Float) -> Unit,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.dominant),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CoachSectionHeader(
            title = "Coach",
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        when (state) {
            CoachUiState.Loading -> CoachInlineLoadingState()

            is CoachUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Theme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                WhatIfScenarioCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    scenario = state.scenario,
                    onSleepDeltaChanged = onSleepDeltaChanged,
                    onTrainingLoadDeltaChanged = onTrainingLoadDeltaChanged,
                    onRecalculate = onRecalculate,
                    isRefreshing = false,
                )
            }

            is CoachUiState.Ready -> CoachCardsSection(
                state = state,
                onSleepDeltaChanged = onSleepDeltaChanged,
                onTrainingLoadDeltaChanged = onTrainingLoadDeltaChanged,
                onRecalculate = onRecalculate,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun CoachInlineLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Theme.colors.surfaceContainerLow,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Theme.colors.accent, strokeWidth = 2.dp)
                Text(
                    text = "Loading coach planning...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CoachCardsSection(
    state: CoachUiState.Ready,
    onSleepDeltaChanged: (Float) -> Unit,
    onTrainingLoadDeltaChanged: (Float) -> Unit,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WhatIfScenarioCard(
            scenario = state.scenario,
            onSleepDeltaChanged = onSleepDeltaChanged,
            onTrainingLoadDeltaChanged = onTrainingLoadDeltaChanged,
            onRecalculate = onRecalculate,
            isRefreshing = state.isRefreshing,
        )
        PredictionProjectionCard(model = state.projection)
        GuidanceNarrativeCard(model = state.guidance)
        WeeklyPlanDraftCard(model = state.weeklyDraft)

        if (state.isRefreshing) {
            Text(
                text = "Recomputing projections...",
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CoachSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = modifier,
    )
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
            isRefreshing = false,
        )
    }
}
