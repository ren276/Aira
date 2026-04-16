package com.aira.health.presentation.dashboard.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.aira.health.presentation.common.components.ConfidenceMetaRow
import com.aira.health.presentation.dashboard.home.components.ConcentricArcHero
import com.aira.health.presentation.dashboard.home.components.CausalAnomalyCard
import com.aira.health.presentation.dashboard.home.components.EnergyBankChart
import com.aira.health.presentation.dashboard.home.components.MetricGridCard
import com.aira.health.presentation.theme.Theme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    modifier: Modifier = Modifier,
    onMetricTap: (metricId: String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val headerName = when (val state = uiState) {
        is HomeUiState.Success -> state.userName
        is HomeUiState.Empty -> state.userName
        else -> "Athlete"
    }
    val headerGreeting = when (val state = uiState) {
        is HomeUiState.Success -> state.greeting
        is HomeUiState.Empty -> state.greeting
        else -> "Morning"
    }
    val headerHeadline = when (val state = uiState) {
        is HomeUiState.Success -> state.statusHeadline
        is HomeUiState.Empty -> state.statusHeadline
        else -> "Loading..."
    }
    val headerConfidence = (uiState as? HomeUiState.Success)
        ?.confidence
        ?.times(100f)
        ?.toInt()
        ?.coerceIn(0, 100)
        ?.let { "$it% Confidence" }
    val headerLastUpdated = (uiState as? HomeUiState.Success)
        ?.lastUpdated
        ?.let(::formatLastUpdatedLabel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant)
    ) {
        // Top App Bar placeholder for the new UI topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerState = uiState as? HomeUiState.Success

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Theme.colors.surfaceContainerHighest)
                ) {
                    // Place coil image here later
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$headerGreeting, $headerName",
                        style = MaterialTheme.typography.labelMedium,
                        color = Theme.colors.onSurfaceVariant
                    )
                    Text(
                        text = headerHeadline,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            if (headerConfidence != null && headerLastUpdated != null) {
                Column(horizontalAlignment = Alignment.End) {
                    ConfidenceMetaRow(
                        confidence = headerConfidence,
                        lastUpdated = headerLastUpdated
                    )
                }
            }
        }

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Theme.colors.accent)
                }
            }
            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Theme.colors.destructive)
                }
            }
            is HomeUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(color = Theme.colors.accent)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Theme.colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::requestRefresh) {
                            Text("Refresh data")
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                HomeSuccessContent(
                    state = state,
                    onMetricTap = onMetricTap,
                    onRefresh = viewModel::requestRefresh
                )
            }
        }
    }
}

@Composable
internal fun _HomeSuccessContentForTest(
    state: HomeUiState.Success,
    onMetricTap: (String) -> Unit,
    onRefresh: () -> Unit = {}
) {
    HomeSuccessContent(
        state = state,
        onMetricTap = onMetricTap,
        onRefresh = onRefresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSuccessContent(
    state: HomeUiState.Success,
    onMetricTap: (String) -> Unit,
    onRefresh: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isSyncing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp), // Space for floating nav
            modifier = Modifier.fillMaxSize()
        ) {
            // Concentric Hero
            item {
                ConcentricArcHero(
                    readinessScore = state.recoveryScore,
                    strainPct = state.strainScore / 21f,
                    recoveryPct = state.recoveryScore / 100f,
                    sleepPct = state.sleepScore / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem("Strain", "${state.strainScore}", Theme.colors.tertiaryColor)
                    LegendItem("Recovery", "${state.recoveryScore}%", Theme.colors.accent)
                    val sleepLabel = state.sleepDurationHours
                        ?.let { "${String.format("%.1f", it)}h" }
                        ?: "${state.sleepScore}%"
                    LegendItem("Sleep", sleepLabel, Theme.colors.secondaryColor)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Default Anomaly / Insight Card from designs
            item {
                CausalAnomalyCard(
                    anomaly = state.anomaly,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Energy Bank
            item {
                Text(
                    text = "Energy Bank",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                EnergyBankChart(
                    energyPercent = state.energyBankPct,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Movement Snapshot
            item {
                Text(
                    text = "Movement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Theme.colors.surfaceContainerHighest)
                        .border(1.dp, Theme.colors.surfaceContainer, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MovementTextRow(
                            label = "Steps",
                            value = state.totalSteps?.toString() ?: "No step data"
                        )
                        MovementTextRow(
                            label = "Calories",
                            value = state.activeCalories?.let { "${it} kcal" } ?: "No calorie data"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Vitals 2x2 Grid
            item {
                Text(
                    text = "Daily Scores",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp) // Fixed height to prevent nested scroll crashes
                        .padding(horizontal = 16.dp),
                    userScrollEnabled = false
                ) {
                    item {
                        MetricGridCard(
                            metricId = "recovery",
                            label = "Recovery",
                            value = state.recoveryScore.toString(),
                            unit = "",
                            sparklineData = state.recoveryHistory,
                            onTap = { onMetricTap("recovery") }
                        )
                    }
                    item {
                        MetricGridCard(
                            metricId = "sleep",
                            label = "Sleep",
                            value = state.sleepScore.toString(),
                            unit = "",
                            sparklineData = state.sleepHistory,
                            onTap = { onMetricTap("sleep") }
                        )
                    }
                    item {
                        MetricGridCard(
                            metricId = "strain",
                            label = "Strain",
                            value = state.strainScore.toString(),
                            unit = "",
                            sparklineData = state.strainHistory,
                            onTap = { onMetricTap("strain") }
                        )
                    }
                    item {
                        MetricGridCard(
                            metricId = "stress",
                            label = "Stress",
                            value = state.stressScore.toString(),
                            unit = "",
                            sparklineData = state.stressHistory,
                            onTap = { onMetricTap("stress") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = Theme.colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun MovementTextRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

private fun formatLastUpdatedLabel(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
    val instant = Instant.ofEpochMilli(epochMillis)
    return "Updated ${formatter.format(instant.atZone(ZoneId.systemDefault()))}"
}
