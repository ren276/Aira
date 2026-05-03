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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.common.components.GlassContainer
import com.aira.health.presentation.theme.Theme

@Composable
fun WhatIfSimulatorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WhatIfViewModel = hiltViewModel()
) {
    val state: WhatIfUiState by viewModel.uiState.collectAsState()
    var projectedSleep by remember(state.currentSleep, state.hasSufficientData) {
        mutableFloatStateOf(if (state.hasSufficientData) state.currentSleep else 0f)
    }
    
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Theme.colors.onSurface)
                }
                Text(
                    text = "What-If Simulator",
                    style = MaterialTheme.typography.titleMedium,
                    color = Theme.colors.onSurface
                )
            }
        }

        item {
            GlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                cornerRadius = 24.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Theme.colors.tertiaryColor.copy(alpha=0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = Theme.colors.tertiaryColor)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Recovery Simulator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Theme.colors.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "If I sleep for:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Theme.colors.onSurfaceVariant
                    )
                    if (state.hasSufficientData) {
                        Text(
                            text = "${String.format("%.1f", projectedSleep)} hours",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Theme.colors.tertiaryColor
                        )

                        Slider(
                            value = projectedSleep,
                            onValueChange = { projectedSleep = it },
                            valueRange = 4f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = Theme.colors.tertiaryColor,
                                activeTrackColor = Theme.colors.tertiaryColor
                            ),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Text(
                            text = "Data unavailable",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = Theme.colors.secondaryColor,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = state.guidance,
                            style = MaterialTheme.typography.bodySmall,
                            color = Theme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    DividerBox()
                    
                    Text(
                        text = "Projected Recovery:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Theme.colors.onSurfaceVariant
                    )
                    if (state.hasSufficientData) {
                        val baselineSleep = state.currentSleep.coerceAtLeast(0.1f)
                        val sleepRatio = projectedSleep / baselineSleep
                        val projectedRecovery = (state.currentRecovery * sleepRatio).coerceIn(0f, 100f).toInt()

                        Text(
                            text = "$projectedRecovery%",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (projectedRecovery > 80) Theme.colors.accent else Theme.colors.secondaryColor
                        )
                    } else {
                        Text(
                            text = "--",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Theme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DividerBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .height(1.dp)
            .background(Theme.colors.outlineVariant.copy(alpha = 0.2f))
    )
}
