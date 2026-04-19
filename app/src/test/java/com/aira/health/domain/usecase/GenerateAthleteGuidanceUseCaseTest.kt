package com.aira.health.domain.usecase

import com.aira.health.ai.fallback.DeterministicGuidanceService
import com.aira.health.ai.prompt.AthleteGuidancePromptAssembler
import com.aira.health.ai.runtime.AiRuntimeExecutionMode
import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.AiRuntimePolicy
import com.aira.health.ai.runtime.AiRuntimePolicyGuard
import com.aira.health.ai.runtime.AiRuntimeRequest
import com.aira.health.ai.runtime.AiRuntimeResponse
import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionProjection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateAthleteGuidanceUseCaseTest {

    @Test
    fun `runtime failure, policy block, and low confidence all return deterministic output`() = runTest {
        val policyBlockedUseCase = useCase(
            gateway = object : AiRuntimeGateway {
                override val executionMode: AiRuntimeExecutionMode = AiRuntimeExecutionMode.NETWORK_BACKED
                override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> =
                    flowOf(AiRuntimeResponse(text = "SUMMARY: should never be used", isDone = true))
                override suspend fun close() = Unit
            },
            requiredMode = AiRuntimeExecutionMode.LOCAL_ONLY,
        )

        val runtimeFailureUseCase = useCase(
            gateway = object : AiRuntimeGateway {
                override val executionMode: AiRuntimeExecutionMode = AiRuntimeExecutionMode.LOCAL_ONLY
                override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> = flow {
                    error("boom")
                }
                override suspend fun close() = Unit
            },
            requiredMode = AiRuntimeExecutionMode.LOCAL_ONLY,
        )

        val localLowConfidenceUseCase = useCase(
            gateway = successfulLocalGateway(),
            requiredMode = AiRuntimeExecutionMode.LOCAL_ONLY,
        )

        val policyBlocked = policyBlockedUseCase.generate(sampleRequest(confidence = 0.8f))
        val runtimeFailure = runtimeFailureUseCase.generate(sampleRequest(confidence = 0.8f))
        val lowConfidence = localLowConfidenceUseCase.generate(sampleRequest(confidence = 0.2f))

        assertTrue(policyBlocked.usedDeterministicFallback)
        assertTrue(runtimeFailure.usedDeterministicFallback)
        assertTrue(lowConfidence.usedDeterministicFallback)
    }

    @Test
    fun `guidance output never leaks raw payload markers or diagnosis language`() = runTest {
        val output = useCase(
            gateway = successfulLocalGateway(),
            requiredMode = AiRuntimeExecutionMode.LOCAL_ONLY,
        ).generate(sampleRequest(confidence = 0.85f))

        val fullText = listOf(
            output.summary,
            output.actions.training,
            output.actions.recovery,
            output.actions.nutrition,
            output.uncertaintyNote.orEmpty(),
        ).joinToString(" ").lowercase()

        listOf("healthrecordraw", "hrsample", "hrvsample", "sleepsession").forEach { marker ->
            assertFalse("Output leaked raw marker: $marker", fullText.contains(marker))
        }

        listOf("diagnos", "prescri", "medical treatment", "disease", "symptom").forEach { term ->
            assertFalse("Output contained prohibited diagnosis language: $term", fullText.contains(term))
        }
    }

    @Test
    fun `primary and fallback paths both keep summary and action sections`() = runTest {
        val primary = useCase(
            gateway = successfulLocalGateway(),
            requiredMode = AiRuntimeExecutionMode.LOCAL_ONLY,
        ).generate(sampleRequest(confidence = 0.8f))

        val fallback = useCase(
            gateway = object : AiRuntimeGateway {
                override val executionMode: AiRuntimeExecutionMode = AiRuntimeExecutionMode.LOCAL_ONLY
                override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> =
                    flowOf(AiRuntimeResponse(text = "invalid output", isDone = true))
                override suspend fun close() = Unit
            },
            requiredMode = AiRuntimeExecutionMode.LOCAL_ONLY,
        ).generate(sampleRequest(confidence = 0.8f))

        assertFalse(primary.usedDeterministicFallback)
        assertTrue(fallback.usedDeterministicFallback)

        listOf(primary, fallback).forEach { output ->
            assertTrue(output.summary.isNotBlank())
            assertTrue(output.actions.training.isNotBlank())
            assertTrue(output.actions.recovery.isNotBlank())
            assertTrue(output.actions.nutrition.isNotBlank())
            assertNotNull(output.citations)
        }
    }

    private fun useCase(
        gateway: AiRuntimeGateway,
        requiredMode: AiRuntimeExecutionMode,
    ): GenerateAthleteGuidanceUseCase {
        return GenerateAthleteGuidanceUseCase(
            aiRuntimeGateway = gateway,
            runtimePolicyGuard = AiRuntimePolicyGuard(
                policy = AiRuntimePolicy(coachingGenerationMode = requiredMode)
            ),
            promptAssembler = AthleteGuidancePromptAssembler(),
            deterministicGuidanceService = DeterministicGuidanceService(
                generateDailyAthleteSummaryUseCase = GenerateDailyAthleteSummaryUseCase(),
                generateActionGuidanceUseCase = GenerateActionGuidanceUseCase(),
            ),
        )
    }

    private fun successfulLocalGateway(): AiRuntimeGateway = object : AiRuntimeGateway {
        override val executionMode: AiRuntimeExecutionMode = AiRuntimeExecutionMode.LOCAL_ONLY

        override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> = flowOf(
            AiRuntimeResponse(text = "SUMMARY: Readiness is balanced today."),
            AiRuntimeResponse(text = "TRAINING: Use a moderate session and avoid sudden load spikes."),
            AiRuntimeResponse(text = "RECOVERY: Keep sleep routine stable and add light mobility tonight."),
            AiRuntimeResponse(text = "NUTRITION: Keep carbs aligned with session demand and hydrate steadily.", isDone = true),
        )

        override suspend fun close() = Unit
    }

    private fun sampleRequest(confidence: Float): AthleteGuidanceRequest = AthleteGuidanceRequest(
        date = "2026-04-18",
        recoveryScore = 70,
        sleepScore = 66,
        strainScore = 57,
        stressScore = 41,
        energyBankScore = 60,
        dataConfidence = confidence,
        predictionProjection = PredictionProjection(
            projectedRecoveryDelta = 2,
            projectedEnergyDelta = 1,
            confidenceTier = PredictionConfidenceTier.MEDIUM,
            confidenceScore = 0.64f,
            rationaleSignalKeys = listOf("recovery_score", "strain_score"),
        ),
        rationaleSignalKeys = listOf("recovery_score", "strain_score", "sleep_score"),
    )
}
