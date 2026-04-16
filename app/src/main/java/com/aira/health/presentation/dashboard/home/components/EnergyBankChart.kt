package com.aira.health.presentation.dashboard.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.theme.Theme

@Composable
fun EnergyBankChart(
    energyPercent: Int,
    modifier: Modifier = Modifier
) {
    val accentColor = Theme.colors.accent
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Theme.colors.surfaceContainerLow)
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Dummy curve for visual approximation of the designs
            val path = Path().apply {
                moveTo(0f, height * 0.8f)
                quadraticBezierTo(width * 0.25f, height * 0.2f, width * 0.5f, height * 0.5f)
                quadraticBezierTo(width * 0.75f, height * 0.8f, width, height * 0.3f)
            }
            
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            
            val gradient = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent),
                startY = 0f,
                endY = height
            )
            
            drawPath(
                path = fillPath,
                brush = gradient
            )
            
            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = 4f)
            )
        }
        
        // Marker
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .shadow(8.dp, spotColor = accentColor, ambientColor = accentColor)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Theme.colors.surfaceContainer)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$energyPercent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
