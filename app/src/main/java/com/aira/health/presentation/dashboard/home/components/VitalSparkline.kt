package com.aira.health.presentation.dashboard.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aira.health.presentation.theme.Theme

@Composable
fun VitalSparkline(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Theme.colors.accent,
    strokeWidth: Float = 2f
) {
    if (dataPoints.isEmpty()) return
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val maxVal = dataPoints.maxOrNull() ?: 1f
        val minVal = dataPoints.minOrNull() ?: 0f
        val range = if (maxVal == minVal) 1f else maxVal - minVal
        
        val stepX = width / if (dataPoints.size > 1) (dataPoints.size - 1) else 1
        
        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            // Y is inverted (0 is top)
            val y = height - ((value - minVal) / range) * height
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                // simple curve roughly
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
}
