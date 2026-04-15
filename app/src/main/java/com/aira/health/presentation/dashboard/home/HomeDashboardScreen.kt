@file:Suppress("FunctionName")
package com.aira.health.presentation.dashboard.home

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.dashboard.home.components.CausalAnomalyCard
import com.aira.health.presentation.dashboard.home.components.MetricGridCard
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme

/**
 * Home dashboard screen — the primary daily-intelligence surface.
 *
 * Layout contract (D-07):
 *  Grid order is FIXED: Recovery (0), Sleep (1), Strain (2), Stress (3)
 *  This order cannot be altered by runtime data — it is a clinically-motivated invariant.
 *
 * Behaviour (D-08):
 *  - Renders cached values immediately; never shows a spinner when data exists
 *  - Pull-to-refresh triggers silent background sync (non-blocking)
 *
 * Anomaly (D-09):
 *  - [CausalAnomalyCard] is ALWAYS rendered below the grid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    modifier: Modifier = Modifier,
    onMetricTap: (metricId: String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            actions = {
                if (uiState is HomeUiState.Success && (uiState as HomeUiState.Success).isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(12.dp),
                        strokeWidth = 2.dp,
                        color = Theme.colors.accent
                    )
                } else {
                    IconButton(onClick = viewModel::requestRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh health data",
                            tint = Theme.colors.accent
                        )
                    }
                }
            }
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> HomeLoadingContent()
            is HomeUiState.Error   -> HomeErrorContent(message = state.message)
            is HomeUiState.Success -> HomeSuccessContent(
                state      = state,
                onMetricTap = onMetricTap,
                onRefresh  = viewModel::requestRefresh
            )
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun _HomeSuccessContentForTest(
    state: HomeUiState.Success,
    onMetricTap: (String) -> Unit,
    onRefresh: () -> Unit = {}
) = HomeSuccessContent(state = state, onMetricTap = onMetricTap, onRefresh = onRefresh)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSuccessContent(
    state: HomeUiState.Success,
    onMetricTap: (String) -> Unit,
    onRefresh: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isSyncing,
        onRefresh    = onRefresh,
        modifier     = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = AiraSpacing.md, vertical = AiraSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AiraSpacing.md)
        ) {
            item {
                // FIXED clinical 2×2 grid — order is invariant (D-07)
                // Indices: [0] Recovery, [1] Sleep, [2] Strain, [3] Stress
                val gridItems = listOf(
                    Triple("recovery", "Recovery", state.recoveryScore) to state.recoveryDelta,
                    Triple("sleep",    "Sleep",    state.sleepScore)    to state.sleepDelta,
                    Triple("strain",   "Strain",   state.strainScore)   to state.strainDelta,
                    Triple("stress",   "Stress",   state.stressScore)   to state.stressDelta
                )

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(AiraSpacing.sm),
                    verticalArrangement   = Arrangement.spacedBy(AiraSpacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Fixed height — prevents nested scroll conflict with LazyColumn
                        .aspectRatio(1f)
                        .semantics { contentDescription = "metric grid" },
                    userScrollEnabled = false
                ) {
                    items(gridItems.size) { idx ->
                        val (triple, delta) = gridItems[idx]
                        val (id, label, score) = triple
                        MetricGridCard(
                            metricId    = id,
                            label       = label,
                            score       = score,
                            confidence  = state.confidence,
                            lastUpdated = state.lastUpdated,
                            delta       = delta,
                            onTap       = onMetricTap,
                            modifier    = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Anomaly card — always present (D-09)
            item {
                CausalAnomalyCard(
                    anomaly  = state.anomaly,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HomeLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Theme.colors.accent)
    }
}

@Composable
private fun HomeErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Theme.colors.destructive
        )
    }
}
