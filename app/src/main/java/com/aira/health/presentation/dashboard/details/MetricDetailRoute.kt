package com.aira.health.presentation.dashboard.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var showExplanationSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is MetricDetailUiState.Loading -> {
                Text("Loading...")
            }
            is MetricDetailUiState.Error -> {
                Text(text = "Error: ${state.message}")
            }
            is MetricDetailUiState.Success -> {
                when (state.metricType) {
                    MetricType.RECOVERY -> RecoveryDetailScreen(state = state, onNavigateBack = onNavigateUp)
                    MetricType.SLEEP -> SleepDetailScreen(state = state, onNavigateBack = onNavigateUp)
                    MetricType.STRAIN -> StrainDetailScreen(state = state, onNavigateBack = onNavigateUp)
                    MetricType.STRESS -> StressDetailScreen(state = state, onNavigateBack = onNavigateUp)
                }

                Button(
                    onClick = { showExplanationSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text("Explain")
                }

                if (showExplanationSheet) {
                    ExplanationBottomSheet(
                        whatChanged = state.whatChanged,
                        whyItMatters = state.whyItMatters,
                        whatToDoNext = state.whatToDoNext,
                        onDismissRequest = { showExplanationSheet = false }
                    )
                }
            }
        }
    }
}
