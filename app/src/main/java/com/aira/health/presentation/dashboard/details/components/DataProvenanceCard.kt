package com.aira.health.presentation.dashboard.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.theme.Theme

@Composable
fun DataProvenanceCard(
    dataSources: List<String>,
    consideredData: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.surfaceContainerLow, RoundedCornerShape(12.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Data Sources",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
        if (dataSources.isEmpty()) {
            Text(
                text = "No source metadata available for this day.",
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.onSurfaceVariant
            )
        } else {
            dataSources.forEach { source ->
                Text(
                    text = "- $source",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Data Considered",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
        if (consideredData.isEmpty()) {
            Text(
                text = "No input values available.",
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.onSurfaceVariant
            )
        } else {
            consideredData.forEach { item ->
                Text(
                    text = "- $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.onSurfaceVariant
                )
            }
        }
    }
}
