package com.aira.health.ai.orchestration

import android.os.SystemClock
import com.aira.health.ai.fallback.DeterministicSummaryService
import com.aira.health.ai.fallback.FallbackReason
import com.aira.health.ai.fallback.FallbackSummary
import com.aira.health.ai.prompt.MetricSnapshot
import com.aira.health.ai.prompt.PromptAssembler
import com.aira.health.ai.runtime.AiRuntimeException
import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.AiRuntimeRequest
import com.aira.health.ai.runtime.AiRuntimeResponse
import com.aira.health.ai.runtime.RuntimeConfig
import com.aira.health.ai.runtime.RuntimeFailureReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Single app-facing API that orchestrates the full AI inference pipeline:
 * prompt assembly → runtime generation → deterministic fallback on any failure.
 *
 * **Usage from ViewModel:**
 * ```kotlin
 * orchestrator.run(snapshot)
 *     .collect { outcome ->
 *         when (outcome) {
 *             is InferenceOutcome.Partial   -> updateUI(partial = outcome.text)
 *             is InferenceOutcome.Complete  -> updateUI(final = outcome.text, latencyMs = outcome.latencyMs)
 *             is InferenceOutcome.Fallback  -> updateUI(fallback = outcome.summary)
 *         }
 *     }
 * ```
 *
 * **Contracts:**
 * - Off-main-thread: all work runs on [Dispatchers.Default] (PERF-01).
 * - Cancellable: collecting coroutine cancellation propagates correctly through to the runtime.
 * - Deterministic fallback: every failure terminal state returns a [InferenceOutcome.Fallback]
 *   with a reason-coded [FallbackSummary] (AIM-04).
 * - No raw biometrics: [MetricSnapshot] is the only accepted input (AIM-03).
 * - No Compose dependencies: orchestrator is UI-framework agnostic so ViewModels consume it directly.
 */
class InferenceOrchestrator @Inject constructor(
    private val gateway: AiRuntimeGateway,
    private val promptAssembler: PromptAssembler,
    private val fallbackService: DeterministicSummaryService,
    private val config: RuntimeConfig,
) {

    /**
     * Run the full inference pipeline for [snapshot], emitting streaming outcomes.
     *
     * @param snapshot   Aggregated daily metrics. Must not contain raw biometric records.
     * @param userNotes  Optional free-text note from the user (will be redacted).
     */
    fun run(
        snapshot: MetricSnapshot,
        userNotes: String? = null,
    ): Flow<InferenceOutcome> {
        val startMs = SystemClock.elapsedRealtime()

        return flow {

        // 1. Assemble privacy-safe prompt (AIM-03)
        val contract = promptAssembler.assemble(snapshot, userNotes)
        val chunks = promptAssembler.pruneToTokenBudget(
            contract.toChunks(),
            config.maxTokens,
        )

        val request = AiRuntimeRequest(
            promptChunks = chunks,
            timeoutMillis = config.timeoutMillis,
        )

        // 2. Stream runtime generation with timeout wrapper
        withTimeout(config.timeoutMillis + TIMEOUT_GRACE_MS) {
            gateway.generate(request)
                .catch { throwable ->
                    // 3a. Map runtime failures to deterministic fallback (AIM-04)
                    val fallbackReason = when {
                        throwable is TimeoutCancellationException -> FallbackReason.TIMEOUT
                        throwable is AiRuntimeException -> throwable.reason.toFallbackReason()
                        throwable is CancellationException -> FallbackReason.CANCELLED
                        else -> FallbackReason.RUNTIME_ERROR
                    }
                    val summary = fallbackService.buildSummary(snapshot, fallbackReason)
                    emit(
                        InferenceOutcome.Fallback(
                            summary = summary,
                            latencyMs = SystemClock.elapsedRealtime() - startMs,
                        )
                    )
                }
                .collect { response ->
                    if (response.isDone) {
                        // 3b. Final emission with latency metadata
                        emit(
                            InferenceOutcome.Complete(
                                text = response.text,
                                latencyMs = response.latencyMs ?: (SystemClock.elapsedRealtime() - startMs),
                                usedFallback = false,
                            )
                        )
                    } else {
                        // 3c. Partial streaming token
                        emit(InferenceOutcome.Partial(text = response.text))
                    }
                }
        }
    }.catch { throwable ->
        // Top-level catch for timeout or unexpected errors escaping the inner flow
        val reason = when (throwable) {
            is TimeoutCancellationException -> FallbackReason.TIMEOUT
            is CancellationException        -> FallbackReason.CANCELLED
            else                            -> FallbackReason.RUNTIME_ERROR
        }
        val summary = fallbackService.buildSummary(snapshot, reason)
        emit(
            InferenceOutcome.Fallback(
                summary = summary,
                latencyMs = SystemClock.elapsedRealtime() - startMs,
            )
        )
    }.flowOn(Dispatchers.Default)
    }

    companion object {
        /** Grace period above request timeout before the outer coroutine also times out. */
        private const val TIMEOUT_GRACE_MS: Long = 200L
    }
}

// ---------------------------------------------------------------------------
// Sealed outcome type
// ---------------------------------------------------------------------------

/**
 * Terminal and intermediate outcomes from [InferenceOrchestrator.run].
 */
sealed interface InferenceOutcome {

    /** A partial streaming token from the runtime — not yet complete. */
    data class Partial(val text: String) : InferenceOutcome

    /** Generation completed successfully with full text and timing metadata. */
    data class Complete(
        val text: String,
        val latencyMs: Long,
        val usedFallback: Boolean = false,
    ) : InferenceOutcome

    /**
     * AI generation was bypassed; the [summary] was produced deterministically.
     * Always emitted when any failure occurs — never a raw exception to the UI (AIM-04).
     */
    data class Fallback(
        val summary: FallbackSummary,
        val latencyMs: Long,
    ) : InferenceOutcome
}

// ---------------------------------------------------------------------------
// Runtime failure → fallback reason mapping
// ---------------------------------------------------------------------------

private fun RuntimeFailureReason.toFallbackReason(): FallbackReason = when (this) {
    RuntimeFailureReason.MODEL_UNAVAILABLE -> FallbackReason.MODEL_UNAVAILABLE
    RuntimeFailureReason.TIMEOUT          -> FallbackReason.TIMEOUT
    RuntimeFailureReason.CANCELLED        -> FallbackReason.CANCELLED
    RuntimeFailureReason.INTERNAL_ERROR   -> FallbackReason.RUNTIME_ERROR
}

// Type alias for convenience in callers
typealias InferenceRequest = AiRuntimeRequest
typealias InferenceResponse = AiRuntimeResponse
