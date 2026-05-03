package com.aira.health.ai.fallback

/**
 * Structured reason for why AI generation was bypassed or failed.
 *
 * Always surface one of these to callers — never propagate raw exception messages
 * to UI or telemetry layers (T-07-06 mitigation).
 */
enum class FallbackReason {
    /** Model artifact not found, failed integrity check, or engine init failed. */
    MODEL_UNAVAILABLE,

    /** Generation did not complete within the allowed time budget. */
    TIMEOUT,

    /** Caller cancelled the request (navigation, app lifecycle, user action). */
    CANCELLED,

    /** Unexpected internal runtime error. Logged internally; opaque to UI. */
    RUNTIME_ERROR,

    /** Sensor data confidence below the minimum threshold for AI-driven output. */
    LOW_CONFIDENCE,

    /** Data is stale beyond the staleness window; fallback is safer than AI guess. */
    STALE_DATA,

    /** AI service is rate-limited or quota exceeded (e.g. HTTP 429). */
    API_THROTTLED,
}

/** Human-readable, wellness-safe display label for each reason (shown in UI). */
val FallbackReason.displayLabel: String
    get() = when (this) {
        FallbackReason.MODEL_UNAVAILABLE -> "AI not ready"
        FallbackReason.TIMEOUT          -> "Response timed out"
        FallbackReason.CANCELLED        -> "Cancelled"
        FallbackReason.RUNTIME_ERROR    -> "Temporarily unavailable"
        FallbackReason.LOW_CONFIDENCE   -> "Insufficient data"
        FallbackReason.STALE_DATA       -> "Data may be outdated"
        FallbackReason.API_THROTTLED    -> "AI service busy"
    }
