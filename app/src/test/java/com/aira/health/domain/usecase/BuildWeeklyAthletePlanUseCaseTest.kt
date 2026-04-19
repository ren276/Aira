package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.data.local.model.WhatIfSimulationResult
import com.aira.health.domain.model.AthleteGuidanceOutput
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionScenario
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildWeeklyAthletePlanUseCaseTest {

    private lateinit var dailyMetricsDao: DailyMetricsDao
    private lateinit var runWhatIfSimulationUseCase: RunWhatIfSimulationUseCase
    private lateinit var generateAthleteGuidanceUseCase: GenerateAthleteGuidanceUseCase
    private lateinit var useCase: BuildWeeklyAthletePlanUseCase

    @Before
    fun setUp() {
        dailyMetricsDao = mockk(relaxed = true)
        runWhatIfSimulationUseCase = mockk(relaxed = true)
        generateAthleteGuidanceUseCase = mockk(relaxed = true)

        useCase = BuildWeeklyAthletePlanUseCase(
            dailyMetricsDao = dailyMetricsDao,
            runWhatIfSimulationUseCase = runWhatIfSimulationUseCase,
            generateAthleteGuidanceUseCase = generateAthleteGuidanceUseCase,
        )
    }

    @Test
    fun `weekly draft combines prediction outputs with guidance contract`() = runTest {
        coEvery { dailyMetricsDao.getLast14Days() } returns listOf(sampleMetrics())
        coEvery { runWhatIfSimulationUseCase.runScenario(any()) } returns sampleSimulationResult()

        val requestSlot = slot<com.aira.health.domain.model.AthleteGuidanceRequest>()
        coEvery { generateAthleteGuidanceUseCase.generate(capture(requestSlot)) } returns sampleGuidance()

        val draft = useCase.build(
            PredictionScenario(
                targetDate = "2026-04-22",
                sleepDeltaHours = 1.5f,
                trainingLoadDeltaPercent = 10f,
            )
        )

        val guidanceRequest = requestSlot.captured
        assertEquals(4, guidanceRequest.predictionProjection?.projectedRecoveryDelta)
        assertEquals(-2, guidanceRequest.predictionProjection?.projectedEnergyDelta)
        assertEquals(PredictionConfidenceTier.MEDIUM, guidanceRequest.predictionProjection?.confidenceTier)
        assertNotNull(guidanceRequest.burnoutProjection)

        assertEquals(4, draft.projectedRecoveryDelta)
        assertEquals(-2, draft.projectedEnergyDelta)
        assertEquals("Readiness remains stable for controlled progression.", draft.guidanceSummary)
        assertEquals(3, draft.priorityActions.size)
    }

    @Test
    fun `draft includes load recovery balance summary priority actions and caution notes`() = runTest {
        coEvery { dailyMetricsDao.getLast14Days() } returns listOf(sampleMetrics())
        coEvery { runWhatIfSimulationUseCase.runScenario(any()) } returns sampleSimulationResult()
        coEvery { generateAthleteGuidanceUseCase.generate(any()) } returns sampleGuidance()

        val draft = useCase.build(
            PredictionScenario(
                targetDate = "2026-04-22",
                sleepDeltaHours = 0.5f,
                trainingLoadDeltaPercent = 5f,
            )
        )

        assertTrue(draft.loadRecoveryBalanceSummary.contains("Load shift"))
        assertTrue(draft.loadRecoveryBalanceSummary.contains("projected recovery"))
        assertTrue(draft.priorityActions.any { it.contains("training", ignoreCase = true) })
        assertTrue(draft.cautionNotes.any { it.contains("burnout", ignoreCase = true) })
        assertTrue(draft.weeklyFocus.isNotBlank())
    }

    @Test
    fun `low confidence simulation adds uncertainty qualifiers to draft`() = runTest {
        coEvery { dailyMetricsDao.getLast14Days() } returns listOf(sampleMetrics())
        coEvery { runWhatIfSimulationUseCase.runScenario(any()) } returns sampleSimulationResult(
            confidenceTier = "LOW",
            confidenceScore = 0.2f,
        )
        coEvery { generateAthleteGuidanceUseCase.generate(any()) } returns sampleGuidance(
            uncertaintyNote = null,
            confidenceTier = PredictionConfidenceTier.LOW,
            confidenceScore = 0.2f,
        )

        val draft = useCase.build(
            PredictionScenario(
                targetDate = "2026-04-22",
                sleepDeltaHours = -1f,
                trainingLoadDeltaPercent = 20f,
            )
        )

        assertEquals(PredictionConfidenceTier.LOW, draft.confidenceTier)
        assertNotNull(draft.uncertaintyLabel)
        assertTrue(draft.uncertaintyLabel!!.contains("confidence", ignoreCase = true))
        assertTrue(draft.loadRecoveryBalanceSummary.contains("Confidence is limited", ignoreCase = true))
        assertTrue(draft.cautionNotes.any { it.contains("confidence", ignoreCase = true) })
    }

    private fun sampleMetrics(): DailyMetrics = DailyMetrics(
        date = "2026-04-21",
        recoveryScore = 68,
        sleepScore = 66,
        strainScore = 60,
        stressScore = 40,
        energyBankScore = 55,
        dataConfidence = 0.76f,
    )

    private fun sampleSimulationResult(
        confidenceTier: String = "MEDIUM",
        confidenceScore: Float = 0.63f,
    ): WhatIfSimulationResult = WhatIfSimulationResult(
        id = 11L,
        targetDate = "2026-04-22",
        baselineDate = "2026-04-21",
        baselineRecoveryScore = 68,
        baselineEnergyScore = 55,
        sleepDeltaHours = 1.5f,
        trainingLoadDeltaPercent = 10f,
        projectedRecoveryDelta = 4,
        projectedEnergyDelta = -2,
        projectedBurnoutTier = "MODERATE",
        projectedBurnoutTrajectory = "RISING",
        confidenceTier = confidenceTier,
        confidenceScore = confidenceScore,
        rationaleSignalKeys = "recovery_score|strain_score|sleep_score",
    )

    private fun sampleGuidance(
        uncertaintyNote: String? = "Confidence is moderate; adjust gradually.",
        confidenceTier: PredictionConfidenceTier = PredictionConfidenceTier.MEDIUM,
        confidenceScore: Float = 0.63f,
    ): AthleteGuidanceOutput = AthleteGuidanceOutput(
        summary = "Readiness remains stable for controlled progression.",
        actions = AthleteGuidanceOutput.ActionGuidance(
            training = "Keep training progression conservative this week.",
            recovery = "Prioritize sleep regularity and downshift after hard sessions.",
            nutrition = "Maintain post-session carbohydrate and protein timing.",
        ),
        confidenceTier = confidenceTier,
        confidenceScore = confidenceScore,
        citations = listOf("recovery_score", "strain_score", "sleep_score"),
        uncertaintyNote = uncertaintyNote,
        usedDeterministicFallback = false,
    )
}
