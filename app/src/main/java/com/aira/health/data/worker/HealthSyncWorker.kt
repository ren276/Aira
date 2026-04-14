package com.aira.health.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aira.health.domain.usecase.IngestHealthDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager [CoroutineWorker] that periodically executes the biometric ingestion pipeline.
 *
 * Scheduling strategy (from CONTEXT.md):
 * - Periodic interval: 30 minutes (minimum system-enforced interval with flex 15 min)
 * - No strict network requirement — Health Connect reads are entirely local
 * - Exponential backoff on failure (10 min initial, 30 min max)
 *
 * The worker is Hilt-injectable via @HiltWorker / @AssistedInject.
 * WorkManager is initialised by [com.aira.health.AiraApplication] which implements
 * [androidx.work.Configuration.Provider] with a Hilt-aware factory.
 */
@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ingestHealthData: IngestHealthDataUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val count = ingestHealthData()
            Result.success()
        }.getOrElse { throwable ->
            // Retry on transient failures; fail permanently for programming errors
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "aira_health_sync"

        /**
         * Enqueues the periodic sync work. Safe to call multiple times; uses
         * [ExistingPeriodicWorkPolicy.KEEP] to avoid re-scheduling if already queued.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Local Health Connect reads only
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 15,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Immediately triggers a one-time sync (fast-sync on app foreground).
         * Uses a different unique name so it doesn't conflict with the periodic request.
         */
        fun scheduleImmediate(context: Context) {
            val immediateRequest = androidx.work.OneTimeWorkRequestBuilder<HealthSyncWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                androidx.work.ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
        }
    }
}
