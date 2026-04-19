package com.aira.health.domain.model

enum class PredictionConfidenceTier {
    LOW,
    MEDIUM,
    HIGH
}

data class PredictionProjection(
    val projectedRecoveryDelta: Int,
    val projectedEnergyDelta: Int,
    val confidenceTier: PredictionConfidenceTier,
    val confidenceScore: Float,
    val rationaleSignalKeys: List<String>
)
