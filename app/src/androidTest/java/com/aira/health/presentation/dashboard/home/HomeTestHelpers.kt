package com.aira.health.presentation.dashboard.home

import androidx.compose.runtime.Composable

/**
 * Test-only wrapper that exposes [_HomeSuccessContentForTest] for Compose instrumentation tests.
 * Lives in androidTest — does not leak into production code.
 */
@Composable
internal fun HomeSuccessContentTestWrapper(
    state: HomeUiState.Success,
    onMetricTap: (String) -> Unit = {}
) {
    _HomeSuccessContentForTest(state = state, onMetricTap = onMetricTap)
}
