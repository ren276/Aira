package com.aira.health.data.strava

data class StravaTokenSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSec: Long,
    val scope: String,
    val athleteId: Long
)

data class StravaSyncCursor(
    val backfillComplete: Boolean,
    val backfillBeforeEpochMs: Long?,
    val incrementalAfterEpochMs: Long?
)
