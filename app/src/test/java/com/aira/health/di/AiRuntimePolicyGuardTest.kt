package com.aira.health.di

import com.aira.health.ai.fallback.DeterministicGuidanceService
import com.aira.health.ai.prompt.AthleteGuidancePromptAssembler
import com.aira.health.ai.runtime.AiRuntimeExecutionMode
import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.AiRuntimePolicyGuard
import com.aira.health.ai.runtime.AiRuntimePolicy
import com.aira.health.ai.runtime.AiRuntimeRequest
import com.aira.health.ai.runtime.AiRuntimeResponse
import com.aira.health.ai.runtime.GeminiCloudRuntimeGateway
import com.aira.health.domain.model.AthleteGuidanceRequest
import com.aira.health.domain.model.PredictionProjection
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.usecase.GenerateActionGuidanceUseCase
import com.aira.health.domain.usecase.GenerateAthleteGuidanceUseCase
import com.aira.health.domain.usecase.GenerateDailyAthleteSummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRuntimePolicyGuardTest {

    @Test
    fun `coaching path enforces local-only runtime and rejects network-backed gateway`() {
        val guard = AiRuntimePolicyGuard(
            policy = AiRuntimePolicy(coachingGenerationMode = AiRuntimeExecutionMode.LOCAL_ONLY)
        )

        val decision = guard.requireLocalOnly(FakeNetworkGateway())

        assertFalse(decision.allowed)
        assertEquals(AiRuntimeExecutionMode.LOCAL_ONLY, decision.requiredMode)
        assertEquals(AiRuntimeExecutionMode.NETWORK_BACKED, decision.actualMode)
    }

    @Test
    fun `when local runtime unavailable guidance uses deterministic fallback and never calls cloud runtime`() = runTest {
        val gateway = CountingNetworkGateway()
        val useCase = GenerateAthleteGuidanceUseCase(
            aiRuntimeGateway = gateway,
            runtimePolicyGuard = AiRuntimePolicyGuard(
                policy = AiRuntimePolicy(coachingGenerationMode = AiRuntimeExecutionMode.LOCAL_ONLY)
            ),
            promptAssembler = AthleteGuidancePromptAssembler(),
            deterministicGuidanceService = DeterministicGuidanceService(
                generateDailyAthleteSummaryUseCase = GenerateDailyAthleteSummaryUseCase(),
                generateActionGuidanceUseCase = GenerateActionGuidanceUseCase(),
            ),
        )

        val output = useCase.generate(sampleRequest())

        assertTrue(output.usedDeterministicFallback)
        assertTrue(output.uncertaintyNote.orEmpty().contains("privacy", ignoreCase = true))
        assertEquals(0, gateway.generateCalls)
    }

    @Test
    fun `module cloud binding remains blocked by coaching local-only policy`() {
        val policy = AiRuntimeModule.provideAiRuntimePolicy()
        val guard = AiRuntimePolicyGuard(policy)

        val bindingMethod = AiRuntimeModule::class.java.declaredMethods
            .first { it.name == "bindAiRuntimeGateway" }
        assertEquals(GeminiCloudRuntimeGateway::class.java, bindingMethod.parameterTypes.first())

        val decision = guard.requireLocalOnly(FakeNetworkGateway())
        assertFalse(decision.allowed)
    }

    private fun sampleRequest(): AthleteGuidanceRequest = AthleteGuidanceRequest(
        date = "2026-04-18",
        recoveryScore = 70,
        sleepScore = 66,
        strainScore = 54,
        stressScore = 44,
        energyBankScore = 62,
        dataConfidence = 0.8f,
        predictionProjection = PredictionProjection(
            projectedRecoveryDelta = 2,
            projectedEnergyDelta = 1,
            confidenceTier = PredictionConfidenceTier.MEDIUM,
            confidenceScore = 0.62f,
            rationaleSignalKeys = listOf("recovery_score", "strain_score"),
        ),
        rationaleSignalKeys = listOf("recovery_score", "strain_score", "sleep_score"),
    )

    private class FakeNetworkGateway : AiRuntimeGateway {
        override val executionMode: AiRuntimeExecutionMode = AiRuntimeExecutionMode.NETWORK_BACKED

        override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> =
            flowOf(AiRuntimeResponse(text = ""))

        override suspend fun close() = Unit
    }

    private class CountingNetworkGateway : AiRuntimeGateway {
        override val executionMode: AiRuntimeExecutionMode = AiRuntimeExecutionMode.NETWORK_BACKED
        var generateCalls: Int = 0

        override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> {
            generateCalls += 1
            return flowOf(AiRuntimeResponse(text = "SUMMARY: placeholder", isDone = true))
        }

        override suspend fun close() = Unit
    }
}
