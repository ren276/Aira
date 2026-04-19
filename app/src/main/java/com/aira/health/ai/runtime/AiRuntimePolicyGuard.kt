package com.aira.health.ai.runtime

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces runtime privacy boundaries for coaching generation.
 */
@Singleton
class AiRuntimePolicyGuard @Inject constructor(
    private val policy: AiRuntimePolicy,
) {

    fun requireLocalOnly(gateway: AiRuntimeGateway): AiRuntimePolicyDecision {
        val requiredMode = policy.coachingGenerationMode
        val actualMode = gateway.executionMode

        if (requiredMode == AiRuntimeExecutionMode.LOCAL_ONLY && actualMode != AiRuntimeExecutionMode.LOCAL_ONLY) {
            return AiRuntimePolicyDecision(
                allowed = false,
                requiredMode = requiredMode,
                actualMode = actualMode,
                reason = "Coaching generation requires LOCAL_ONLY runtime but active gateway is $actualMode.",
            )
        }

        return AiRuntimePolicyDecision(
            allowed = true,
            requiredMode = requiredMode,
            actualMode = actualMode,
        )
    }

    fun requiresDeterministicFallback(gateway: AiRuntimeGateway): Boolean =
        !requireLocalOnly(gateway).allowed
}
