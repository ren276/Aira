package com.aira.health.domain.model

/**
 * Card-ready weekly planning payload composed from prediction and guidance outputs.
 */
data class WeeklyAthletePlanDraft(
    val targetDate: String,
    val scenario: PredictionScenario,
    val projectedRecoveryDelta: Int,
    val projectedEnergyDelta: Int,
    val projectedBurnoutTier: BurnoutRiskTier,
    val projectedBurnoutTrajectory: BurnoutTrajectory,
    val confidenceTier: PredictionConfidenceTier,
    val confidenceScore: Float,
    val loadRecoveryBalanceSummary: String,
    val weeklyFocus: String,
    val guidanceSummary: String,
    val priorityActions: List<String>,
    val cautionNotes: List<String>,
    val citations: List<String>,
    val uncertaintyLabel: String?,
)
