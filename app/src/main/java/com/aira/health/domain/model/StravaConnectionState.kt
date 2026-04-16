package com.aira.health.domain.model

data class StravaConnectionState(
    val isConnected: Boolean = false,
    val reconnectRequired: Boolean = false,
    val athleteId: Long? = null,
    val lastSyncEpochMs: Long? = null,
    val deferredSyncUntilEpochMs: Long? = null,
    val lastSyncErrorCode: Int? = null,
    val lastSyncErrorMessage: String? = null
)
