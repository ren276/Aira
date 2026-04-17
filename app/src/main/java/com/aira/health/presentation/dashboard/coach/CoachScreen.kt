package com.aira.health.presentation.dashboard.coach

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.dashboard.home.HomeUiState
import com.aira.health.presentation.dashboard.home.HomeViewModel
import com.aira.health.presentation.theme.Theme

@Composable
fun CoachScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var inputText by remember { mutableStateOf("") }
    val homeState by viewModel.uiState.collectAsState()
    val success = homeState as? HomeUiState.Success
    val recommendation = success?.let { buildCoachRecommendation(it) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Theme.colors.primaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Theme.colors.primaryContainer)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Aira Intelligence",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                
                // Context Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Theme.colors.surfaceContainerHighest)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Using today's data", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            
            // Chat List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    AiraMessage(
                        text = recommendation?.intro
                            ?: "I am waiting for enough local data to generate a high-confidence coaching brief.",
                        chips = recommendation?.chips ?: emptyList()
                    )
                }
                item {
                    AiraClinicalRecommendation(
                        title = recommendation?.title ?: "Recommendation: Build your baseline first",
                        text = recommendation?.body
                            ?: "Run a short, low-intensity session and refresh sync after your workout to improve model confidence.",
                        bulletPoints = recommendation?.bullets ?: emptyList()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Space for input field
                }
            }
        }
        
        // Bottom Input Pannel
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 120.dp) // Above nav pill
                .clip(RoundedCornerShape(32.dp))
                .background(Theme.colors.surfaceContainerLow)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .padding(4.dp)
        ) {
            ChatInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = { inputText = "" }
            )
        }
    }
}

@Composable
fun AiraMessage(text: String, chips: List<Pair<String, Color>> = emptyList()) {
    Column(modifier = Modifier.padding(end = 48.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 4.dp))
                .background(Theme.colors.surfaceContainerLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 4.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    lineHeight = 24.sp
                )
                if (chips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chips.forEach { (label, color) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserMessage(text: String) {
    Column(modifier = Modifier.padding(start = 48.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 4.dp, bottomStart = 24.dp))
                .background(Theme.colors.accent.copy(alpha = 0.1f))
                .border(1.dp, Theme.colors.accent.copy(alpha = 0.3f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 4.dp, bottomStart = 24.dp))
                .padding(16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun AiraClinicalRecommendation(title: String, text: String, bulletPoints: List<String>) {
    Column(modifier = Modifier.padding(end = 24.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Theme.colors.surfaceContainerLow)
                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row {
                Box(modifier = Modifier.width(4.dp).height(120.dp).background(Theme.colors.accent))
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Theme.colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Theme.colors.accent)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    
                    if (bulletPoints.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        bulletPoints.forEach { pt ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha=0.5f)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pt, style = MaterialTheme.typography.bodyMedium, color = Theme.colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Ask Aira...", style = MaterialTheme.typography.bodyLarge, color = Theme.colors.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Theme.colors.accent
        ),
        trailingIcon = {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotEmpty()) Theme.colors.accent else Theme.colors.surfaceContainerHigh)
            ) {
                Icon(
                    Icons.Default.Send, 
                    contentDescription = "Send", 
                    tint = if (value.isNotEmpty()) Color.Black else Theme.colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

private data class CoachRecommendation(
    val intro: String,
    val title: String,
    val body: String,
    val bullets: List<String>,
    val chips: List<Pair<String, Color>>
)

@Composable
private fun buildCoachRecommendation(state: HomeUiState.Success): CoachRecommendation {
    val intensity = when {
        state.recoveryScore >= 80 && state.strainScore <= 70 -> "high"
        state.recoveryScore >= 60 -> "moderate"
        else -> "low"
    }

    val title = when (intensity) {
        "high" -> "Recommendation: Green Light for High Intensity"
        "moderate" -> "Recommendation: Moderate Progressive Session"
        else -> "Recommendation: Recovery-First Day"
    }

    val body = when (intensity) {
        "high" -> "Recovery and confidence are both strong. Use your hardest work block early, then cap total load before fatigue spillover."
        "moderate" -> "Your physiology is stable but not fully peaked. Prioritise quality reps and maintain moderate cardio intensity."
        else -> "Current recovery and sleep suggest reduced adaptive capacity. Focus on mobility, walking, and parasympathetic recovery work."
    }

    val bullets = when (intensity) {
        "high" -> listOf("Target strain: 65-80", "Session duration: 45-60 min")
        "moderate" -> listOf("Target strain: 45-65", "Session duration: 30-50 min")
        else -> listOf("Target strain: <45", "Session duration: 20-40 min")
    }

    return CoachRecommendation(
        intro = "Today recovery is ${state.recoveryScore}, sleep is ${state.sleepScore}, and strain is ${state.strainScore}. I can tune today's recommendation from this local profile.",
        title = title,
        body = body,
        bullets = bullets,
        chips = listOf(
            "Recovery: ${state.recoveryScore}" to Theme.colors.accent,
            "Strain: ${state.strainScore}" to Theme.colors.tertiaryColor,
            "Confidence: ${(state.confidence * 100).toInt()}%" to Theme.colors.secondaryColor
        )
    )
}
