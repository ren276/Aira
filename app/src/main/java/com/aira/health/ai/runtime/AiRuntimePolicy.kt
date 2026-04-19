package com.aira.health.ai.runtime

/**
 * Runtime policy used by privacy-sensitive coaching generation paths.
 *
 * Phase 09 coaching must remain local-only. If a local runtime is unavailable,
 * callers must route to deterministic local fallback output.
 */
data class AiRuntimePolicy(
    val coachingGenerationMode: AiRuntimeExecutionMode = AiRuntimeExecutionMode.LOCAL_ONLY,
    val allowNetworkFallback: Boolean = false,
)

/**
 * Structured policy evaluation output for runtime compliance checks.
 */
data class AiRuntimePolicyDecision(
    val allowed: Boolean,
    val requiredMode: AiRuntimeExecutionMode,
    val actualMode: AiRuntimeExecutionMode,
    val reason: String? = null,
)
