package com.aira.health.domain.model

data class StravaSyncSummary(
    val insertedCount: Int,
    val skippedCount: Int,
    val pagesFetched: Int,
    val backfillComplete: Boolean,
    val throttled: Boolean = false,
    val deferredUntilEpochMs: Long? = null,
    val message: String? = null
)
