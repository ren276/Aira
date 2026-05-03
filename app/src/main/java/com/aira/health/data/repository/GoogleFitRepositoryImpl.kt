package com.aira.health.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.HrvSample
import com.aira.health.data.local.model.SleepSession
import com.aira.health.data.model.ConfidenceRouter
import com.aira.health.domain.repository.HealthDataRepository
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Fallback [HealthDataRepository] implementation backed by the Google Fit History API.
 *
 * Used on Android 10, 11, and 12 devices where Health Connect is not available.
 * Google Fit data is mapped to the same Room entity types as [HealthConnectRepositoryImpl],
 * ensuring the rest of the pipeline is source-agnostic.
 *
 * NOTE: Google Fit API is deprecated but still functional for read-only historical
 * access on legacy devices. New writes use Health Connect exclusively.
 */
class GoogleFitRepositoryImpl @Inject constructor(
    private val context: Context
) : HealthDataRepository {

    // Google Fit uses "com.google.android.apps.fitness" as the canonical package name
    private val googleFitPackage = "com.google.android.apps.fitness"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun isAvailable(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(
            account,
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, com.google.android.gms.fitness.FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, com.google.android.gms.fitness.FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, com.google.android.gms.fitness.FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_SLEEP_SEGMENT, com.google.android.gms.fitness.FitnessOptions.ACCESS_READ)
                .build()
        )
    }

    override suspend fun readHeartRate(start: Instant, end: Instant): List<HrSample> {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return emptyList()
        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(start.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()

        return runCatching {
            val result = Fitness.getHistoryClient(context, account)
                .readData(request).await()
            result.getDataSet(DataType.TYPE_HEART_RATE_BPM).dataPoints.map { point ->
                val pkg = point.dataSource.appPackageName ?: googleFitPackage
                HrSample(
                    timestamp = point.getStartTime(TimeUnit.MILLISECONDS),
                    bpm = point.getValue(DataType.TYPE_HEART_RATE_BPM.fields[0]).asFloat().toInt(),
                    sourcePackage = pkg,
                    context = "unknown",
                    confidence = ConfidenceRouter.getConfidenceFloat(pkg)
                )
            }
        }.getOrElse { emptyList() }
    }

    override suspend fun readHeartRateVariability(start: Instant, end: Instant): List<HrvSample> {
        // Google Fit does not expose RMSSD directly — return empty list; HC is preferred source
        return emptyList()
    }

    override suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSession> {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return emptyList()
        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_SLEEP_SEGMENT)
            .setTimeRange(start.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()

        return runCatching {
            val result = Fitness.getHistoryClient(context, account)
                .readData(request).await()
            val sessions = mutableListOf<SleepSession>()
            result.getDataSet(DataType.TYPE_SLEEP_SEGMENT).dataPoints.forEach { point ->
                val startMs = point.getStartTime(TimeUnit.MILLISECONDS)
                val endMs = point.getEndTime(TimeUnit.MILLISECONDS)
                val durationMin = ((endMs - startMs) / 60_000).toInt()
                val date = LocalDate.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault())
                    .format(dateFormatter)
                val pkg = point.dataSource.appPackageName ?: googleFitPackage
                sessions.add(
                    SleepSession(
                        date = date,
                        startTime = startMs,
                        endTime = endMs,
                        durationMin = durationMin,
                        sourcePackage = pkg,
                        confidence = ConfidenceRouter.getConfidenceFloat(pkg)
                    )
                )
            }
            sessions
        }.getOrElse { emptyList() }
    }

    override suspend fun readSpO2(start: Instant, end: Instant): List<Pair<Long, Float>> {
        // Google Fit SpO2 requires Wear OS device; return empty for phone-only fallback
        return emptyList()
    }

    override suspend fun readActiveCalories(start: Instant, end: Instant): List<Pair<Long, Double>> {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return emptyList()
        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_CALORIES_EXPENDED)
            .setTimeRange(start.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()

        return runCatching {
            val result = Fitness.getHistoryClient(context, account)
                .readData(request).await()
            result.getDataSet(DataType.TYPE_CALORIES_EXPENDED).dataPoints.map { point ->
                Pair(
                    point.getStartTime(TimeUnit.MILLISECONDS),
                    point.getValue(DataType.TYPE_CALORIES_EXPENDED.fields[0]).asFloat().toDouble()
                )
            }
        }.getOrElse { emptyList() }
    }

    override suspend fun readSteps(start: Instant, end: Instant): List<Pair<Long, Long>> {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return emptyList()
        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_STEP_COUNT_DELTA)
            .setTimeRange(start.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()

        return runCatching {
            val result = Fitness.getHistoryClient(context, account)
                .readData(request).await()
            result.getDataSet(DataType.TYPE_STEP_COUNT_DELTA).dataPoints.map { point ->
                Pair(
                    point.getStartTime(TimeUnit.MILLISECONDS),
                    point.getValue(DataType.TYPE_STEP_COUNT_DELTA.fields[0]).asInt().toLong()
                )
            }
        }.getOrElse { emptyList() }
    }

    override suspend fun readVo2Max(start: Instant, end: Instant): List<Pair<Long, Double>> {
        // Google Fit does not expose VO2 Max in standard types — return empty list
        return emptyList()
    }
}
