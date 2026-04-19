package com.aira.health.domain.model

/**
 * Athlete guidance contract returned to presentation surfaces.
 */
data class AthleteGuidanceOutput(
    val summary: String,
    val actions: ActionGuidance,
    val confidenceTier: PredictionConfidenceTier,
    val confidenceScore: Float,
    val citations: List<String>,
    val uncertaintyNote: String?,
    val usedDeterministicFallback: Boolean,
) {
    data class ActionGuidance(
        val training: String,
        val recovery: String,
        val nutrition: String,
    )
}
