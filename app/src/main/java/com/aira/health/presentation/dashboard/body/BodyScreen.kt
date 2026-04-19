package com.aira.health.presentation.dashboard.body

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aira.health.presentation.dashboard.home.HomeViewModel
import com.aira.health.presentation.dashboard.home.HomeUiState
import com.aira.health.presentation.theme.Theme
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun BodyScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenCoach: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val success = state as? HomeUiState.Success
    val recoveryScore = success?.recoveryScore ?: 0
    val bodyNarrative = success?.let { deriveBodyNarrative(it) }
        ?: "Waiting for enough biometric data to build your daily body narrative."
    val prediction = success?.let { deriveRecoveryForecast(it) } ?: "--"

    val hrvConsistency = success?.let { computeConsistency(it.hrvHistory) } ?: 0f
    val strainBalance = success?.let { (100f - it.strainScore).coerceIn(0f, 100f) / 100f } ?: 0f
    val sleepQuality = success?.let { it.sleepScore.coerceIn(0, 100) / 100f } ?: 0f
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
        contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            onClick = onOpenCoach,
                            role = Role.Button
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Coach",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Theme.colors.accent,
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null, // Hidden from screen readers as the Row handles it
                        tint = Theme.colors.accent,
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Single Glow Arc (Recovery)
        item {
            RecoveryArcHero(score = recoveryScore, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Body Narrative
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Body Narrative",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Theme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bodyNarrative,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    lineHeight = 26.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Tomorrow Prediction Chip
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme.colors.primaryContainer.copy(alpha = 0.15f))
                    .border(0.5.dp, Theme.colors.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Theme.colors.primaryContainer))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Aira Prediction", style = MaterialTheme.typography.labelSmall, color = Theme.colors.primaryContainer)
                        Text("Tomorrow's recovery forecast: $prediction", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Contributors
        item {
            Text(
                text = "Contributors",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
            
            ContributorRow(
                "HRV Consistency",
                hrvConsistency,
                if (hrvConsistency >= 0.7f) Theme.colors.accent else Theme.colors.secondaryColor,
                labelForScore(hrvConsistency)
            )
            ContributorRow(
                "Strain Balance",
                strainBalance,
                if (strainBalance >= 0.6f) Theme.colors.accent else Theme.colors.tertiaryColor,
                labelForScore(strainBalance)
            )
            ContributorRow(
                "Sleep Quality",
                sleepQuality,
                if (sleepQuality >= 0.7f) Theme.colors.accent else Theme.colors.tertiaryColor,
                labelForScore(sleepQuality)
            )
        }
    }
}

@Composable
fun RecoveryArcHero(score: Int, modifier: Modifier = Modifier) {
    val animFill = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animFill.animateTo(score / 100f, tween(1000, easing = FastOutSlowInEasing))
    }
    
    Box(
        modifier = modifier.height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        val accentColor = Theme.colors.accent
        
        Canvas(modifier = Modifier
            .size(240.dp)
            .shadow(24.dp, shape = CircleShape, spotColor = accentColor.copy(alpha = 0.5f))) {
            val strokeW = 16.dp.toPx()
            
            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            
            // Fill
            drawArc(
                color = accentColor,
                startAngle = -220f,
                sweepAngle = 260f * animFill.value,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RECOVERY",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                color = Theme.colors.onSurfaceVariant
            )
            Text(
                text = "$score%",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
        }
    }
}

@Composable
fun ContributorRow(label: String, fillFraction: Float, color: Color, status: String) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(status, style = MaterialTheme.typography.labelMedium, color = color)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress bar
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Theme.colors.surfaceContainerHighest)) {
            Box(modifier = Modifier
                .fillMaxWidth(fillFraction)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color))
        }
    }
}

private fun deriveBodyNarrative(state: HomeUiState.Success): String {
    val readiness = state.recoveryScore
    val strain = state.strainScore
    val sleep = state.sleepScore

    return when {
        readiness >= 80 && strain in 35..70 ->
            "Recovery and sleep are both strong today. Your autonomic profile supports a productive training day with controlled high-intensity blocks."
        readiness < 55 || sleep < 55 ->
            "Your recovery inputs are currently suppressed. Keep effort moderate, prioritise hydration, and use low-intensity movement to preserve adaptation."
        strain > 80 ->
            "Current load is elevated relative to recovery. Maintain technique work or Zone 1-2 sessions to avoid carrying fatigue into tomorrow."
        else ->
            "Biometrics are stable with moderate strain. A balanced session is recommended while monitoring your evening recovery trend."
    }
}

private fun deriveRecoveryForecast(state: HomeUiState.Success): String {
    val projected = (state.recoveryScore * 0.65f + state.sleepScore * 0.35f).coerceIn(0f, 100f)
    val lower = (projected - 4f).coerceIn(0f, 100f).toInt()
    val upper = (projected + 4f).coerceIn(0f, 100f).toInt()
    return "$lower-$upper%"
}

private fun computeConsistency(history: List<Float>): Float {
    if (history.size < 2) return 0f
    val mean = history.average().toFloat().coerceAtLeast(1f)
    val variance = history.map { (it - mean).pow(2) }.average().toFloat()
    val std = sqrt(variance)
    return (1f - (std / mean)).coerceIn(0f, 1f)
}

private fun labelForScore(score: Float): String = when {
    score >= 0.75f -> "Optimal"
    score >= 0.5f -> "Good"
    else -> "Suboptimal"
}
