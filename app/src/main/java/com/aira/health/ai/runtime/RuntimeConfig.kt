package com.aira.health.ai.runtime



/**
 * Immutable configuration for AI runtime requests.
 *
 * Values come from the AI-SPEC (Phase 7, Section 4) and are aligned to
 * Gemini generation parameters used by [GeminiCloudRuntimeGateway].
 *
 * Do not change these values in feature code — they are phase-locked for
 * regression-safe benchmarking. A future phase will expose device-tier profiles.
 */
data class RuntimeConfig(
    /** Maximum number of tokens requested from the model per generation call. */
    val maxTokens: Int = DEFAULT_MAX_TOKENS,

    /** Upper bound for top-K sampling used by request generation config. */
    val maxTopK: Int = DEFAULT_MAX_TOP_K,

    /** Request-level top-K sampling value. Must be <= [maxTopK]. */
    val topK: Int = DEFAULT_TOP_K,

    /** Request-level nucleus sampling probability. */
    val topP: Float = DEFAULT_TOP_P,

    /** Request-level temperature for output diversity control. */
    val temperature: Float = DEFAULT_TEMPERATURE,

    /** Fixed random seed for deterministic QA/test evaluation runs. */
    val randomSeed: Int = DEFAULT_RANDOM_SEED,

    /**
     * Preferred local backend when a device-side runtime is introduced.
     * Currently informational only for the cloud gateway path.
     */
    val backend: HardwareBackend = HardwareBackend.CPU,

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
        const val DEFAULT_TIMEOUT_MS: Long = 30_000L
    }

    enum class HardwareBackend { CPU, GPU }
}
