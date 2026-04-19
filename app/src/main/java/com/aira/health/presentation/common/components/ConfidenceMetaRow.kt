package com.aira.health.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme

@Composable
fun ConfidenceMetaRow(
    confidence: String,
    lastUpdated: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AiraSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Confidence: $confidence",
            style = MaterialTheme.typography.labelLarge,
            color = Theme.colors.accent
        )
        Text(
            text = "|",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Window: $lastUpdated",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
