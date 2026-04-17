package com.aira.health.ai.runtime

/**
 * Per-request lifecycle bookkeeping value type.
 *
 * Carries the correlation id, start timestamp, and final outcome once the session
 * completes or fails. Consumers use [latencyMs] for telemetry and [failureReason]
 * to route deterministic fallback output.
 *
 * Instances are immutable value types and are never persisted to disk or
 * transmitted off-device — they live only in memory for the duration of a request.
 */
data class RuntimeSession(
    /** Opaque id matching [AiRuntimeRequest.requestId]. */
    val requestId: String,

    /** Wall-clock time the session was created (epoch millis). */
    val startedAtMs: Long = System.currentTimeMillis(),

    /** Terminal failure reason when the session did not complete successfully. Null on success. */
    val failureReason: RuntimeFailureReason? = null,

    /** Wall-clock duration from [startedAtMs] to completion. Null until session ends. */
    val latencyMs: Long? = null,
) {

    /** True if the session ended without a failure. */
    val isSuccess: Boolean get() = failureReason == null && latencyMs != null

    /**
     * Returns a copy marked as successfully completed with the elapsed wall-clock duration.
     */
    fun complete(): RuntimeSession = copy(
        latencyMs = System.currentTimeMillis() - startedAtMs,
    )

    /**
     * Returns a copy marked as failed with the given [reason] and elapsed duration.
     */
    fun fail(reason: RuntimeFailureReason): RuntimeSession = copy(
        failureReason = reason,
        latencyMs = System.currentTimeMillis() - startedAtMs,
    )
}
