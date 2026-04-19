package com.aira.health.domain.engine

import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.model.BurnoutRiskProjection
import com.aira.health.domain.model.BurnoutRiskTier
import com.aira.health.domain.model.BurnoutTrajectory
import com.aira.health.domain.model.PredictionConfidenceTier
import javax.inject.Inject

class BurnoutRiskProjectionEngine @Inject constructor() {

    fun projectRisk(recentMetrics: List<DailyMetrics>): BurnoutRiskProjection {
        val window = recentMetrics
            .sortedBy { it.date }
            .takeLast(14)
            .takeLast(7)

        if (window.isEmpty()) {
            return BurnoutRiskProjection(
                tier = BurnoutRiskTier.LOW,
                trajectory = BurnoutTrajectory.STABLE,
                confidenceTier = PredictionConfidenceTier.LOW,
                confidenceScore = 0.05f,
                rationaleSignalKeys = DEFAULT_RATIONALE_KEYS
            )
        }

        val loadScores = window.map(::dailyLoadScore)
        val averageLoad = loadScores.average().toFloat().coerceIn(0f, 1f)

        val tier = when {
            averageLoad >= 0.65f -> BurnoutRiskTier.HIGH
            averageLoad >= 0.38f -> BurnoutRiskTier.MODERATE
            else -> BurnoutRiskTier.LOW
        }

        val trajectory = computeTrajectory(loadScores)

        val confidenceScore = computeConfidence(
            sampleSize = window.size,
            averageDataConfidence = window.map { it.dataConfidence.coerceIn(0f, 1f) }.average().toFloat()
        )

        return BurnoutRiskProjection(
            tier = tier,
            trajectory = trajectory,
            confidenceTier = toTier(confidenceScore),
            confidenceScore = confidenceScore,
            rationaleSignalKeys = DEFAULT_RATIONALE_KEYS
        )
    }

    private fun dailyLoadScore(metrics: DailyMetrics): Float {
        val strain = metrics.strainScore.coerceIn(0, 100)
        val stress = metrics.stressScore.coerceIn(0, 100)
        val energyDepletion = (100 - metrics.energyBankScore).coerceIn(0, 100)

        return ((strain * 0.45f) + (stress * 0.35f) + (energyDepletion * 0.20f)) / 100f
    }

    private fun computeTrajectory(loadScores: List<Float>): BurnoutTrajectory {
        if (loadScores.size < 6) return BurnoutTrajectory.STABLE

        val recent = loadScores.takeLast(3).average().toFloat()
        val prior = loadScores.dropLast(3).takeLast(3).average().toFloat()
        val delta = recent - prior

        return when {
            delta >= 0.05f -> BurnoutTrajectory.RISING
            delta <= -0.05f -> BurnoutTrajectory.FALLING
            else -> BurnoutTrajectory.STABLE
        }
    }

    private fun computeConfidence(sampleSize: Int, averageDataConfidence: Float): Float {
        if (sampleSize < 7) return 0.30f

        val sampleWeight = (sampleSize / 14f).coerceIn(0.5f, 1f)
        return (averageDataConfidence * sampleWeight).coerceIn(0.30f, 0.95f)
    }

    private fun toTier(confidence: Float): PredictionConfidenceTier = when {
        confidence >= 0.75f -> PredictionConfidenceTier.HIGH
        confidence >= 0.45f -> PredictionConfidenceTier.MEDIUM
        else -> PredictionConfidenceTier.LOW
    }

    companion object {
        private val DEFAULT_RATIONALE_KEYS = listOf("strain_score", "stress_score", "energy_bank_score")
    }
}
