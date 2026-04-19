package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "continuity_sync_state")
data class ContinuitySyncState(
    @PrimaryKey val key: String = "primary",
    val userId: String,
    val lastSnapshotId: String?,
    val lastSuccessEpochMs: Long?,
    val lastAttemptEpochMs: Long,
    val retryCount: Int,
    val lastErrorCode: String?
)
