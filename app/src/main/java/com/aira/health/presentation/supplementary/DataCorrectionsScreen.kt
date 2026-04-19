package com.aira.health.presentation.supplementary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

        item {
            Text(
                text = "Correction Impact Preview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CorrectionTargetChip(
                    label = CorrectionTarget.SLEEP.displayLabel,
                    selected = state.selectedTarget == CorrectionTarget.SLEEP,
                    onClick = {
                        viewModel.selectTarget(CorrectionTarget.SLEEP)
                    },
                    modifier = Modifier.weight(1f)
                )
                CorrectionTargetChip(
                    label = CorrectionTarget.HRV.displayLabel,
                    selected = state.selectedTarget == CorrectionTarget.HRV,
                    onClick = {
                        viewModel.selectTarget(CorrectionTarget.HRV)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.originalValueInput,
                onValueChange = { viewModel.updateOriginalValue(it) },
                label = { Text("Original value") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("original-value-input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.correctedValueInput,
                onValueChange = { viewModel.updateCorrectedValue(it) },
                label = { Text("Corrected value") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("corrected-value-input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Theme.colors.surfaceContainerLow)
                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .testTag("correction-preview-card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val preview = state.preview
                    Text(
                        text = "Affected area: ${preview?.affectedParameter ?: "Not available"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Influence window: ${preview?.influenceWindowLabel ?: "next 14 days"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.colors.onSurfaceVariant
                    )
                    Text(
                        text = "Maximum influence cap: ${(preview?.maxInfluenceCap?.times(100f) ?: 20f).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("correction-confirmation-row"),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = state.confirmationChecked,
                    onCheckedChange = { viewModel.updateConfirmation(it) },
                    modifier = Modifier.testTag("correction-confirmation-checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I understand this correction will shape future personalization over the next 14 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (state.insufficientData) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Not Enough Recent Data",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Theme.colors.secondaryColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = state.insufficientDataMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (!state.submitErrorMessage.isNullOrBlank()) {
                val submitErrorMessage = state.submitErrorMessage.orEmpty()
                Text(
                    text = submitErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.secondaryColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (!state.submitSuccessMessage.isNullOrBlank()) {
                val submitSuccessMessage = state.submitSuccessMessage.orEmpty()
                Text(
                    text = submitSuccessMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.accent,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("correction-success-message")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.previewInfluence()
                    viewModel.applyCorrection()
                },
                enabled = !state.isSubmitting && !state.insufficientData,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .testTag("apply-correction-button")
            ) {
                Text("Apply Correction")
            }
        }
    }
}

@Composable
private fun CorrectionTargetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Theme.colors.accent.copy(alpha = 0.25f) else Theme.colors.surfaceContainerLow)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
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
