package com.aira.health.ai.runtime

import kotlinx.coroutines.flow.Flow

/**
 * App-facing contract for AI inference.
 *
 * **Thread safety:** All suspend functions MUST be called from a background dispatcher.
 * Never invoke from the main thread — see PERF-01.
 *
 * **Privacy (AIM-03):** Implementations may be local or network-backed, but raw biometric
 * records must never appear in prompt payloads. Only aggregated derived features
 * (scores, confidence, recency) are permitted.
 *
 * The current production binding [GeminiCloudRuntimeGateway] transmits aggregated prompt
 * content to a remote Gemini endpoint over HTTPS.
 */
interface AiRuntimeGateway {

    /**
     * Declares whether this gateway runs locally on-device or relies on remote transport.
     *
     * Default is network-backed to fail closed for coaching privacy policy checks.
     */
    val executionMode: AiRuntimeExecutionMode
        get() = AiRuntimeExecutionMode.NETWORK_BACKED

    /**
     * Execute a generation request and emit partial/final response tokens as a [Flow].
     *
     * The returned flow emits [AiRuntimeResponse] values as the model streams output.
     * The final emission will have [AiRuntimeResponse.isDone] = true.
     *
     * On failure the flow terminates with [AiRuntimeException] carrying the [RuntimeFailureReason].
     *
     * Cancelling the collecting coroutine will cancel the in-flight generation and emit
     * [RuntimeFailureReason.CANCELLED] as the terminal reason.
     */
    fun generate(request: AiRuntimeRequest): Flow<AiRuntimeResponse>

    /**
     * Release all engine resources. Call when the gateway is no longer needed
     * (e.g., in ViewModel.onCleared or app process teardown).
     */
    suspend fun close()
}

/**
 * Runtime execution capability used by policy gates for privacy-bound coaching paths.
 */
enum class AiRuntimeExecutionMode {
    LOCAL_ONLY,
    NETWORK_BACKED,
}

// ---------------------------------------------------------------------------
// Request
// ---------------------------------------------------------------------------

/**
 * Encapsulates all inputs needed for one generation round-trip.
 *
 * @param promptChunks  Ordered list of prompt segments (system + user context).
 *                      Each chunk must contain only aggregated health features — no raw records.
 * @param timeoutMillis Maximum milliseconds allowed before [RuntimeFailureReason.TIMEOUT] is raised.
 *                      Defaults to [RuntimeConfig.DEFAULT_TIMEOUT_MS].
 * @param requestId     Opaque identifier used for telemetry correlation. Never contains PII.
 */
data class AiRuntimeRequest(
    val promptChunks: List<String>,
    val timeoutMillis: Long = RuntimeConfig.DEFAULT_TIMEOUT_MS,
    val requestId: String = generateRequestId(),
)

// ---------------------------------------------------------------------------
// Response
// ---------------------------------------------------------------------------

/**
 * A single streamed token or the final response from the runtime.
 *
 * @param text    Partial or final text from the model for this emission.
 * @param isDone  True only on the last emission of a successful generation.
 * @param latencyMs  Wall-clock milliseconds since request start (populated on final emission).
 */
data class AiRuntimeResponse(
    val text: String,
    val isDone: Boolean = false,
    /** Monotonic elapsed milliseconds since request start. */
    val latencyMs: Long? = null,
)

// ---------------------------------------------------------------------------
// Failure
// ---------------------------------------------------------------------------

/**
 * Structured failure reason covers all terminal error conditions.
 * Always return one of these — never expose raw runtime exceptions to callers.
 */
enum class RuntimeFailureReason {
    /** Model artifacts not found, failed integrity check, or engine init failed. */
    MODEL_UNAVAILABLE,
    /** Generation did not complete within [AiRuntimeRequest.timeoutMillis]. */
    TIMEOUT,
    /** Caller cancelled the collecting coroutine before completion. */
    CANCELLED,
    /** API rate limit exceeded (e.g. HTTP 429). */
    RATE_LIMITED,
    /** Unexpected native or runtime error. Check logs for diagnostic reason code. */
    INTERNAL_ERROR,
}

/** Thrown by [AiRuntimeGateway.generate] flow on non-cancellation failures. */
class AiRuntimeException(
    val reason: RuntimeFailureReason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun generateRequestId(): String =
    "req-${System.currentTimeMillis()}-${(1000..9999).random()}"
