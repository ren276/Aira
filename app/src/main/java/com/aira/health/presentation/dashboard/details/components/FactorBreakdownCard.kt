package com.aira.health.presentation.dashboard.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FactorBreakdownCard(
    factors: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Factor Breakdown", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (factors.isEmpty()) {
                Text("No specific factors identified.", style = MaterialTheme.typography.bodyMedium)
            } else {
                factors.forEach { factor ->
                    Text("• $factor", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
