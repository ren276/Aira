package com.aira.health.domain.model

enum class BurnoutRiskTier {
    LOW,
    MODERATE,
    HIGH
}

enum class BurnoutTrajectory {
    RISING,
    STABLE,
    FALLING
}

data class BurnoutRiskProjection(
    val tier: BurnoutRiskTier,
    val trajectory: BurnoutTrajectory,
    val confidenceTier: PredictionConfidenceTier,
    val confidenceScore: Float,
    val rationaleSignalKeys: List<String>
)
