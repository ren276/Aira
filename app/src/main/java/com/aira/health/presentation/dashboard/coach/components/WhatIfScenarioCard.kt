package com.aira.health.presentation.dashboard.coach.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.domain.model.PredictionScenario
import com.aira.health.presentation.dashboard.coach.ScenarioInput
import com.aira.health.presentation.theme.Theme
import kotlin.math.roundToInt

@Composable
fun WhatIfScenarioCard(
    scenario: ScenarioInput,
    onSleepDeltaChanged: (Float) -> Unit,
    onTrainingLoadDeltaChanged: (Float) -> Unit,
    onRecalculate: () -> Unit,
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("coach-card-scenario")
            .background(
                color = Theme.colors.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "What-If Scenario",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )

        Text(
            text = "Sleep delta: ${scenario.sleepDeltaHours.toSignedHoursLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.colors.onSurfaceVariant,
        )
        Slider(
            modifier = Modifier.testTag("coach-sleep-slider"),
            value = scenario.sleepDeltaHours,
            onValueChange = onSleepDeltaChanged,
            valueRange = PredictionScenario.MIN_SLEEP_DELTA_HOURS..PredictionScenario.MAX_SLEEP_DELTA_HOURS,
        )

        Text(
            text = "Training load delta: ${scenario.trainingLoadDeltaPercent.toSignedPercentLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.colors.onSurfaceVariant,
        )
        Slider(
            modifier = Modifier.testTag("coach-load-slider"),
            value = scenario.trainingLoadDeltaPercent,
            onValueChange = onTrainingLoadDeltaChanged,
            valueRange = PredictionScenario.MIN_TRAINING_LOAD_DELTA_PERCENT..
                PredictionScenario.MAX_TRAINING_LOAD_DELTA_PERCENT,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                modifier = Modifier.testTag("coach-recalculate-button"),
                enabled = !isRefreshing,
                onClick = onRecalculate,
            ) {
                Text(if (isRefreshing) "Recalculating..." else "Recalculate plan")
            }
        }
    }
}

private fun Float.toSignedPercentLabel(): String {
    val rounded = roundToInt()
    return if (rounded > 0) "+$rounded%" else "$rounded%"
}

private fun Float.toSignedHoursLabel(): String {
    return if (this > 0f) "+${String.format("%.1f", this)} h" else "${String.format("%.1f", this)} h"
}
