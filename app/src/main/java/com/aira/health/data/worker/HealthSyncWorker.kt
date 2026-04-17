package com.aira.health.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aira.health.BuildConfig
import com.aira.health.data.local.dao.HrSampleDao
import com.aira.health.data.local.dao.HrvSampleDao
import com.aira.health.data.local.dao.SleepSessionDao
import com.aira.health.data.local.dao.WorkoutSessionDao
import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.domain.repository.HealthDataRepository
import com.aira.health.domain.usecase.ComputeDailyScoresUseCase
import com.aira.health.domain.usecase.IngestHealthDataUseCase
import com.aira.health.domain.usecase.SyncStravaActivitiesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    private val syncStravaActivities: SyncStravaActivitiesUseCase,
    private val computeDailyScores: ComputeDailyScoresUseCase,
    private val hrSampleDao: HrSampleDao,
    private val hrvSampleDao: HrvSampleDao,
    private val sleepSessionDao: SleepSessionDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val healthDataRepository: HealthDataRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            // Step 1 — Ingest fresh health data from Health Connect
            // Ingest should not abort score computation when one source read fails.
            runCatching { ingestHealthData() }

            // Step 1b — Ingest Strava activities into Room. Keep score computation resilient
            // by handling external API failures without aborting the worker.
            runCatching { syncStravaActivities() }

            // Step 2 — Compute and persist scores for today
            val today = LocalDate.now().toString()
            val yesterday = LocalDate.now().minusDays(1).toString()
            val zoneId = ZoneId.systemDefault()
            val dayStartInstant = LocalDate.now().atStartOfDay(zoneId).toInstant()
            val now = Instant.now()
            val dayStartMs = dayStartInstant.toEpochMilli()
            val nowMs = now.toEpochMilli()

            val hrSamples = hrSampleDao.getRange(dayStartMs, nowMs)
            val hrvSamples = hrvSampleDao.getRange(dayStartMs, nowMs)
            val sleepSessions = sleepSessionDao.getRange(yesterday, today)
            val workouts = workoutSessionDao.getRange(dayStartMs, nowMs)

            val earlyMorningStart = LocalDate.now().atTime(3, 0).atZone(zoneId).toInstant().toEpochMilli()
            val earlyMorningEnd = LocalDate.now().atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
            val earlyHr = hrSamples.filter { it.timestamp in earlyMorningStart..earlyMorningEnd }

            val rhrMorning = (earlyHr.minOfOrNull { it.bpm } ?: hrSamples.minOfOrNull { it.bpm })?.toFloat()
            val hrvMorning = hrvSamples.minByOrNull { it.timestamp }?.rmssd

            // Prefer the latest session that ended in the active scoring window (overnight included).
            val sleepWindowStartMs = dayStartInstant.minusSeconds(12 * 60 * 60).toEpochMilli()
            val mainSleep = sleepSessions
                .filter { it.endTime in sleepWindowStartMs..nowMs }
                .maxByOrNull { it.endTime }
                ?: sleepSessions.maxByOrNull { it.endTime }
            val sleepDurationMin = mainSleep?.durationMin
            val sleepEfficiency = mainSleep?.let { session ->
                if (session.efficiency > 0f) {
                    session.efficiency.coerceIn(0f, 1f)
                } else {
                    val awake = session.awakeMin.coerceAtLeast(0)
                    val duration = session.durationMin.coerceAtLeast(1)
                    ((duration - awake).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
            }
            val sleepDeepFraction = mainSleep?.let { session ->
                if (session.durationMin <= 0) null
                else ((session.deepMin + session.remMin).toFloat() / session.durationMin.toFloat()).coerceIn(0f, 1f)
            }

            val hourlyStressScores = hrSamples
                .groupBy {
                    Instant.ofEpochMilli(it.timestamp).atZone(zoneId).hour
                }
                .toSortedMap()
                .values
                .map { hourSamples ->
                    val avgBpm = hourSamples.map { it.bpm }.average().toFloat()
                    ((avgBpm - 48f) * 1.2f).coerceIn(0f, 100f)
                }

            val totalActiveMin = workouts.sumOf { it.durationMin.toDouble() }.toFloat().takeIf { it > 0f }

            val spo2Values = runCatching {
                healthDataRepository.readSpO2(dayStartInstant, now)
            }.getOrDefault(emptyList()).map { it.second }
            val avgSpo2 = if (spo2Values.isNotEmpty()) spo2Values.average().toFloat() else null

            val healthSteps = runCatching {
                healthDataRepository.readSteps(dayStartInstant, now)
            }.getOrDefault(emptyList())
                .sumOf { it.second }
                .coerceAtLeast(0L)

            val stravaSteps = workouts
                .filter { it.sourcePackage == STRAVA_SOURCE_PACKAGE }
                .sumOf { (it.steps ?: 0).toLong() }
                .coerceAtLeast(0L)

            val totalSteps = (healthSteps + stravaSteps)
                .toInt()
                .takeIf { it > 0 }

            val healthActiveCalories = runCatching {
                healthDataRepository.readActiveCalories(dayStartInstant, now)
            }.getOrDefault(emptyList())
                .sumOf { it.second }
                .toInt()
                .coerceAtLeast(0)

            val stravaActiveCalories = workouts
                .filter { it.sourcePackage == STRAVA_SOURCE_PACKAGE }
                .sumOf { it.activeCalories }
                .coerceAtLeast(0)

            val activeCalories = (healthActiveCalories + stravaActiveCalories)
                .takeIf { it > 0 }

            val totalDistanceMeters = workouts
                .filter { it.sourcePackage == STRAVA_SOURCE_PACKAGE }
                .sumOf { (it.distanceMeters ?: 0f).toDouble() }
                .toFloat()
                .takeIf { it > 0f }

            val zoneMinutes = deriveHeartRateZoneMinutes(
                hrSamples = hrSamples,
                workouts = workouts,
                restingHr = rhrMorning
            )

            if (BuildConfig.DEBUG) {
                Log.i(
                    TAG,
                    "Today=$today raw Room -> hr=${hrSamples.size}, hrv=${hrvSamples.size}, sleep=${sleepSessions.size}, workouts=${workouts.size}; derived -> rhrMorning=$rhrMorning, hrvMorning=$hrvMorning, sleepDurationMin=$sleepDurationMin, sleepEfficiency=$sleepEfficiency, sleepDeepFraction=$sleepDeepFraction, totalActiveMin=$totalActiveMin, spo2Count=${spo2Values.size}, avgSpo2=$avgSpo2, steps=$totalSteps (hc=$healthSteps, strava=$stravaSteps), activeCalories=$activeCalories (hc=$healthActiveCalories, strava=$stravaActiveCalories), distanceMeters=$totalDistanceMeters, zones=[${zoneMinutes.zone1}, ${zoneMinutes.zone2}, ${zoneMinutes.zone3}, ${zoneMinutes.zone4}, ${zoneMinutes.zone5}]"
                )
            }

            computeDailyScores.computeForDate(
                date = today,
                hrvMorning = hrvMorning,
                rhrMorning = rhrMorning,
                sleepDurationMin = sleepDurationMin,
                sleepEfficiency = sleepEfficiency,
                sleepDeepFraction = sleepDeepFraction,
                hourlyStressScores = hourlyStressScores,
                zone1Min = zoneMinutes.zone1,
                zone2Min = zoneMinutes.zone2,
                zone3Min = zoneMinutes.zone3,
                zone4Min = zoneMinutes.zone4,
                zone5Min = zoneMinutes.zone5,
                totalActiveMin = totalActiveMin,
                totalSteps = totalSteps,
                totalDistanceMeters = totalDistanceMeters,
                activeCalories = activeCalories,
                spo2 = avgSpo2,
                skinTemperature = null
            )

            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Score computation finished for $today; Home should read the persisted DailyMetrics row next.")
            }

            Result.success()
        }.getOrElse { _ ->
            // Retry on transient failures; fail permanently after 3 attempts
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun deriveHeartRateZoneMinutes(
        hrSamples: List<HrSample>,
        workouts: List<WorkoutSession>,
        restingHr: Float?
    ): ZoneMinutes {
        val workoutHrSignals = workouts
            .mapNotNull { workout ->
                val avgHr = workout.avgHr.takeIf { it > 0 }?.toFloat() ?: return@mapNotNull null
                val duration = workout.durationMin.takeIf { it > 0 }?.toFloat() ?: return@mapNotNull null
                avgHr to duration
            }

        if (hrSamples.isEmpty() && workoutHrSignals.isEmpty()) {
            return ZoneMinutes()
        }

        val baseResting = restingHr?.takeIf { it > 0f }
            ?: hrSamples.minOfOrNull { it.bpm }?.toFloat()
            ?: workoutHrSignals.minOfOrNull { it.first }
            ?: 60f

        val observedPeak = maxOf(
            hrSamples.maxOfOrNull { it.bpm }?.toFloat() ?: (baseResting + 40f),
            workoutHrSignals.maxOfOrNull { it.first } ?: (baseResting + 40f)
        )
        val zoneMaxHr = maxOf(observedPeak, baseResting + 30f)

        var z1 = 0f
        var z2 = 0f
        var z3 = 0f
        var z4 = 0f
        var z5 = 0f

        fun addMinutesToZone(bpm: Float, minutes: Float) {
            if (minutes <= 0f) return
            val reserve = (zoneMaxHr - baseResting).coerceAtLeast(1f)
            val intensity = ((bpm - baseResting) / reserve).coerceIn(0f, 1f)
            when {
                intensity < 0.60f -> z1 += minutes
                intensity < 0.70f -> z2 += minutes
                intensity < 0.80f -> z3 += minutes
                intensity < 0.90f -> z4 += minutes
                else -> z5 += minutes
            }
        }

        if (hrSamples.isNotEmpty()) {
            val sorted = hrSamples.sortedBy { it.timestamp }
            sorted.forEachIndexed { index, sample ->
                val intervalMinutes = if (index < sorted.lastIndex) {
                    ((sorted[index + 1].timestamp - sample.timestamp) / 60_000f).coerceIn(0f, 2f)
                } else {
                    0.5f
                }
                addMinutesToZone(sample.bpm.toFloat(), intervalMinutes)
            }
        }

        workoutHrSignals.forEach { (avgHr, durationMinutes) ->
            addMinutesToZone(avgHr, durationMinutes)
        }

        return ZoneMinutes(
            zone1 = z1,
            zone2 = z2,
            zone3 = z3,
            zone4 = z4,
            zone5 = z5
        )
    }

    private data class ZoneMinutes(
        val zone1: Float? = null,
        val zone2: Float? = null,
        val zone3: Float? = null,
        val zone4: Float? = null,
        val zone5: Float? = null
    )

    companion object {
        private const val TAG = "AiraHealthSync"
        private const val STRAVA_SOURCE_PACKAGE = "com.strava"
        const val WORK_NAME = "aira_health_sync"

        /**
         * Enqueues the periodic sync work. Safe to call multiple times; uses
         * [ExistingPeriodicWorkPolicy.KEEP] to avoid re-scheduling if already queued.
         */
        fun schedule(context: Context) {
            schedule(context, WorkManager.getInstance(context))
        }

        internal fun schedule(context: Context, workManager: WorkManager) {
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

            workManager.enqueueUniquePeriodicWork(
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
