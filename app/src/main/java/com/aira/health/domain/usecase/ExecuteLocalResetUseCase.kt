package com.aira.health.domain.usecase

import com.aira.health.data.local.db.AiraDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LocalResetResult {
    data object Completed : LocalResetResult
    data class Blocked(val reason: String) : LocalResetResult
}

class ExecuteLocalResetUseCase @Inject constructor(
    private val uploadContinuitySnapshotUseCase: UploadContinuitySnapshotUseCase,
    private val airaDatabase: AiraDatabase
) {

    suspend operator fun invoke(allowIrreversibleOverride: Boolean): LocalResetResult {
        val uploadResult = uploadContinuitySnapshotUseCase(force = true)

        if (uploadResult.isFailure && !allowIrreversibleOverride) {
            return LocalResetResult.Blocked(
                reason = uploadResult.exceptionOrNull()?.message
                    ?: "Final continuity upload failed"
            )
        }

        withContext(Dispatchers.IO) {
            airaDatabase.clearAllTables()
        }

        return LocalResetResult.Completed
    }
}
