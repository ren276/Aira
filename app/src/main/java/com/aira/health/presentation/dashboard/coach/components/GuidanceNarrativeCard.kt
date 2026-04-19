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
import com.aira.health.presentation.dashboard.coach.GuidanceCardModel
import com.aira.health.presentation.theme.Theme

@Composable
fun GuidanceNarrativeCard(
    model: GuidanceCardModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("coach-card-guidance")
            .background(
                color = Theme.colors.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Guidance Narrative",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Text(
            text = model.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )

        model.actions.forEach { action ->
            Text(
                text = "• $action",
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.onSurfaceVariant,
            )
        }

        if (model.citations.isNotEmpty()) {
            Text(
                text = "Signals: ${model.citations.joinToString()}",
                style = MaterialTheme.typography.labelSmall,
                color = Theme.colors.secondaryColor,
            )
        }

        model.uncertaintyLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.tertiaryColor,
            )
        }
    }
}
