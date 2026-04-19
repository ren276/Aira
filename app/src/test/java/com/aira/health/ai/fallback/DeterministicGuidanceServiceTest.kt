package com.aira.health.ai.fallback

import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionProjection
import com.aira.health.domain.usecase.GenerateActionGuidanceUseCase
import com.aira.health.domain.usecase.GenerateDailyAthleteSummaryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicGuidanceServiceTest {

    private val service = DeterministicGuidanceService(
        generateDailyAthleteSummaryUseCase = GenerateDailyAthleteSummaryUseCase(),
        generateActionGuidanceUseCase = GenerateActionGuidanceUseCase(),
    )

    @Test
    fun `same request and fallback reason always produce deterministic output`() {
        val request = sampleRequest(confidence = 0.8f)

        val first = service.build(
            request = request,
            reason = GuidanceFallbackReason.POLICY_BLOCKED,
            citations = listOf("recovery_score", "strain_score"),
        )
        val second = service.build(
            request = request,
            reason = GuidanceFallbackReason.POLICY_BLOCKED,
            citations = listOf("recovery_score", "strain_score"),
        )

        assertEquals(first.summary, second.summary)
        assertEquals(first.actions, second.actions)
        assertEquals(first.citations, second.citations)
        assertTrue(first.usedDeterministicFallback)
    }

    @Test
    fun `fallback output contains no raw payload markers and no diagnosis language`() {
        val output = service.build(
            request = sampleRequest(confidence = 0.3f),
            reason = GuidanceFallbackReason.LOW_CONFIDENCE,
            citations = listOf("recovery_score", "sleep_score"),
        )

        val fullText = listOf(
            output.summary,
            output.actions.training,
            output.actions.recovery,
            output.actions.nutrition,
            output.uncertaintyNote.orEmpty(),
        ).joinToString(" ").lowercase()

        listOf("healthrecordraw", "hrsample", "hrvsample", "sleepsession").forEach { marker ->
            assertFalse("Found disallowed raw payload marker: $marker", fullText.contains(marker))
        }

        listOf("diagnos", "prescri", "medical treatment", "disease", "symptom").forEach { term ->
            assertFalse("Found prohibited diagnosis language: $term", fullText.contains(term))
        }
    }

    @Test
    fun `fallback output keeps stable summary and action structure`() {
        val output = service.build(
            request = sampleRequest(confidence = 0.8f),
            reason = GuidanceFallbackReason.RUNTIME_FAILURE,
            citations = listOf("recovery_score", "energy_bank_score"),
        )

        assertTrue(output.summary.isNotBlank())
        assertTrue(output.actions.training.isNotBlank())
        assertTrue(output.actions.recovery.isNotBlank())
        assertTrue(output.actions.nutrition.isNotBlank())
        assertNotNull(output.citations)
        assertTrue(output.citations.isNotEmpty())
    }

    private fun sampleRequest(confidence: Float): AthleteGuidanceRequest = AthleteGuidanceRequest(
        date = "2026-04-18",
        recoveryScore = 66,
        sleepScore = 64,
        strainScore = 58,
        stressScore = 43,
        energyBankScore = 59,
        dataConfidence = confidence,
        predictionProjection = PredictionProjection(
            projectedRecoveryDelta = 1,
            projectedEnergyDelta = 0,
            confidenceTier = PredictionConfidenceTier.MEDIUM,
            confidenceScore = 0.6f,
            rationaleSignalKeys = listOf("recovery_score", "stress_score"),
        ),
        rationaleSignalKeys = listOf("recovery_score", "sleep_score", "strain_score"),
    )
}
