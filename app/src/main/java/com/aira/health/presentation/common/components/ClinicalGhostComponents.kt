package com.aira.health.presentation.common.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.theme.Theme

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // Glassmorphism 2.0 per DESIGN.md: surface_variant at 60%, 0.5px ghost border at 15%
    val glassColor = Theme.colors.surfaceContainerHighest.copy(alpha = 0.6f)
    val borderColor = Theme.colors.outlineVariant.copy(alpha = 0.15f)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

@Composable
fun PredictiveChip(
    text: String,
    stateColor: Color = Theme.colors.accent,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    GlassContainer(cornerRadius = 999.dp, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(stateColor.copy(alpha = alpha))
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Theme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SparklineChart(
    data: List<Float>,
    color: Color = Theme.colors.accent,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier) {
        val max = data.maxOrNull() ?: 1f
        val min = data.minOrNull() ?: 0f
        val range = (max - min).takeIf { it > 0 } ?: 1f
        
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        
        val path = Path()
        data.forEachIndexed { index, value ->
            val normalizedY = 1f - ((value - min) / range)
            val x = index * stepX
            val y = normalizedY * size.height
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun ConcentricRingsChart(
    proteinPct: Float,
    carbsPct: Float,
    fatPct: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 8.dp
) {
    val proteinColor = Theme.colors.tertiaryColor
    val carbsColor = Theme.colors.secondaryColor
    val fatColor = Theme.colors.accent

    val pPct by animateFloatAsState(targetValue = proteinPct.coerceIn(0f, 1f), tween(1000), label = "p")
    val cPct by animateFloatAsState(targetValue = carbsPct.coerceIn(0f, 1f), tween(1000, 100), label = "c")
    val fPct by animateFloatAsState(targetValue = fatPct.coerceIn(0f, 1f), tween(1000, 200), label = "f")

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = (size.minDimension / 2) - thickness.toPx()
        val spacing = thickness.toPx() + 4.dp.toPx()

        val drawRing = { radius: Float, pct: Float, ringColor: Color ->
            drawArc(
                color = ringColor.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
            )
            if (pct > 0) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * pct,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        drawRing(baseRadius, pPct, proteinColor)
        drawRing(baseRadius - spacing, cPct, carbsColor)
        drawRing(baseRadius - spacing * 2, fPct, fatColor)
    }
}

@Composable
fun AiraHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    profileImageUrl: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = Theme.colors.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Theme.colors.onSurfaceVariant
            )
        }
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Theme.colors.surfaceContainerHighest.copy(alpha = 0.6f))
                .border(0.5.dp, Theme.colors.outlineVariant.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A", 
                style = MaterialTheme.typography.headlineMedium,
                color = Theme.colors.onSurface
            )
        }
    }
}

