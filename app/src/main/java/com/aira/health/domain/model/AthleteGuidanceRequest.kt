package com.aira.health.domain.model

/**
 * Aggregate-only request payload for athlete guidance generation.
 *
 * Raw biometric event rows are intentionally excluded to preserve privacy boundaries.
 */
data class AthleteGuidanceRequest(
    val date: String,
    val recoveryScore: Int,
    val sleepScore: Int,
    val strainScore: Int,
    val stressScore: Int,
    val energyBankScore: Int,
    val dataConfidence: Float,
    val predictionProjection: PredictionProjection? = null,
    val burnoutProjection: BurnoutRiskProjection? = null,
    val rationaleSignalKeys: List<String> = emptyList(),
    val athleteNotes: String? = null,
) {

    fun validate() {
        require(date.isNotBlank()) { "date must not be blank" }
        require(recoveryScore in SCORE_RANGE) { "recoveryScore must be 0..100" }
        require(sleepScore in SCORE_RANGE) { "sleepScore must be 0..100" }
        require(strainScore in SCORE_RANGE) { "strainScore must be 0..100" }
        require(stressScore in SCORE_RANGE) { "stressScore must be 0..100" }
        require(energyBankScore in SCORE_RANGE) { "energyBankScore must be 0..100" }
        require(dataConfidence in CONFIDENCE_RANGE) { "dataConfidence must be 0.0..1.0" }
    }

    fun confidenceTier(): PredictionConfidenceTier = when {
        dataConfidence >= 0.75f -> PredictionConfidenceTier.HIGH
        dataConfidence >= 0.45f -> PredictionConfidenceTier.MEDIUM
        else -> PredictionConfidenceTier.LOW
    }

    companion object {
        val SCORE_RANGE: IntRange = 0..100
        val CONFIDENCE_RANGE: ClosedFloatingPointRange<Float> = 0f..1f
    }
}
