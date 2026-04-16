package com.aira.health.presentation.dashboard.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.dashboard.details.components.ActionGuidanceCard
import com.aira.health.presentation.dashboard.details.components.FactorBreakdownCard
import com.aira.health.presentation.dashboard.details.components.MetricTrendWindow
import com.aira.health.presentation.theme.Theme

@Composable
fun SleepDetailScreen(
    state: MetricDetailUiState.Success,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
        contentPadding = PaddingValues(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Sleep",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        item {
            Text(
                text = "Score ${state.currentScore}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Confidence ${(state.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Theme.colors.onSurfaceVariant
            )
        }

        item { MetricTrendWindow(dataPoints = state.trendDataPoints) }

        item {
            FactorBreakdownCard(
                factors = listOf(
                    "Sleep score trajectory",
                    "Recovery linkage",
                    "Stress carryover"
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ActionGuidanceCard(
                guidance = state.whatToDoNext,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
