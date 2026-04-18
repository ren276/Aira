package com.aira.health.domain.model

/**
 * Snapshot of ranked factors for a single metric/date.
 */
data class CausalInsightSnapshot(
    val metricKey: String,
    val date: String,
    val confidence: Float,
    val factors: List<CausalFactor>,
    val calculatedAt: Long
)
