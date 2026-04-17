package com.aira.health.ai.runtime

import com.google.mediapipe.tasks.genai.llminference.LlmInference

/**
 * Immutable configuration for the on-device AI runtime engine.
 *
 * Values come from the AI-SPEC (Phase 7, Section 4) and are aligned to
 * MediaPipe Tasks GenAI 0.10.22 option builder field names.
 *
 * Do not change these values in feature code — they are phase-locked for
 * regression-safe benchmarking. A future phase will expose device-tier profiles.
 */
data class RuntimeConfig(
    /** Maximum number of tokens the engine can produce per session. */
    val maxTokens: Int = DEFAULT_MAX_TOKENS,

    /** Engine-level top-K sampling limit. */
    val maxTopK: Int = DEFAULT_MAX_TOP_K,

    /** Session-level top-K sampling value. Must be <= [maxTopK]. */
    val topK: Int = DEFAULT_TOP_K,

    /** Session-level nucleus sampling probability. */
    val topP: Float = DEFAULT_TOP_P,

    /** Session-level temperature for output diversity control. */
    val temperature: Float = DEFAULT_TEMPERATURE,

    /** Fixed random seed for deterministic QA/test evaluation runs. */
    val randomSeed: Int = DEFAULT_RANDOM_SEED,

    /**
     * Preferred inference backend.
     * CPU is the safe default for this phase — GPU/OpenCL has device-specific stability risks.
     */
    val backend: LlmInference.Backend = LlmInference.Backend.CPU,

    /** Coroutine-level timeout applied around each generation call. */
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
) {
    companion object {
        const val DEFAULT_MAX_TOKENS: Int = 1024
        const val DEFAULT_MAX_TOP_K: Int = 64
        const val DEFAULT_TOP_K: Int = 40
        const val DEFAULT_TOP_P: Float = 0.9f
        const val DEFAULT_TEMPERATURE: Float = 0.2f
        const val DEFAULT_RANDOM_SEED: Int = 7
        const val DEFAULT_TIMEOUT_MS: Long = 2_500L
    }
}
