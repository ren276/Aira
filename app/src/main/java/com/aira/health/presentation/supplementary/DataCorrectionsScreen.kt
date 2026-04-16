package com.aira.health.presentation.supplementary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.theme.Theme

@Composable
fun DataCorrectionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataCorrectionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
        contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Data Corrections",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        item {
            Text(
                text = "Model Baseline Alignments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CorrectionCard(
                title = if (state.sleepCorrections > 0) {
                    "Sleep baseline updated"
                } else {
                    "Sleep baseline update pending"
                },
                description = if (state.sleepCorrections > 0) {
                    "${state.sleepCorrections} correction(s) applied. Baseline: ${state.sleepBaselineLabel}."
                } else {
                    "No validated sleep correction events found. Baseline: ${state.sleepBaselineLabel}."
                },
                isApplied = state.sleepCorrections > 0
            )
            Spacer(modifier = Modifier.height(12.dp))
            CorrectionCard(
                title = if (state.hrvCorrections > 0) {
                    "HRV baseline updated"
                } else {
                    "HRV baseline update pending"
                },
                description = if (state.hrvCorrections > 0) {
                    "${state.hrvCorrections} correction(s) applied. Baseline: ${state.hrvBaselineLabel}."
                } else {
                    "Sync more nightly records to evaluate HRV baseline. Current baseline: ${state.hrvBaselineLabel}."
                },
                isApplied = state.hrvCorrections > 0
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.timelineMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Theme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun CorrectionCard(title: String, description: String, isApplied: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surfaceContainerLow)
            .border(0.5.dp, Color.White.copy(alpha=0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isApplied) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Theme.colors.accent)
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Theme.colors.secondaryColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Theme.colors.onSurfaceVariant)
        }
    }
}
