package com.aira.health.domain.engine

import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatIfProjectionEngineTest {

    private val engine = WhatIfProjectionEngine()

    @Test
    fun `sleep and training deltas project next-day recovery and energy`() {
        val scenario = PredictionScenario(
            targetDate = "2026-04-19",
            sleepDeltaHours = 1.5f,
            trainingLoadDeltaPercent = 10f
        )

        val projection = engine.project(
            scenario = scenario,
            baselineRecovery = 70,
            baselineEnergy = 62,
            dataConfidence = 0.84f,
            recoverySpeed = 1.1f,
            rationaleSignalKeys = listOf("sleep_score", "strain_score", "recovery_score")
        )

        assertTrue("recovery delta should improve", projection.projectedRecoveryDelta > 0)
        assertTrue("energy delta should improve", projection.projectedEnergyDelta > 0)
        assertEquals(PredictionConfidenceTier.HIGH, projection.confidenceTier)
        assertTrue(projection.rationaleSignalKeys.size <= 3)
        assertTrue(projection.rationaleSignalKeys.contains("sleep_score"))
    }

    @Test
    fun `projection output is bounded and confidence tiered`() {
        val scenario = PredictionScenario(
            targetDate = "2026-04-19",
            sleepDeltaHours = -3f,
            trainingLoadDeltaPercent = 40f
        )

        val projection = engine.project(
            scenario = scenario,
            baselineRecovery = 55,
            baselineEnergy = 48,
            dataConfidence = 0.62f,
            recoverySpeed = 1.0f,
            rationaleSignalKeys = listOf("strain_score", "stress_score")
        )

        assertTrue(projection.projectedRecoveryDelta in -20..20)
        assertTrue(projection.projectedEnergyDelta in -18..18)
        assertTrue(projection.confidenceTier in listOf(PredictionConfidenceTier.LOW, PredictionConfidenceTier.MEDIUM, PredictionConfidenceTier.HIGH))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `out-of-range scenario deltas are rejected`() {
        engine.project(
            scenario = PredictionScenario(
                targetDate = "2026-04-19",
                sleepDeltaHours = 4f,
                trainingLoadDeltaPercent = 0f
            ),
            baselineRecovery = 60,
            baselineEnergy = 60,
            dataConfidence = 0.8f,
            recoverySpeed = 1f,
            rationaleSignalKeys = emptyList()
        )
    }
}
