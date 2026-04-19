package com.aira.health.presentation.dashboard.details

/**
 * Shared UI state contract for all metric detail screens.
 */
sealed interface MetricDetailUiState {
    data object Loading : MetricDetailUiState

    data class Error(val message: String) : MetricDetailUiState

    enum class FactorDirection {
        INCREASED,
        DECREASED,
        NEUTRAL
    }

    data class RankedFactor(
        val rank: Int,
        val name: String,
        val direction: FactorDirection,
        val weight: Float,
        val windowTag: String
    )

    data class Success(
        val metricType: MetricType,
        val currentScore: Int,
        val trendDataPoints: List<Float>,  // e.g., last 14 days
        val confidence: Float,             // Preserved from Room/DailyMetrics (T-04-08)
        val confidenceTierLabel: String = "Low",
        val recencyWindowText: String = "last 3d",
        val rankedFactors: List<RankedFactor> = emptyList(),
        
        // D-11: 3-part Explanation Sheet Contract fields
        val whatChanged: String,
        val whyItMatters: String,
        val whatToDoNext: String,

        // Explainability contract: source provenance + concrete values considered for this score.
        val dataSources: List<String> = emptyList(),
        val consideredData: List<String> = emptyList()
    ) : MetricDetailUiState
}
