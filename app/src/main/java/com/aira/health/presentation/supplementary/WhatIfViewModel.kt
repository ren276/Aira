package com.aira.health.presentation.supplementary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.DailyMetricsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class WhatIfUiState(
    val currentRecovery: Int = 0,
    val currentSleep: Float = 0f,
    val hasSufficientData: Boolean = false,
    val guidance: String = "Sync sleep and recovery data to use simulations.",
    val isLoading: Boolean = true
)

@HiltViewModel
class WhatIfViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao
) : ViewModel() {

    private val today: String get() = LocalDate.now().toString()

    val uiState: StateFlow<WhatIfUiState> = dailyMetricsDao.observeByDate(today)
        .map { metrics ->
            if (metrics == null) {
                // Try fetching most recent if today is empty.
                val recent = dailyMetricsDao.getLast14Days().firstOrNull()
                val recovery = recent?.recoveryScore ?: 0
                val sleep = recent?.sleepDurationMin?.div(60f) ?: 0f
                val hasData = recovery > 0 && sleep > 0f
                WhatIfUiState(
                    currentRecovery = recovery,
                    currentSleep = sleep,
                    hasSufficientData = hasData,
                    guidance = if (hasData) {
                        "Projection is based on your most recent synced day."
                    } else {
                        "Not enough synced baseline data yet."
                    },
                    isLoading = false
                )
            } else {
                val recovery = metrics.recoveryScore
                val sleep = metrics.sleepDurationMin?.div(60f) ?: 0f
                val hasData = recovery > 0 && sleep > 0f
                WhatIfUiState(
                    currentRecovery = recovery,
                    currentSleep = sleep,
                    hasSufficientData = hasData,
                    guidance = if (hasData) {
                        "Projection uses today's synced physiology."
                    } else {
                        "Current day is incomplete. Sync wearables and try again."
                    },
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WhatIfUiState()
        )
}
