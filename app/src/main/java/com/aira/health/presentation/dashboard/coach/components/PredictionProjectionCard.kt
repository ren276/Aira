package com.aira.health.presentation.dashboard.coach.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.dashboard.coach.ProjectionCardModel
import com.aira.health.presentation.theme.Theme

@Composable
fun PredictionProjectionCard(
    model: ProjectionCardModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("coach-card-projection")
            .background(
                color = Theme.colors.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Projected Deltas",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Recovery",
                style = MaterialTheme.typography.bodyMedium,
                color = Theme.colors.onSurfaceVariant,
            )
            Text(
                text = model.projectedRecoveryDelta.toSignedValue(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Energy",
                style = MaterialTheme.typography.bodyMedium,
                color = Theme.colors.onSurfaceVariant,
            )
            Text(
                text = model.projectedEnergyDelta.toSignedValue(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }

        Text(
            text = "Burnout outlook: ${model.projectedBurnoutTier.lowercase()} (${model.projectedBurnoutTrajectory.lowercase()})",
            style = MaterialTheme.typography.bodySmall,
            color = Theme.colors.onSurfaceVariant,
        )

        Text(
            text = "Confidence ${model.confidenceTier.name.lowercase()} (${(model.confidenceScore * 100).toInt()}%)",
            style = MaterialTheme.typography.labelMedium,
            color = Theme.colors.secondaryColor,
        )

        model.uncertaintyLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.tertiaryColor,
            )
        }
    }
}

private fun Int.toSignedValue(): String = if (this > 0) "+$this" else toString()
