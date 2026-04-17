package com.aira.health.presentation.dashboard.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MetricTrendWindow(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trend Window", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (dataPoints.size < 2) {
                Text("Not enough samples for trend chart.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val min = dataPoints.minOrNull() ?: return@Canvas
                val max = dataPoints.maxOrNull() ?: return@Canvas
                val range = (max - min).takeIf { it > 0f } ?: 1f

                val xStep = size.width / (dataPoints.size - 1)
                val path = Path()

                dataPoints.forEachIndexed { index, value ->
                    val x = index * xStep
                    val yRatio = (value - min) / range
                    val y = size.height - (yRatio * size.height)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF47EAED),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                val last = dataPoints.last()
                val lastY = size.height - (((last - min) / range) * size.height)
                drawCircle(
                    color = Color(0xFF47EAED),
                    radius = 6f,
                    center = Offset(size.width, lastY)
                )
            }
        }
    }
}
