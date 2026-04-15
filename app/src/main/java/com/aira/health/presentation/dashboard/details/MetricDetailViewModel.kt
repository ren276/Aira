package com.aira.health.presentation.dashboard.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * Shared ViewModel handling the data contract for all metric detail screens (D-05).
 * Resolves the metric ID from navigation arguments, loads trailing trend data,
 * and maintains the D-11 explanation state.
 */
@HiltViewModel
class MetricDetailViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Argument parsing is aligned with MetricDetailRoute contract
    private val metricId: String = checkNotNull(savedStateHandle["metricId"]) {
        "metricId argument is required"
    }
    
    // T-04-07: Mitigate invalid route arguments by fallback/erroring securely
    val metricType: MetricType? = MetricType.fromIdOrNull(metricId)

    /** Trend data for the metric, spanning the last 14 days */
    private val trendFlow = flow {
        // Fetch trailing 14 days
        val recent = dailyMetricsDao.getLast14Days()
        // Ensure they are ordered chronologically (oldest first)
        val sorted = recent.sortedBy { it.date }
        emit(sorted)
    }

    /** Today's value */
    private val todayFlow = flow {
        val todayStr = LocalDate.now().toString()
        val metrics = dailyMetricsDao.getRange(todayStr, todayStr).firstOrNull()
        emit(metrics)
    }

    val uiState: StateFlow<MetricDetailUiState> = combine(
        trendFlow,
        todayFlow
    ) { trendMetrics, todayMetrics ->
        if (metricType == null) {
            return@combine MetricDetailUiState.Error("Invalid or unknown metric type: $metricId")
        }

        if (todayMetrics == null && trendMetrics.isEmpty()) {
            return@combine MetricDetailUiState.Error("No data available for $metricId")
        }

        val activeMetrics = todayMetrics ?: trendMetrics.last()
        val currentScore = extractScore(activeMetrics, metricType)

        // Map trend data into continuous floats
        val dataPoints = trendMetrics.map { extractScore(it, metricType).toFloat() }

        // Mocks for Phase 04 UI contract (real AI narratives come in a later phase)
        MetricDetailUiState.Success(
            metricType = metricType,
            currentScore = currentScore,
            trendDataPoints = dataPoints,
            confidence = activeMetrics.dataConfidence, // T-04-08 preserved provenance
            whatChanged = generateMockWhatChanged(metricType),
            whyItMatters = generateMockWhyItMatters(metricType),
            whatToDoNext = generateMockWhatToDoNext(metricType)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MetricDetailUiState.Loading
    )

    private fun extractScore(metrics: DailyMetrics, type: MetricType): Int {
        return when (type) {
            MetricType.RECOVERY -> metrics.recoveryScore
            MetricType.SLEEP -> metrics.sleepScore
            MetricType.STRAIN -> metrics.strainScore
            MetricType.STRESS -> metrics.stressScore
        }
    }

    private fun generateMockWhatChanged(type: MetricType) = when (type) {
        MetricType.RECOVERY -> "Your morning HRV dropped by 12% following yesterday's strenuous activity."
        MetricType.SLEEP -> "Deep sleep duration fell below your 90-minute baseline."
        MetricType.STRAIN -> "Cardio load accumulated faster than typical for this time of day."
        MetricType.STRESS -> "Physiological stress markers remained elevated throughout the night."
    }

    private fun generateMockWhyItMatters(type: MetricType) = when (type) {
        MetricType.RECOVERY -> "Suppressed HRV indicates your central nervous system is still processing yesterday's load."
        MetricType.SLEEP -> "Deep sleep is critical for physical tissue repair and human growth hormone release."
        MetricType.STRAIN -> "Exceeding your capacity without adequate recovery breaks down muscle without supercompensation gains."
        MetricType.STRESS -> "Prolonged sympathetic dominant states inhibit digestion and immune response."
    }

    private fun generateMockWhatToDoNext(type: MetricType) = when (type) {
        MetricType.RECOVERY -> "Keep today's strain below 40. Hydrate well and stretch."
        MetricType.SLEEP -> "Avoid caffeine after 2 PM and reduce screen time an hour before bed."
        MetricType.STRAIN -> "Prioritise Zone 1/2 recovery work over high-intensity intervals."
        MetricType.STRESS -> "Take a 10-minute resonance breathing exercise to re-engage your parasympathetic system."
    }
}
