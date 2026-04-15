package com.aira.health.presentation.dashboard.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.dashboard.home.AnomalyPayload
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme

/**
 * Always-present causal anomaly card (D-09).
 *
 * Contract:
 *  - When [anomaly] is non-null, displays the active anomaly title + description.
 *  - When [anomaly] is null, renders [ForecastGuidanceCard] as the fallback — never hides the card.
 *
 * This card MUST always be visible on the Home screen regardless of anomaly presence (D-09 invariant).
 */
@Composable
fun CausalAnomalyCard(
    anomaly: AnomalyPayload?,
    modifier: Modifier = Modifier
) {
    val cardBg = if (anomaly != null) {
        Theme.colors.caution.copy(alpha = 0.18f)
    } else {
        Theme.colors.secondary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .semantics { contentDescription = "causal anomaly card" }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(AiraSpacing.sm)
    ) {
        if (anomaly != null) {
            // Active anomaly view
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⚠ Active Insight",
                    style = MaterialTheme.typography.labelLarge,
                    color = Theme.colors.caution,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = anomaly.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = anomaly.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        } else {
            // Fallback forecast / prevention view (D-09)
            ForecastGuidanceCard()
        }
    }
}
