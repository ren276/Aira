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
    private val metricId: String = savedStateHandle["metricId"] ?: MetricType.RECOVERY.id
    
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

        val previousScore = trendMetrics
            .dropLast(1)
            .lastOrNull()
            ?.let { extractScore(it, metricType) }
        val trendAverage = if (dataPoints.isNotEmpty()) dataPoints.average().toFloat() else currentScore.toFloat()
        val delta = previousScore?.let { currentScore - it } ?: 0
        val vsAverage = currentScore - trendAverage

        MetricDetailUiState.Success(
            metricType = metricType,
            currentScore = currentScore,
            trendDataPoints = dataPoints,
            confidence = activeMetrics.dataConfidence, // T-04-08 preserved provenance
            whatChanged = buildWhatChanged(metricType, currentScore, delta, vsAverage),
            whyItMatters = buildWhyItMatters(metricType, activeMetrics.dataConfidence),
            whatToDoNext = buildWhatToDoNext(metricType, currentScore)
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

    private fun buildWhatChanged(
        type: MetricType,
        current: Int,
        deltaVsYesterday: Int,
        deltaVsAverage: Float
    ): String = when (type) {
        MetricType.RECOVERY ->
            "Recovery is $current (${signed(deltaVsYesterday)} vs yesterday, ${signed(deltaVsAverage.toInt())} vs 14-day trend)."
        MetricType.SLEEP ->
            "Sleep score is $current (${signed(deltaVsYesterday)} vs yesterday) with a ${signed(deltaVsAverage.toInt())} shift against your recent baseline."
        MetricType.STRAIN ->
            "Strain is $current (${signed(deltaVsYesterday)} vs yesterday), indicating ${if (current >= 70) "elevated" else "managed"} training load."
        MetricType.STRESS ->
            "Stress is $current (${signed(deltaVsYesterday)} vs yesterday), currently ${if (current >= 65) "above" else "within"} your normal daily band."
    }

    private fun buildWhyItMatters(type: MetricType, confidence: Float): String {
        val confidencePct = (confidence * 100).toInt().coerceIn(0, 100)
        val confidenceClause = "Model confidence is $confidencePct%, so this guidance reflects your current local data quality."

        return when (type) {
            MetricType.RECOVERY ->
                "Recovery summarizes how ready your system is for adaptation after prior strain and sleep load. $confidenceClause"
            MetricType.SLEEP ->
                "Sleep quality drives hormonal repair and nervous-system recalibration, directly affecting next-day readiness. $confidenceClause"
            MetricType.STRAIN ->
                "Strain captures exercise load; keeping it aligned with recovery avoids cumulative fatigue and plateau risk. $confidenceClause"
            MetricType.STRESS ->
                "Stress reflects autonomic load over the day; sustained high values can suppress recovery and learning readiness. $confidenceClause"
        }
    }

    private fun buildWhatToDoNext(type: MetricType, score: Int): String = when (type) {
        MetricType.RECOVERY -> when {
            score >= 80 -> "Proceed with a quality training block and protect hydration plus post-session downregulation."
            score >= 60 -> "Use moderate intensity and keep total load capped to protect tomorrow's recovery."
            else -> "Prioritise recovery: low-intensity movement, sleep extension, and breath-focused downshift work."
        }
        MetricType.SLEEP -> when {
            score >= 80 -> "Maintain current sleep routine and pre-bed wind-down timing."
            score >= 60 -> "Reduce evening stimulation and target a consistent bedtime for the next 2 nights."
            else -> "Shift tonight toward recovery: earlier lights-out, lower caffeine, and cooler room temperature."
        }
        MetricType.STRAIN -> when {
            score >= 75 -> "Keep next session lighter or technique-focused to avoid overload carryover."
            score >= 50 -> "Maintain balanced training with one high-quality effort and sufficient recovery spacing."
            else -> "If training today, gradually ramp load to stay inside your adaptive range."
        }
        MetricType.STRESS -> when {
            score >= 70 -> "Use low-intensity activity and scheduled recovery breaks to reduce autonomic load."
            score >= 50 -> "Maintain regular movement and add short breathing resets during the day."
            else -> "You are in a stable range; maintain current routines and monitor evening recovery signals."
        }
    }

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
}
