package com.aira.health.presentation.dashboard.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme

/**
 * Forecast / prevention guidance card shown when no active anomaly is detected.
 *
 * This is the fallback content rendered inside [CausalAnomalyCard] (D-09).
 * In future phases, real AI-generated guidance will replace the static content.
 */
@Composable
fun ForecastGuidanceCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics { contentDescription = "forecast guidance card" },
        verticalArrangement = Arrangement.spacedBy(AiraSpacing.xs)
    ) {
        Text(
            text = "✦ Forecast",
            style = MaterialTheme.typography.labelLarge,
            color = Theme.colors.accent,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Everything looks on track",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "No anomalies detected. Aira is monitoring your patterns and will alert you if anything needs attention.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
