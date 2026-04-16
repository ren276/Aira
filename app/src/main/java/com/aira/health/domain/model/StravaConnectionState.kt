package com.aira.health.domain.model

data class StravaConnectionState(
    val isConnected: Boolean = false,
    val reconnectRequired: Boolean = false,
    val athleteId: Long? = null,
    val lastSyncEpochMs: Long? = null
)
