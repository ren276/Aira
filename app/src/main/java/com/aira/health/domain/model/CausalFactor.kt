package com.aira.health.domain.model

/**
 * Ranked contributor used in explainability surfaces.
 */
data class CausalFactor(
    val key: String,
    val direction: CausalDirection,
    val weight: Float,
    val windowLabel: String,
    val windowTimestamp: Long
)

enum class CausalDirection {
    INCREASED,
    DECREASED,
    NEUTRAL
}
