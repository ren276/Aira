package com.aira.health.domain.model

data class StravaSyncSummary(
    val insertedCount: Int,
    val skippedCount: Int,
    val pagesFetched: Int,
    val backfillComplete: Boolean
)
