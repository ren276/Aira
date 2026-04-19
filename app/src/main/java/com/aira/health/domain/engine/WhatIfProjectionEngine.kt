package com.aira.health.domain.engine

import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionProjection
import com.aira.health.domain.model.PredictionScenario
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

class WhatIfProjectionEngine @Inject constructor() {

    fun project(
        scenario: PredictionScenario,
        baselineRecovery: Int,
        baselineEnergy: Int,
        dataConfidence: Float,
        recoverySpeed: Float,
        rationaleSignalKeys: List<String>
    ): PredictionProjection {
        scenario.validate()

        val speed = recoverySpeed.coerceIn(0.7f, 1.3f)

        val sleepImpact = scenario.sleepDeltaHours * 7f
        val trainingImpact = scenario.trainingLoadDeltaPercent * 0.20f

        val boundedRecoveryDelta = ((sleepImpact - trainingImpact) * speed)
            .roundToInt()
            .coerceIn(-20, 20)

        val boundedEnergyDelta = ((boundedRecoveryDelta * 0.65f) - (scenario.trainingLoadDeltaPercent * 0.08f))
            .roundToInt()
            .coerceIn(-18, 18)

        val confidenceScore = computeConfidenceScore(
            baseConfidence = dataConfidence,
            sleepDeltaHours = scenario.sleepDeltaHours,
            trainingLoadDeltaPercent = scenario.trainingLoadDeltaPercent
        )

        return PredictionProjection(
            projectedRecoveryDelta = boundedRecoveryDelta,
            projectedEnergyDelta = boundedEnergyDelta,
            confidenceTier = toTier(confidenceScore),
            confidenceScore = confidenceScore,
            rationaleSignalKeys = rationaleSignalKeys
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(3)
                .toList()
        )
    }

    private fun computeConfidenceScore(
        baseConfidence: Float,
        sleepDeltaHours: Float,
        trainingLoadDeltaPercent: Float
    ): Float {
        val normalizedSleepMagnitude = abs(sleepDeltaHours) / PredictionScenario.MAX_SLEEP_DELTA_HOURS
        val normalizedTrainingMagnitude =
            abs(trainingLoadDeltaPercent) / PredictionScenario.MAX_TRAINING_LOAD_DELTA_PERCENT
        val magnitudePenalty = ((normalizedSleepMagnitude + normalizedTrainingMagnitude) / 2f) * 0.20f

        return (baseConfidence.coerceIn(0f, 1f) - magnitudePenalty).coerceIn(0.05f, 1f)
    }

    private fun toTier(confidence: Float): PredictionConfidenceTier = when {
        confidence >= 0.75f -> PredictionConfidenceTier.HIGH
        confidence >= 0.45f -> PredictionConfidenceTier.MEDIUM
        else -> PredictionConfidenceTier.LOW
    }
}
