package com.aira.health.presentation.dashboard.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aira.health.presentation.theme.Theme

@Composable
fun ConcentricArcHero(
    readinessScore: Int,
    strainPct: Float,
    recoveryPct: Float,
    sleepPct: Float,
    modifier: Modifier = Modifier
) {
    val animStrain = remember { Animatable(0f) }
    val animRecovery = remember { Animatable(0f) }
    val animSleep = remember { Animatable(0f) }

    LaunchedEffect(strainPct, recoveryPct, sleepPct) {
        animStrain.animateTo(strainPct, tween(1000, easing = FastOutSlowInEasing))
        animRecovery.animateTo(recoveryPct, tween(1200, easing = FastOutSlowInEasing))
        animSleep.animateTo(sleepPct, tween(1400, easing = FastOutSlowInEasing))
    }

    val strainColor = Theme.colors.tertiaryColor
    val recoveryColor = Theme.colors.accent
    val sleepColor = Theme.colors.secondaryColor
    val trackColor = Theme.colors.surfaceContainerHighest
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val strokeWidth = size.width * 0.04f
            val spacing = strokeWidth * 1.5f
            
            // Outer: Strain
            val radiusOuter = size.width / 2 - strokeWidth
            drawArc(
                color = trackColor,
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - radiusOuter, center.y - radiusOuter),
                size = Size(radiusOuter * 2, radiusOuter * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = strainColor,
                startAngle = -220f,
                sweepAngle = 260f * animStrain.value,
                useCenter = false,
                topLeft = Offset(center.x - radiusOuter, center.y - radiusOuter),
                size = Size(radiusOuter * 2, radiusOuter * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Middle: Recovery
            val radiusMid = radiusOuter - spacing
            drawArc(
                color = trackColor,
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - radiusMid, center.y - radiusMid),
                size = Size(radiusMid * 2, radiusMid * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = recoveryColor,
                startAngle = -220f,
                sweepAngle = 260f * animRecovery.value,
                useCenter = false,
                topLeft = Offset(center.x - radiusMid, center.y - radiusMid),
                size = Size(radiusMid * 2, radiusMid * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Inner: Sleep
            val radiusInner = radiusMid - spacing
            drawArc(
                color = trackColor,
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - radiusInner, center.y - radiusInner),
                size = Size(radiusInner * 2, radiusInner * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = sleepColor,
                startAngle = -220f,
                sweepAngle = 260f * animSleep.value,
                useCenter = false,
                topLeft = Offset(center.x - radiusInner, center.y - radiusInner),
                size = Size(radiusInner * 2, radiusInner * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "READINESS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Theme.colors.onSurfaceVariant
            )
            Text(
                text = readinessScore.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp
                ),
                color = Color.White
            )
        }
    }
}
