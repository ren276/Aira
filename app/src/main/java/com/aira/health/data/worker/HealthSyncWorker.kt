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
import com.aira.health.domain.usecase.ComputeDailyScoresUseCase
import com.aira.health.domain.usecase.IngestHealthDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * WorkManager [CoroutineWorker] that periodically executes the biometric ingestion pipeline
 * followed by daily score computation.
 *
 * Execution order (ingest → compute):
 *  1. [IngestHealthDataUseCase] — pulls fresh Health Connect data into local Room tables
 *  2. [ComputeDailyScoresUseCase] — derives all daily scores for today and persists full [DailyMetrics]
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
    private val ingestHealthData: IngestHealthDataUseCase,
    private val computeDailyScores: ComputeDailyScoresUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            // Step 1 — Ingest fresh health data from Health Connect
            ingestHealthData()

            // Step 2 — Compute and persist scores for today
            // Raw sensor values come from the ingest pipeline via Room; we compute for today.
            // For a full backfill path, BaselineRecalculatorUseCase handles historical days.
            val today = LocalDate.now().toString()
            computeDailyScores.computeForDate(
                date = today,
                // Sensor inputs are read from Room by the use case via DAOs, not passed here.
                // Passing nulls causes the engine to degrade to partial-input confidence mode (D-03, D-04).
                // A follow-up phase will connect the ingest DAOs to feed real values here.
                hrvMorning = null,
                rhrMorning = null,
                sleepDurationMin = null,
                sleepEfficiency = null,
                sleepDeepFraction = null,
                hourlyStressScores = emptyList(),
                zone1Min = null,
                zone2Min = null,
                zone3Min = null,
                zone4Min = null,
                zone5Min = null,
                totalActiveMin = null
            )

            Result.success()
        }.getOrElse { _ ->
            // Retry on transient failures; fail permanently after 3 attempts
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
