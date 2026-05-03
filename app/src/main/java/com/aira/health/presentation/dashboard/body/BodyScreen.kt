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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Theme.colors.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Theme.colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AIRA",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                        color = Theme.colors.accent
                    )
                }
                Text(
                    text = "98% Confidence",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Theme.colors.accent
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Single Glow Arc (Recovery)
        item {
            RecoveryArcHero(
                score = recoveryScore,
                status = labelForScore(recoveryScore / 100f),
                trend = "+4%",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Body Narrative
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Body Narrative",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = bodyNarrative,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Light),
                    color = Theme.colors.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Contributors
        item {
            Text(
                text = "DAILY CONTRIBUTORS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Theme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            
            ContributorRow(
                category = "STRAIN BALANCE",
                label = "Physiological Load",
                fillFraction = strainBalance,
                color = Theme.colors.accent,
                percentageText = "${(strainBalance * 100).toInt()}%"
            )
            ContributorRow(
                category = "RESTORATION",
                label = "Sleep Quality",
                fillFraction = sleepQuality,
                color = Theme.colors.accent,
                percentageText = "${(sleepQuality * 100).toInt()}%"
            )
            ContributorRow(
                category = "CARDIO",
                label = "HRV Consistency",
                fillFraction = hrvConsistency,
                color = Theme.colors.secondaryColor,
                percentageText = "${(hrvConsistency * 100).toInt()}%"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Tomorrow Prediction Chip
        item {
            com.aira.health.presentation.common.components.GlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                cornerRadius = 16.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    com.aira.health.presentation.common.components.PredictiveChip(
                        text = "PREDICTING",
                        stateColor = Theme.colors.accent
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("AIRA PREDICTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Theme.colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tomorrow's Score: ", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text(prediction.replace("%", "").trim(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Theme.colors.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecoveryArcHero(score: Int, status: String, trend: String, modifier: Modifier = Modifier) {
    val animFill = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animFill.animateTo(score / 100f, tween(1000, easing = FastOutSlowInEasing))
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            val accentColor = Theme.colors.accent
            
            Canvas(modifier = Modifier
                .size(220.dp)
                .shadow(0.dp, shape = CircleShape)) {
                val strokeW = 12.dp.toPx()
                
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
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-10).dp)) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "RECOVERY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                    color = Theme.colors.accent
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.width(200.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("STATUS", style = MaterialTheme.typography.labelSmall, color = Theme.colors.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(status, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.secondaryColor)
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Theme.colors.onSurfaceVariant.copy(alpha=0.3f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TREND", style = MaterialTheme.typography.labelSmall, color = Theme.colors.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(trend, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Theme.colors.accent)
            }
        }
    }
}

@Composable
fun ContributorRow(category: String, label: String, fillFraction: Float, color: Color, percentageText: String) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(category, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Theme.colors.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text(percentageText, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = color)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress bar
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Theme.colors.surfaceContainerHighest)) {
            Box(modifier = Modifier
                .fillMaxWidth(fillFraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
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
            "Your cardiovascular efficiency has rebounded significantly. HRV stability suggests the nervous system is successfully integrating recent high-intensity loads.\n\nSleep architecture shows a solid increase in Deep Sleep cycles. This shift correlates with your optimized routine."
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
    val point = projected.toInt()
    return "$point"
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
