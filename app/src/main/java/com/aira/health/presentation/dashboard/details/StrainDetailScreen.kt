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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aira.health.presentation.theme.Theme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StrainDetailScreen(
    state: MetricDetailUiState.Success,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strainScore = state.currentScore.toFloat()
    val targetLower = (strainScore * 0.8f).coerceIn(10f, 95f)
    val targetUpper = (strainScore * 1.15f).coerceIn(15f, 100f)
    
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
                    text = "Strain",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Strain Dial Hero
        item {
            StrainDialHero(score = strainScore, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Causal Insight
        item {
            StrainInsightCard(
                title = if (strainScore >= 75f) "Strain Elevated" else "Strain In Range",
                message = state.whatChanged
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Target Range
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme.colors.surfaceContainerLow)
                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Optimal Range", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("Adaptive range from current load", style = MaterialTheme.typography.labelMedium, color = Theme.colors.onSurfaceVariant)
                    }
                    Text(
                        "${String.format("%.1f", targetLower)} - ${String.format("%.1f", targetUpper)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Theme.colors.tertiaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun StrainDialHero(score: Float, modifier: Modifier = Modifier) {
    val animFill = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animFill.animateTo(score / 21f, tween(1200))
    }
    
    Box(
        modifier = modifier.height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        val color = Theme.colors.tertiaryColor
        
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeW = 12.dp.toPx()
            
            // Dotted track
            for (i in 0..40) {
                val angle = -220f + (260f * (i / 40f))
                val angleRad = Math.toRadians(angle.toDouble())
                val r = size.width / 2
                val x = center.x + r * cos(angleRad).toFloat()
                val y = center.y + r * sin(angleRad).toFloat()
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            // Fill track
            drawArc(
                color = color,
                startAngle = -220f,
                sweepAngle = 260f * animFill.value,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                color = Theme.colors.onSurfaceVariant
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun StrainInsightCard(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.tertiaryColor.copy(alpha = 0.1f))
            .border(0.5.dp, Theme.colors.tertiaryColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Theme.colors.tertiaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Theme.colors.tertiaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Theme.colors.tertiaryColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}
