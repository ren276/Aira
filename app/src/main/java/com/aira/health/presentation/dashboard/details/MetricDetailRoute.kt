package com.aira.health.presentation.dashboard.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.dashboard.details.components.ExplanationBottomSheet

/**
 * Shared routing wrapper for all metric details.
 * Connects the [MetricDetailViewModel] and manages the overarching state handling
 * and fallback rendering before delegating strictly metric-specific rendering to child composables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailRoute(
    modifier: Modifier = Modifier,
    viewModel: MetricDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is MetricDetailUiState.Loading -> {
                Text("Loading...")
            }
            is MetricDetailUiState.Error -> {
                Text(text = "Error: ${state.message}")
            }
            is MetricDetailUiState.Success -> {
                // Delegate to specific metric screens
                when (state.metricType) {
                    MetricType.RECOVERY -> com.aira.health.presentation.dashboard.details.screens.RecoveryDetailScreen(state = state)
                    MetricType.SLEEP -> com.aira.health.presentation.dashboard.details.screens.SleepDetailScreen(state = state)
                    MetricType.STRAIN -> com.aira.health.presentation.dashboard.details.screens.StrainDetailScreen(state = state)
                    MetricType.STRESS -> com.aira.health.presentation.dashboard.details.screens.StressDetailScreen(state = state)
                }
                
                // Demo logic: This is currently always visible for the slice test.
                // In full integration this would be conditionally shown via a state trigger.
                ExplanationBottomSheet(
                    whatChanged = state.whatChanged,
                    whyItMatters = state.whyItMatters,
                    whatToDoNext = state.whatToDoNext,
                    onDismissRequest = { /* Handle bottom sheet dismissal */ }
                )
            }
        }
    }
}
