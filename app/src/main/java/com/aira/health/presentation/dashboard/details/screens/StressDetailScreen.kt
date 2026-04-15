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
fun StressDetailScreen(
    state: MetricDetailUiState.Success,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Stress", style = MaterialTheme.typography.headlineMedium)
        Text("Score: ${state.currentScore} (Confidence: ${state.confidence})", style = MaterialTheme.typography.titleMedium)
        
        MetricTrendWindow(dataPoints = state.trendDataPoints)
        FactorBreakdownCard(factors = listOf("Psychological Stress", "Physiological Stress"))
        ActionGuidanceCard(guidance = "Your stress levels are elevated. Try a 5-minute breathing exercise.")
    }
}
