package com.aira.health.data.repository

import android.util.Log
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
import com.aira.health.BuildConfig
import com.aira.health.domain.repository.HealthDataRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

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

    companion object {
        private const val TAG = "AiraHealthConnectRepo"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun isAvailable(): Boolean = true // client was instantiated successfully

    override suspend fun readHeartRate(start: Instant, end: Instant): List<HrSample> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        if (BuildConfig.DEBUG) {
            Log.i(TAG, buildHeartRatePayload(start, end, response.records).toString())
        }
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
        if (BuildConfig.DEBUG) {
            Log.i(TAG, buildHrvPayload(start, end, response.records).toString())
        }
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
        if (BuildConfig.DEBUG) {
            Log.i(TAG, buildSleepPayload(start, end, response.records).toString())
        }
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
        if (BuildConfig.DEBUG) {
            Log.i(TAG, buildSpO2Payload(start, end, response.records).toString())
        }
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
        if (BuildConfig.DEBUG) {
            Log.i(TAG, buildActiveCaloriesPayload(start, end, response.records).toString())
        }
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
        if (BuildConfig.DEBUG) {
            Log.i(TAG, buildStepsPayload(start, end, response.records).toString())
        }
        return response.records.map { record ->
            Pair(record.startTime.toEpochMilli(), record.count)
        }
    }

    private fun buildHeartRatePayload(
        start: Instant,
        end: Instant,
        records: List<HeartRateRecord>
    ): JSONObject {
        val packages = records.map { it.metadata.dataOrigin.packageName }.distinct()
        return basePayload("heart_rate", start, end).apply {
            put("counts", JSONObject().apply {
                put("records", records.size)
                put("samples", records.sumOf { it.samples.size })
            })
            put("packages", JSONArray(packages))
            put("records", JSONArray().apply {
                records.take(10).forEach { record ->
                    put(JSONObject().apply {
                        put("package", record.metadata.dataOrigin.packageName)
                        put("sampleCount", record.samples.size)
                        put("firstSampleTime", record.samples.firstOrNull()?.time?.toString())
                        put("lastSampleTime", record.samples.lastOrNull()?.time?.toString())
                        put("samples", JSONArray().apply {
                            record.samples.take(10).forEach { sample ->
                                put(JSONObject().apply {
                                    put("time", sample.time.toString())
                                    put("bpm", sample.beatsPerMinute.toInt())
                                })
                            }
                        })
                    })
                }
            })
        }
    }

    private fun buildHrvPayload(
        start: Instant,
        end: Instant,
        records: List<HeartRateVariabilityRmssdRecord>
    ): JSONObject {
        val packages = records.map { it.metadata.dataOrigin.packageName }.distinct()
        return basePayload("hrv", start, end).apply {
            put("counts", JSONObject().apply { put("records", records.size) })
            put("packages", JSONArray(packages))
            put("records", JSONArray().apply {
                records.take(10).forEach { record ->
                    put(JSONObject().apply {
                        put("package", record.metadata.dataOrigin.packageName)
                        put("time", record.time.toString())
                        put("rmssdMs", record.heartRateVariabilityMillis)
                    })
                }
            })
        }
    }

    private fun buildSleepPayload(
        start: Instant,
        end: Instant,
        records: List<SleepSessionRecord>
    ): JSONObject {
        val packages = records.map { it.metadata.dataOrigin.packageName }.distinct()
        return basePayload("sleep", start, end).apply {
            put("counts", JSONObject().apply {
                put("records", records.size)
                put("stages", records.sumOf { it.stages.size })
            })
            put("packages", JSONArray(packages))
            put("records", JSONArray().apply {
                records.take(10).forEach { record ->
                    put(JSONObject().apply {
                        put("package", record.metadata.dataOrigin.packageName)
                        put("start", record.startTime.toString())
                        put("end", record.endTime.toString())
                        put("stageCount", record.stages.size)
                        put("stages", JSONArray().apply {
                            record.stages.take(10).forEach { stage ->
                                put(JSONObject().apply {
                                    put("type", stage.stage.toString())
                                    put("start", stage.startTime.toString())
                                    put("end", stage.endTime.toString())
                                })
                            }
                        })
                    })
                }
            })
        }
    }

    private fun buildSpO2Payload(
        start: Instant,
        end: Instant,
        records: List<OxygenSaturationRecord>
    ): JSONObject {
        val packages = records.map { it.metadata.dataOrigin.packageName }.distinct()
        return basePayload("spo2", start, end).apply {
            put("counts", JSONObject().apply { put("records", records.size) })
            put("packages", JSONArray(packages))
            put("records", JSONArray().apply {
                records.take(10).forEach { record ->
                    put(JSONObject().apply {
                        put("package", record.metadata.dataOrigin.packageName)
                        put("time", record.time.toString())
                        put("value", record.percentage.value)
                    })
                }
            })
        }
    }

    private fun buildActiveCaloriesPayload(
        start: Instant,
        end: Instant,
        records: List<TotalCaloriesBurnedRecord>
    ): JSONObject {
        val packages = records.map { it.metadata.dataOrigin.packageName }.distinct()
        return basePayload("active_calories", start, end).apply {
            put("counts", JSONObject().apply {
                put("records", records.size)
                put("totalKcal", records.sumOf { it.energy.inKilocalories })
            })
            put("packages", JSONArray(packages))
            put("records", JSONArray().apply {
                records.take(10).forEach { record ->
                    put(JSONObject().apply {
                        put("package", record.metadata.dataOrigin.packageName)
                        put("start", record.startTime.toString())
                        put("kcal", record.energy.inKilocalories)
                    })
                }
            })
        }
    }

    private fun buildStepsPayload(
        start: Instant,
        end: Instant,
        records: List<StepsRecord>
    ): JSONObject {
        val packages = records.map { it.metadata.dataOrigin.packageName }.distinct()
        return basePayload("steps", start, end).apply {
            put("counts", JSONObject().apply {
                put("records", records.size)
                put("totalSteps", records.sumOf { it.count })
            })
            put("packages", JSONArray(packages))
            put("records", JSONArray().apply {
                records.take(10).forEach { record ->
                    put(JSONObject().apply {
                        put("package", record.metadata.dataOrigin.packageName)
                        put("start", record.startTime.toString())
                        put("steps", record.count)
                    })
                }
            })
        }
    }

    private fun basePayload(query: String, start: Instant, end: Instant): JSONObject {
        return JSONObject().apply {
            put("query", query)
            put("window", JSONObject().apply {
                put("start", start.toString())
                put("end", end.toString())
            })
        }
    }
}
