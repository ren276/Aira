package com.aira.health.presentation.supplementary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.DailyMetricsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InsightsPredictionsUiState(
    val hasPrediction: Boolean = false,
    val title: String = "Prediction Status",
    val message: String = "Predictions are unavailable until enough local trend data is synced.",
    val action: String = "Sync new sessions to unlock personalized prediction guidance."
)

@HiltViewModel
class InsightsPredictionsViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao
) : ViewModel() {

    val uiState: StateFlow<InsightsPredictionsUiState> = dailyMetricsDao.observeRecent(7)
        .map { recent ->
            if (recent.size < 3) {
                InsightsPredictionsUiState(
                    hasPrediction = false,
                    title = "Prediction Status",
                    message = "Not enough trend depth yet. Keep syncing to unlock robust next-day forecasting.",
                    action = "Collect at least 3 days of recovery/sleep records."
                )
            } else {
                val chronological = recent.sortedBy { it.date }
                val latest = chronological.last()
                val averageRecovery = chronological.map { it.recoveryScore }.average().toInt()
                val averageStrain = chronological.map { it.strainScore }.average().toInt()

                val projected = ((latest.recoveryScore * 0.65f) + ((100 - averageStrain) * 0.35f))
                    .toInt()
                    .coerceIn(0, 100)

                val guidance = when {
                    projected >= 80 -> "High-adaptation window expected tomorrow if you preserve tonight's sleep quality."
                    projected >= 60 -> "Stable readiness expected tomorrow with moderate load and consistent bedtime."
                    else -> "Recovery risk expected tomorrow; lower strain and prioritize sleep extension."
                }

                InsightsPredictionsUiState(
                    hasPrediction = true,
                    title = "Tomorrow Forecast: $projected% readiness",
                    message = "Based on recent recovery trend ($averageRecovery%) and average strain ($averageStrain), your projected readiness is $projected%.",
                    action = guidance
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InsightsPredictionsUiState()
        )
}
