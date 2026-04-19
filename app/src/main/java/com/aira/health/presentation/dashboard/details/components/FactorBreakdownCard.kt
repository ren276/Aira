package com.aira.health.presentation.dashboard.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.dashboard.details.MetricDetailUiState
import kotlin.math.roundToInt

@Composable
fun FactorBreakdownCard(
    factors: List<MetricDetailUiState.RankedFactor>,
    modifier: Modifier = Modifier
) {
    val topThree = (factors
        .sortedBy { it.rank }
        .take(3) + buildFallbackFactors(factors.size)).take(3)

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Factor Breakdown", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            topThree.forEachIndexed { index, factor ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("factor-row-${index + 1}"),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}. ${factor.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${factor.direction.toDisplayLabel()} • ${factor.weight.toPercent()}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Text(
                        text = factor.windowTag,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun MetricDetailUiState.FactorDirection.toDisplayLabel(): String = when (this) {
    MetricDetailUiState.FactorDirection.INCREASED -> "Increased"
    MetricDetailUiState.FactorDirection.DECREASED -> "Decreased"
    MetricDetailUiState.FactorDirection.NEUTRAL -> "Neutral"
}

private fun Float.toPercent(): String = "${(this * 100f).roundToInt()}%"

private fun buildFallbackFactors(existingCount: Int): List<MetricDetailUiState.RankedFactor> {
    if (existingCount >= 3) return emptyList()
    return (existingCount until 3).map { index ->
        MetricDetailUiState.RankedFactor(
            rank = index + 1,
            name = "Additional local data required",
            direction = MetricDetailUiState.FactorDirection.NEUTRAL,
            weight = 0f,
            windowTag = "last 7d"
        )
    }
}
