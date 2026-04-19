package com.aira.health.domain.repository

import com.aira.health.domain.model.ContinuitySnapshot

interface ContinuitySnapshotRepository {
    suspend fun uploadSnapshot(userId: String, snapshot: ContinuitySnapshot): Result<Unit>
    suspend fun getLatestSnapshot(userId: String): Result<ContinuitySnapshot?>
}
