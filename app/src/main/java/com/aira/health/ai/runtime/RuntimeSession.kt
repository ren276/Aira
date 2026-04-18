package com.aira.health.ai.runtime

import android.os.SystemClock

/**
 * Per-request lifecycle bookkeeping value type.
 *
 * Carries the correlation id, monotonic start timestamp, and final outcome once the session
 * completes or fails. Consumers use [latencyMs] for telemetry and [failureReason]
 * to route deterministic fallback output.
 *
 * Instances are immutable value types and are never persisted to disk or
 * transmitted off-device — they live only in memory for the duration of a request.
 */
data class RuntimeSession(
    /** Opaque id matching [AiRuntimeRequest.requestId]. */
    val requestId: String,

    /** Monotonic elapsed timestamp when the session was created. */
    val startedAtElapsedMs: Long = SystemClock.elapsedRealtime(),

    /** Terminal failure reason when the session did not complete successfully. Null on success. */
    val failureReason: RuntimeFailureReason? = null,

    /** Elapsed duration from [startedAtElapsedMs] to completion. Null until session ends. */
    val latencyMs: Long? = null,
) {

    /** True if the session ended without a failure. */
    val isSuccess: Boolean get() = failureReason == null && latencyMs != null

    /**
     * Returns a copy marked as successfully completed with elapsed duration.
     */
    fun complete(): RuntimeSession = copy(
        latencyMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
    )

    /**
     * Returns a copy marked as failed with the given [reason] and elapsed duration.
     */
    fun fail(reason: RuntimeFailureReason): RuntimeSession = copy(
        failureReason = reason,
        latencyMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
    )
}
