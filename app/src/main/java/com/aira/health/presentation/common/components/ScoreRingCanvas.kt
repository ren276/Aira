package com.aira.health.presentation.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aira.health.presentation.theme.Theme

@Composable
fun ScoreRingCanvas(
    score: Int,
    modifier: Modifier = Modifier,
    thickness: Float = 12f
) {
    val clampedScore = score.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedScore / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "scoreRingProgress"
    )

    val trackColor = Theme.colors.secondary
    val activeColor = Theme.colors.accent

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .drawWithCache {
                onDrawBehind {
                    val stroke = Stroke(width = thickness, cap = StrokeCap.Round)
                    
                    // Draw track
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = stroke
                    )
                    
                    // Draw active score
                    drawArc(
                        color = activeColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        style = stroke
                    )
                }
            }
    )
}
