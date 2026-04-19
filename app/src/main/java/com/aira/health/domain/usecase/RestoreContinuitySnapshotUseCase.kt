package com.aira.health.domain.usecase

import com.aira.health.domain.model.ContinuitySnapshot
import com.aira.health.domain.repository.ContinuitySnapshotRepository
import com.aira.health.domain.repository.UserRepository
import javax.inject.Inject

data class RestoreContinuityResult(
    val restored: Boolean,
    val snapshot: ContinuitySnapshot?
)

class RestoreContinuitySnapshotUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val continuitySnapshotRepository: ContinuitySnapshotRepository
) {

    suspend fun fetchLatest(): Result<ContinuitySnapshot?> {
        val session = userRepository.getCurrentSession()
            ?: return Result.failure(IllegalStateException("No active user session for continuity restore"))
        return continuitySnapshotRepository.getLatestSnapshot(session.userId)
    }

    suspend fun applySelected(snapshot: ContinuitySnapshot?): Result<RestoreContinuityResult> {
        return Result.success(
            RestoreContinuityResult(
                restored = snapshot != null,
                snapshot = snapshot
            )
        )
    }
}
