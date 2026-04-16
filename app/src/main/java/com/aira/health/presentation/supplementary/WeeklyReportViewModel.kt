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
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import javax.inject.Inject

data class WeeklyReportUiState(
    val dateRange: String = "",
    val headline: String = "Calculating...",
    val avgHrv: Int = 0,
    val hrvTrend: String = "0%",
    val totalStrain: Float = 0f,
    val strainTrend: String = "0%",
    val isLoading: Boolean = true
)

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao
) : ViewModel() {

    val uiState: StateFlow<WeeklyReportUiState> = dailyMetricsDao.observeRecent(7)
        .map { metrics ->
            if (metrics.isEmpty()) {
                WeeklyReportUiState(isLoading = false, headline = "No data for this week.")
            } else {
                val chronological = metrics.sortedBy { it.date }

                val avgHrv = chronological.mapNotNull { it.hrvMorning }.average().toInt()
                val totalStrain = chronological.sumOf { it.strainScore.toDouble() }.toFloat()

                val splitIndex = (chronological.size / 2).coerceAtLeast(1)
                val olderWindow = chronological.take(splitIndex)
                val recentWindow = chronological.drop(splitIndex).ifEmpty { chronological.takeLast(1) }

                val olderHrv = olderWindow.mapNotNull { it.hrvMorning }.average().takeIf { !it.isNaN() }
                val recentHrv = recentWindow.mapNotNull { it.hrvMorning }.average().takeIf { !it.isNaN() }

                val olderStrain = olderWindow.map { it.strainScore.toFloat() }.average().toFloat()
                val recentStrain = recentWindow.map { it.strainScore.toFloat() }.average().toFloat()

                val hrvTrend = formatTrendPercentage(olderHrv, recentHrv)
                val strainTrend = formatTrendPercentage(olderStrain.toDouble(), recentStrain.toDouble())

                val avgRecovery = chronological.map { it.recoveryScore.toDouble() }.average().toFloat()
                val headline = when {
                    avgRecovery >= 75f -> "Strong recovery baseline this week."
                    avgRecovery >= 55f -> "Stable baseline with room to improve sleep and load balance."
                    else -> "Recovery load is elevated. Prioritize rest and lower strain blocks."
                }

                val start = LocalDate.parse(chronological.first().date).format(DateTimeFormatter.ofPattern("MMM dd"))
                val end = LocalDate.parse(chronological.last().date).format(DateTimeFormatter.ofPattern("MMM dd"))

                WeeklyReportUiState(
                    dateRange = "$start - $end",
                    headline = headline,
                    avgHrv = avgHrv,
                    hrvTrend = hrvTrend,
                    totalStrain = totalStrain,
                    strainTrend = strainTrend,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeeklyReportUiState()
        )

    private fun formatTrendPercentage(oldValue: Double?, newValue: Double?): String {
        if (oldValue == null || newValue == null || oldValue.absoluteValue < 0.001) {
            return "Insufficient trend data"
        }
        val deltaPct = ((newValue - oldValue) / oldValue) * 100.0
        val rounded = deltaPct.toInt()
        val sign = if (rounded >= 0) "+" else ""
        return "$sign$rounded% vs prior window"
    }
}
