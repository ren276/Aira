package com.aira.health.domain.model

data class PersonalizationUpdateDecision(
    val applied: Boolean,
    val parameters: PersonalizationParameters,
    val usableDays: Int,
    val skipReason: PersonalizationSkipReason? = null,
    val capped: Boolean = false
)

enum class PersonalizationSkipReason {
    INSUFFICIENT_HISTORY
}
