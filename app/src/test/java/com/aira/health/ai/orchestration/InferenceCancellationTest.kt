package com.aira.health.ai.orchestration

import com.aira.health.ai.fallback.DeterministicSummaryService
import com.aira.health.ai.fallback.FallbackReason
import com.aira.health.ai.prompt.MetricSnapshot
import com.aira.health.ai.prompt.PromptAssembler
import com.aira.health.ai.runtime.AiRuntimeException
import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.AiRuntimeRequest
import com.aira.health.ai.runtime.AiRuntimeResponse
import com.aira.health.ai.runtime.RuntimeConfig
import com.aira.health.ai.runtime.RuntimeFailureReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * PERF-01 — Verifies [InferenceOrchestrator] maps all runtime failure paths to
 * controlled [InferenceOutcome.Fallback] values; never propagates raw exceptions to caller.
 *
 * Deliberately uses only [kotlinx.coroutines.test] (JVM-17-compatible) and hand-written
 * Kotlin fake gateways. Turbine and MockK both require JVM 21+ and cannot be used here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InferenceCancellationTest {

    private val promptAssembler = PromptAssembler()
    private val fallbackService = DeterministicSummaryService()
    private val config = RuntimeConfig(timeoutMillis = 2_500)

    private val sampleSnapshot = MetricSnapshot(
        date = "2026-04-17",
        recoveryScore = 70,
        sleepScore = 65,
        strainScore = 50,
        stressScore = 38,
        dataConfidence = 0.80f,
        compositeReadiness = 68,
    )

    // ---------------------------------------------------------------------------
    // Fake gateway builders (no MockK / Turbine)
    // ---------------------------------------------------------------------------

    private fun failingGateway(reason: RuntimeFailureReason): AiRuntimeGateway =
        object : AiRuntimeGateway {
            override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> = flow {
                throw AiRuntimeException(reason, "fake: $reason")
            }
            override suspend fun close() = Unit
        }

    private fun successfulGateway(): AiRuntimeGateway =
        object : AiRuntimeGateway {
            override fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse> = flowOf(
                AiRuntimeResponse(text = "Your recovery", isDone = false),
                AiRuntimeResponse(text = " looks good.", isDone = true, latencyMs = 800),
            )
            override suspend fun close() = Unit
        }

    private fun orchestratorWith(gateway: AiRuntimeGateway) =
        InferenceOrchestrator(gateway, promptAssembler, fallbackService, config)

    // Helper: collect all outcomes to a list (flow always terminates for InferenceOrchestrator)
    private suspend fun collectOutcomes(gateway: AiRuntimeGateway): List<InferenceOutcome> =
        orchestratorWith(gateway).run(sampleSnapshot).toList()

    // ---------------------------------------------------------------------------
    // Successful generation path
    // ---------------------------------------------------------------------------

    @Test
    fun `successful generation emits Partial then Complete`() = runTest {
        val outcomes = collectOutcomes(successfulGateway())
        assertTrue(outcomes.isNotEmpty(), "Expected at least one outcome")
        val partial = outcomes.first()
        assertTrue(partial is InferenceOutcome.Partial, "First outcome must be Partial, got $partial")
        assertEquals("Your recovery", (partial as InferenceOutcome.Partial).text)

        val complete = outcomes.last()
        assertTrue(complete is InferenceOutcome.Complete, "Last outcome must be Complete, got $complete")
        assertFalse((complete as InferenceOutcome.Complete).usedFallback)
    }

    // ---------------------------------------------------------------------------
    // PERF-01 + AIM-04: Each failure reason → Fallback (not raw exception)
    // ---------------------------------------------------------------------------

    @Test
    fun `TIMEOUT produces Fallback with TIMEOUT reason`() = runTest {
        val outcomes = collectOutcomes(failingGateway(RuntimeFailureReason.TIMEOUT))
        val fallback = singleFallback(outcomes)
        assertEquals(FallbackReason.TIMEOUT, fallback.summary.reason)
    }

    @Test
    fun `CANCELLED produces Fallback with CANCELLED reason`() = runTest {
        val outcomes = collectOutcomes(failingGateway(RuntimeFailureReason.CANCELLED))
        val fallback = singleFallback(outcomes)
        assertEquals(FallbackReason.CANCELLED, fallback.summary.reason)
    }

    @Test
    fun `MODEL_UNAVAILABLE produces Fallback with MODEL_UNAVAILABLE reason`() = runTest {
        val outcomes = collectOutcomes(failingGateway(RuntimeFailureReason.MODEL_UNAVAILABLE))
        val fallback = singleFallback(outcomes)
        assertEquals(FallbackReason.MODEL_UNAVAILABLE, fallback.summary.reason)
    }

    @Test
    fun `INTERNAL_ERROR produces Fallback with RUNTIME_ERROR reason`() = runTest {
        val outcomes = collectOutcomes(failingGateway(RuntimeFailureReason.INTERNAL_ERROR))
        val fallback = singleFallback(outcomes)
        assertEquals(FallbackReason.RUNTIME_ERROR, fallback.summary.reason)
    }

    // ---------------------------------------------------------------------------
    // PERF-01: Fallback text — non-blank, wellness-safe, no raw exceptions forwarded
    // ---------------------------------------------------------------------------

    @Test
    fun `all runtime failure reasons produce non-blank wellness-safe fallback text`() = runTest {
        // Medical / diagnostic terms must never appear in generated fallback text
        val blockedTerms = listOf("diagnos", "prescri", "treatment", "medic", "symptom")

        for (runtimeReason in RuntimeFailureReason.entries) {
            val outcomes = collectOutcomes(failingGateway(runtimeReason))
            val fallback = singleFallback(outcomes)

            assertTrue(fallback.summary.text.isNotBlank(), "Blank fallback text for $runtimeReason")
            blockedTerms.forEach { term ->
                assertFalse(
                    fallback.summary.text.contains(term, ignoreCase = true),
                    "Blocked term '$term' in fallback for $runtimeReason: ${fallback.summary.text}",
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // PERF-01: Metadata correctness
    // ---------------------------------------------------------------------------

    @Test
    fun `fallback outcome carries non-negative latencyMs`() = runTest {
        val outcomes = collectOutcomes(failingGateway(RuntimeFailureReason.TIMEOUT))
        val fallback = singleFallback(outcomes)
        assertTrue(fallback.latencyMs >= 0, "latencyMs must be >= 0, got ${fallback.latencyMs}")
    }

    @Test
    fun `fallback summary carries input snapshot recovery score`() = runTest {
        val outcomes = collectOutcomes(failingGateway(RuntimeFailureReason.MODEL_UNAVAILABLE))
        val fallback = singleFallback(outcomes)
        assertEquals(sampleSnapshot.recoveryScore, fallback.summary.recoveryScore)
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun singleFallback(outcomes: List<InferenceOutcome>): InferenceOutcome.Fallback {
        assertEquals(1, outcomes.size, "Expected exactly 1 fallback outcome, got: $outcomes")
        val outcome = outcomes.first()
        assertTrue(outcome is InferenceOutcome.Fallback, "Expected Fallback outcome, got $outcome")
        return outcome as InferenceOutcome.Fallback
    }
}
