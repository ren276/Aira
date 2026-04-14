package com.aira.health.data.repository

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.HrvSample
import com.aira.health.data.local.model.SleepSession
import com.aira.health.data.model.ConfidenceRouter
import com.aira.health.domain.repository.HealthDataRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Primary implementation of [HealthDataRepository] backed by Health Connect.
 * All reads are suspend functions safe to call on any dispatcher.
 *
 * @param healthConnectClient Injected by Hilt — nullable because [HealthConnectClient] may not
 *   be available on older devices or if the provider APK is not installed.
 */
class HealthConnectRepositoryImpl @Inject constructor(
    private val healthConnectClient: HealthConnectClient
) : HealthDataRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun isAvailable(): Boolean = true // client was instantiated successfully

    override suspend fun readHeartRate(start: Instant, end: Instant): List<HrSample> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.flatMap { record ->
            val pkg = record.metadata.dataOrigin.packageName
            val confidence = ConfidenceRouter.getConfidenceFloat(pkg)
            record.samples.map { sample ->
                HrSample(
                    timestamp = sample.time.toEpochMilli(),
                    bpm = sample.beatsPerMinute.toInt(),
                    sourcePackage = pkg,
                    context = "unknown",
                    confidence = confidence
                )
            }
        }
    }

    override suspend fun readHeartRateVariability(start: Instant, end: Instant): List<HrvSample> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.map { record ->
            val pkg = record.metadata.dataOrigin.packageName
            HrvSample(
                timestamp = record.time.toEpochMilli(),
                rmssd = record.heartRateVariabilityMillis.toFloat(),
                sourcePackage = pkg,
                confidence = ConfidenceRouter.getConfidenceFloat(pkg)
            )
        }
    }

    override suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSession> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.map { record ->
            val pkg = record.metadata.dataOrigin.packageName
            val startTime = record.startTime
            val endTime = record.endTime
            val date = LocalDate.ofInstant(startTime, ZoneId.systemDefault())
                .format(dateFormatter)
            val durationMin = ((endTime.toEpochMilli() - startTime.toEpochMilli()) / 60_000).toInt()

            // Map sleep stage minutes from stages list
            var remMin = 0; var deepMin = 0; var lightMin = 0; var awakeMin = 0
            record.stages.forEach { stage ->
                val mins = ((stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()) / 60_000).toInt()
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_REM -> remMin += mins
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMin += mins
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMin += mins
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awakeMin += mins
                    else -> Unit
                }
            }

            SleepSession(
                date = date,
                startTime = startTime.toEpochMilli(),
                endTime = endTime.toEpochMilli(),
                durationMin = durationMin,
                remMin = remMin,
                deepMin = deepMin,
                lightMin = lightMin,
                awakeMin = awakeMin,
                sourcePackage = pkg,
                confidence = ConfidenceRouter.getConfidenceFloat(pkg)
            )
        }
    }

    override suspend fun readSpO2(start: Instant, end: Instant): List<Pair<Long, Float>> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.map { record ->
            Pair(record.time.toEpochMilli(), record.percentage.value.toFloat())
        }
    }

    override suspend fun readActiveCalories(start: Instant, end: Instant): List<Pair<Long, Double>> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.map { record ->
            Pair(record.startTime.toEpochMilli(), record.energy.inKilocalories)
        }
    }

    override suspend fun readSteps(start: Instant, end: Instant): List<Pair<Long, Long>> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response.records.map { record ->
            Pair(record.startTime.toEpochMilli(), record.count)
        }
    }
}
