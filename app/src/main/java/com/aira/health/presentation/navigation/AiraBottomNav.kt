package com.aira.health.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aira.health.presentation.theme.Theme

data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AiraBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        BottomNavTab(AiraRoutes.HOME, "Home", Icons.Default.Home),
        BottomNavTab(AiraRoutes.INSIGHTS, "Insights", Icons.Default.AccessibilityNew),
        BottomNavTab(AiraRoutes.FUEL_TRAIN, "Fuel & Train", Icons.Default.Restaurant),
        BottomNavTab(AiraRoutes.ASSISTANT, "Assistant", Icons.Default.AutoAwesome),
        BottomNavTab(AiraRoutes.SETTINGS, "Settings", Icons.Default.Settings)
    )

    // Glassmorphism effect background mimicking iOS frosted glass (Clinical Ghost)
    com.aira.health.presentation.common.components.GlassContainer(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
            .height(84.dp),
        cornerRadius = 999.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    tab = tab,
                    isSelected = isSelected,
                    onClick = { onNavigate(tab.route) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    modifier: Modifier = Modifier,
    tab: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) Theme.colors.accent else Color.White.copy(alpha = 0.4f)
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = tween(300),
        label = "nav item scale"
    )
    
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.1f else 0.0f,
        animationSpec = tween(300),
        label = "nav item bg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    color = Theme.colors.accent.copy(alpha = bgAlpha),
                    shape = RoundedCornerShape(999.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = color
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
    }
}
