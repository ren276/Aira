package com.aira.health.presentation.dashboard.coach

import com.aira.health.domain.model.PredictionConfidenceTier

data class ScenarioInput(
    val targetDate: String,
    val sleepDeltaHours: Float,
    val trainingLoadDeltaPercent: Float,
)

data class ProjectionCardModel(
    val projectedRecoveryDelta: Int,
    val projectedEnergyDelta: Int,
    val projectedBurnoutTier: String,
    val projectedBurnoutTrajectory: String,
    val confidenceTier: PredictionConfidenceTier,
    val confidenceScore: Float,
    val uncertaintyLabel: String?,
)

data class GuidanceCardModel(
    val summary: String,
    val actions: List<String>,
    val citations: List<String>,
    val uncertaintyLabel: String?,
)

data class WeeklyDraftCardModel(
    val targetDate: String,
    val loadRecoveryBalanceSummary: String,
    val weeklyFocus: String,
    val priorityActions: List<String>,
    val cautionNotes: List<String>,
    val confidenceTier: PredictionConfidenceTier,
    val confidenceScore: Float,
    val uncertaintyLabel: String?,
)

sealed interface CoachUiState {
    data object Loading : CoachUiState

    data class Ready(
        val scenario: ScenarioInput,
        val projection: ProjectionCardModel,
        val guidance: GuidanceCardModel,
        val weeklyDraft: WeeklyDraftCardModel,
        val isRefreshing: Boolean = false,
    ) : CoachUiState

    data class Error(
        val message: String,
        val scenario: ScenarioInput,
    ) : CoachUiState
}
