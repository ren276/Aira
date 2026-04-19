package com.aira.health.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.aira.health.data.local.dao.ContinuitySyncStateDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.ContinuitySyncState
import com.aira.health.domain.model.ContinuitySnapshot
import com.aira.health.domain.repository.ContinuitySnapshotRepository
import com.aira.health.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UploadContinuitySnapshotUseCase @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val userRepository: UserRepository,
    private val dailyMetricsDao: DailyMetricsDao,
    private val continuitySyncStateDao: ContinuitySyncStateDao,
    private val continuitySnapshotRepository: ContinuitySnapshotRepository
) {

    private companion object {
        val CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
    }

    suspend operator fun invoke(force: Boolean = false): Result<Boolean> {
        val cloudEnabled = dataStore.data.first()[CLOUD_BACKUP_ENABLED] ?: false
        if (!cloudEnabled && !force) return Result.success(false)

        val session = userRepository.getCurrentSession()
            ?: return Result.failure(IllegalStateException("No active user session for continuity upload"))

        val latest = dailyMetricsDao.observeRecent(1).first().firstOrNull()
            ?: return Result.failure(IllegalStateException("No daily metrics available for continuity snapshot"))

        val snapshot = ContinuitySnapshot(
            snapshotId = "${session.userId}-${latest.date}",
            capturedAtEpochMs = System.currentTimeMillis(),
            recoveryScore = latest.recoveryScore,
            sleepScore = latest.sleepScore,
            strainScore = latest.strainScore,
            stressScore = latest.stressScore,
            energyBankScore = latest.energyBankScore,
            burnoutRiskIndex = latest.burnoutRiskIndex,
            dataConfidence = latest.dataConfidence,
            cloudBackupEnabled = cloudEnabled
        )

        val uploadResult = continuitySnapshotRepository.uploadSnapshot(session.userId, snapshot)
        val now = System.currentTimeMillis()

        return if (uploadResult.isSuccess) {
            continuitySyncStateDao.upsert(
                ContinuitySyncState(
                    userId = session.userId,
                    lastSnapshotId = snapshot.snapshotId,
                    lastSuccessEpochMs = now,
                    lastAttemptEpochMs = now,
                    retryCount = 0,
                    lastErrorCode = null
                )
            )
            Result.success(true)
        } else {
            val previous = continuitySyncStateDao.get()
            continuitySyncStateDao.upsert(
                ContinuitySyncState(
                    userId = session.userId,
                    lastSnapshotId = previous?.lastSnapshotId,
                    lastSuccessEpochMs = previous?.lastSuccessEpochMs,
                    lastAttemptEpochMs = now,
                    retryCount = (previous?.retryCount ?: 0) + 1,
                    lastErrorCode = uploadResult.exceptionOrNull()?.javaClass?.simpleName
                )
            )
            Result.failure(uploadResult.exceptionOrNull() ?: IllegalStateException("Upload failed"))
        }
    }
}
