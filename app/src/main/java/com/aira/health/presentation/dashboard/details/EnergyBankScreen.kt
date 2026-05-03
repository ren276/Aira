package com.aira.health.presentation.dashboard.details

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.theme.Theme

@Composable
fun EnergyBankScreen(
    state: MetricDetailUiState.Success,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val energyPct = state.currentScore
    val trendAverage = if (state.trendDataPoints.isNotEmpty()) state.trendDataPoints.average().toFloat() else energyPct.toFloat()
    val trendDelta = energyPct - trendAverage
    val confidencePct = (state.confidence * 100).toInt().coerceIn(0, 100)
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
        contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp)
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
                    text = "Energy Bank",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Battery Hero Shell
        item {
            EnergyBatteryHero(percent = energyPct, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Restorative Events
        item {
            Text(
                text = "Restorative Events",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
            
            EventRow(
                title = "Trend vs 14-day baseline",
                impact = "${if (trendDelta >= 0f) "+" else ""}${trendDelta.toInt()}%",
                time = "from local score history"
            )
            EventRow(
                title = "Data confidence",
                impact = "$confidencePct%",
                time = "coverage across current inputs"
            )
            EventRow(
                title = "Action guidance",
                impact = if (energyPct >= 70) "Stable" else "Rebuild",
                time = state.whatToDoNext
            )
        }
    }
}

@Composable
fun EnergyBatteryHero(percent: Int, modifier: Modifier = Modifier) {
    val animFill = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        animFill.animateTo(percent / 100f, tween(1000))
    }
    
    Box(
        modifier = modifier.height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // "Battery" Shell
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(220.dp)
                    .border(2.dp, Theme.colors.surfaceContainerHighest, RoundedCornerShape(24.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Liquid fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(animFill.value)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Theme.colors.accent.copy(alpha = 0.8f))
                )
                
                // Overlay text
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun EventRow(title: String, impact: String, time: String) {
    com.aira.health.presentation.common.components.GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(time, style = MaterialTheme.typography.labelMedium, color = Theme.colors.onSurfaceVariant)
            }
            Text(
                impact,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Theme.colors.accent
            )
        }
    }
}
