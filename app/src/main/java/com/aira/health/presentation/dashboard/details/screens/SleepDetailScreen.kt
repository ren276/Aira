package com.aira.health.presentation.dashboard.details.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.dashboard.details.MetricDetailUiState
import com.aira.health.presentation.dashboard.details.components.ActionGuidanceCard
import com.aira.health.presentation.dashboard.details.components.FactorBreakdownCard
import com.aira.health.presentation.dashboard.details.components.MetricTrendWindow

@Composable
fun SleepDetailScreen(
    state: MetricDetailUiState.Success,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sleep", style = MaterialTheme.typography.headlineMedium)
        Text("Score: ${state.currentScore} (Confidence: ${state.confidence})", style = MaterialTheme.typography.titleMedium)
        
        MetricTrendWindow(dataPoints = state.trendDataPoints)
        FactorBreakdownCard(factors = listOf("Sleep Duration", "Deep Sleep", "REM Sleep"))
        ActionGuidanceCard(guidance = "Maintain your current sleep schedule to sustain optimal recovery.")
    }
}
