package com.aira.health.presentation.dashboard.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.common.components.ConfidenceMetaRow
import com.aira.health.presentation.common.components.ScoreRingCanvas
import com.aira.health.presentation.dashboard.home.DeltaDirection
import com.aira.health.presentation.dashboard.home.ScoreDelta
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clinical metric card for the Home 2×2 grid.
 *
 * Design contract (D-07, D-08):
 *  - Always shows score ring, label, confidence, and last-updated context
 *  - [delta] non-null triggers a brief animated delta colour flash
 *  - Tap routes to metric detail via [onTap] callback
 *  - Semantic tag [metricId] is attached for UI test anchoring
 */
@Composable
fun MetricGridCard(
    metricId: String,
    label: String,
    score: Int,
    confidence: Float,
    lastUpdated: Long,
    modifier: Modifier = Modifier,
    delta: ScoreDelta? = null,
    onTap: (metricId: String) -> Unit = {}
) {
    // Brief flash animation when a delta arrives
    var flashActive by remember { mutableStateOf(false) }
    LaunchedEffect(delta) {
        if (delta != null) {
            flashActive = true
            delay(800)
            flashActive = false
        }
    }

    val flashColor = when (delta?.direction) {
        DeltaDirection.UP   -> Theme.colors.accent.copy(alpha = 0.15f)
        DeltaDirection.DOWN -> Theme.colors.destructive.copy(alpha = 0.15f)
        else                -> Color.Transparent
    }
    val bgTint by animateColorAsState(
        targetValue = if (flashActive) flashColor else Color.Transparent,
        animationSpec = tween(400),
        label = "cardFlash"
    )

    val confidenceLabel = "%.0f%%".format(confidence * 100)
    val updatedLabel    = remember(lastUpdated) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastUpdated))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Theme.colors.secondary)
            .background(bgTint)
            .clickable { onTap(metricId) }
            .semantics { contentDescription = "$label metric card" }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AiraSpacing.sm)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            ScoreRingCanvas(
                score = score,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            ConfidenceMetaRow(
                confidence  = confidenceLabel,
                lastUpdated = updatedLabel,
                modifier    = Modifier.fillMaxWidth()
            )

            // Delta badge
            if (delta != null) {
                DeltaBadge(delta = delta)
            }
        }
    }
}

@Composable
private fun DeltaBadge(delta: ScoreDelta) {
    val sign  = if (delta.direction == DeltaDirection.UP) "+" else ""
    val color = if (delta.direction == DeltaDirection.UP) Theme.colors.accent
                else Theme.colors.destructive
    Text(
        text  = "$sign${delta.current - delta.previous}",
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Bold
    )
}
