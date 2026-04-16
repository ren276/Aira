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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.theme.Theme

@Composable
fun StressDetailScreen(
    state: MetricDetailUiState.Success,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stressScore = state.currentScore
    val highMinutes = (stressScore / 100f * 240f).toInt().coerceIn(20, 360)
    val mediumMinutes = (stressScore / 100f * 420f).toInt().coerceIn(120, 600)
    val restMinutes = (24 * 60 - highMinutes - mediumMinutes).coerceAtLeast(60)
    
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
                    text = "Stress",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Stress Radar Hero
        item {
            StressRadarHero(score = stressScore, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Zones
        item {
            Text(
                text = "Time in Zones",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
            
            StressZoneRow("High", formatMinutes(highMinutes), Theme.colors.destructive)
            StressZoneRow("Medium", formatMinutes(mediumMinutes), Theme.colors.secondaryColor)
            StressZoneRow("Rest", formatMinutes(restMinutes), Theme.colors.accent)
        }
    }
}

@Composable
fun StressRadarHero(score: Int, modifier: Modifier = Modifier) {
    val animRadius = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animRadius.animateTo(score / 100f, tween(1000))
    }
    
    Box(
        modifier = modifier.height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        val secondaryColor = Theme.colors.secondaryColor
        Canvas(modifier = Modifier.size(240.dp)) {
            val width = size.width
            val center = Offset(width / 2, width / 2)
            val maxRadius = width / 2
            
            // Outer rings
            listOf(0.33f, 0.66f, 1f).forEach { scale ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = maxRadius * scale,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
            
            // Active zone
            drawCircle(
                color = secondaryColor.copy(alpha = 0.2f),
                radius = maxRadius * animRadius.value,
                center = center
            )
            drawCircle(
                color = secondaryColor,
                radius = maxRadius * animRadius.value,
                center = center,
                style = Stroke(width = 4f)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "STRESS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Theme.colors.onSurfaceVariant
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun StressZoneRow(label: String, time: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Theme.colors.surfaceContainerLow)
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            Text(
                time,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}
