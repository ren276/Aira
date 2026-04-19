package com.aira.health.presentation.dashboard.coach.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.dashboard.coach.WeeklyDraftCardModel
import com.aira.health.presentation.theme.Theme

@Composable
fun WeeklyPlanDraftCard(
    model: WeeklyDraftCardModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("coach-card-weekly-draft")
            .background(
                color = Theme.colors.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Weekly Plan Draft (${model.targetDate})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )

        Text(
            text = model.loadRecoveryBalanceSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )

        Text(
            text = model.weeklyFocus,
            style = MaterialTheme.typography.bodySmall,
            color = Theme.colors.onSurfaceVariant,
        )

        Text(
            text = "Priority actions",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Theme.colors.secondaryColor,
        )
        model.priorityActions.forEach { action ->
            Text(
                text = "• $action",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }

        Text(
            text = "Caution notes",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Theme.colors.secondaryColor,
        )
        model.cautionNotes.forEach { caution ->
            Text(
                text = "• $caution",
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.onSurfaceVariant,
            )
        }

        Text(
            text = "Confidence ${model.confidenceTier.name.lowercase()} (${(model.confidenceScore * 100).toInt()}%)",
            style = MaterialTheme.typography.labelSmall,
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
