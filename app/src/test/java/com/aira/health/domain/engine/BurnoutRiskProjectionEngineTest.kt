package com.aira.health.domain.engine

import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.model.BurnoutRiskTier
import com.aira.health.domain.model.BurnoutTrajectory
import com.aira.health.domain.model.PredictionConfidenceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BurnoutRiskProjectionEngineTest {

    private val engine = BurnoutRiskProjectionEngine()

    @Test
    fun `burnout projection returns tier and trajectory for short horizon trend`() {
        val history = listOf(
            daily(date = "2026-04-11", strain = 55, stress = 52, energy = 65),
            daily(date = "2026-04-12", strain = 60, stress = 56, energy = 62),
            daily(date = "2026-04-13", strain = 66, stress = 61, energy = 58),
            daily(date = "2026-04-14", strain = 71, stress = 64, energy = 53),
            daily(date = "2026-04-15", strain = 76, stress = 68, energy = 49),
            daily(date = "2026-04-16", strain = 81, stress = 73, energy = 44),
            daily(date = "2026-04-17", strain = 86, stress = 79, energy = 40),
            daily(date = "2026-04-18", strain = 90, stress = 84, energy = 36)
        )

        val projection = engine.projectRisk(history)

        assertEquals(BurnoutRiskTier.HIGH, projection.tier)
        assertEquals(BurnoutTrajectory.RISING, projection.trajectory)
        assertTrue(projection.rationaleSignalKeys.contains("strain_score"))
    }

    @Test
    fun `sparse history downgrades confidence to low`() {
        val sparseHistory = listOf(
            daily(date = "2026-04-16", strain = 62, stress = 58, energy = 57),
            daily(date = "2026-04-17", strain = 64, stress = 61, energy = 55),
            daily(date = "2026-04-18", strain = 66, stress = 63, energy = 52)
        )

        val projection = engine.projectRisk(sparseHistory)

        assertEquals(PredictionConfidenceTier.LOW, projection.confidenceTier)
        assertTrue(projection.tier in listOf(BurnoutRiskTier.LOW, BurnoutRiskTier.MODERATE, BurnoutRiskTier.HIGH))
    }

    @Test
    fun `flat load trend reports stable trajectory`() {
        val history = listOf(
            daily(date = "2026-04-11", strain = 58, stress = 52, energy = 60),
            daily(date = "2026-04-12", strain = 57, stress = 53, energy = 61),
            daily(date = "2026-04-13", strain = 59, stress = 52, energy = 60),
            daily(date = "2026-04-14", strain = 58, stress = 53, energy = 59),
            daily(date = "2026-04-15", strain = 57, stress = 54, energy = 60),
            daily(date = "2026-04-16", strain = 58, stress = 53, energy = 60),
            daily(date = "2026-04-17", strain = 59, stress = 52, energy = 59)
        )

        val projection = engine.projectRisk(history)

        assertEquals(BurnoutTrajectory.STABLE, projection.trajectory)
    }

    private fun daily(
        date: String,
        strain: Int,
        stress: Int,
        energy: Int,
        confidence: Float = 0.9f
    ): DailyMetrics = DailyMetrics(
        date = date,
        strainScore = strain,
        stressScore = stress,
        energyBankScore = energy,
        dataConfidence = confidence
    )
}
