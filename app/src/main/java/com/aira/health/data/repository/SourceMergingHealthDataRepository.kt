package com.aira.health.data.repository

import com.aira.health.BuildConfig
import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.HrvSample
import com.aira.health.data.local.model.SleepSession
import com.aira.health.domain.repository.HealthDataRepository
import java.time.Instant
import javax.inject.Inject
import android.util.Log

/**
 * Merges Health Connect and Google Fit reads so current Health Connect users can still
 * surface Fit-origin data when it has not yet been migrated or when one source is sparse.
 *
 * Health Connect remains the preferred source for modern on-device data.
 */
class SourceMergingHealthDataRepository @Inject constructor(
    private val healthConnectRepository: HealthDataRepository,
    private val googleFitRepository: HealthDataRepository,
) : HealthDataRepository {

    companion object {
        private const val TAG = "AiraHealthSourceMerge"
    }

    override suspend fun isAvailable(): Boolean {
        val healthConnectAvailable = runCatching { healthConnectRepository.isAvailable() }.getOrDefault(false)
        val googleFitAvailable = runCatching { googleFitRepository.isAvailable() }.getOrDefault(false)
        return healthConnectAvailable || googleFitAvailable
    }

    override suspend fun readHeartRate(start: Instant, end: Instant): List<HrSample> {
        val healthConnect = runCatching { healthConnectRepository.readHeartRate(start, end) }.getOrDefault(emptyList())
        val googleFit = runCatching { googleFitRepository.readHeartRate(start, end) }.getOrDefault(emptyList())
        return mergeAndLog("heart_rate", healthConnect, googleFit) { it.timestamp }
    }

    override suspend fun readHeartRateVariability(start: Instant, end: Instant): List<HrvSample> {
        val healthConnect = runCatching { healthConnectRepository.readHeartRateVariability(start, end) }.getOrDefault(emptyList())
        val googleFit = runCatching { googleFitRepository.readHeartRateVariability(start, end) }.getOrDefault(emptyList())
        return mergeAndLog("hrv", healthConnect, googleFit) { it.timestamp }
    }

    override suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSession> {
        val healthConnect = runCatching { healthConnectRepository.readSleepSessions(start, end) }.getOrDefault(emptyList())
        val googleFit = runCatching { googleFitRepository.readSleepSessions(start, end) }.getOrDefault(emptyList())
        return mergeAndLog("sleep", healthConnect, googleFit) { it.startTime }
    }

    override suspend fun readSpO2(start: Instant, end: Instant): List<Pair<Long, Float>> {
        val healthConnect = runCatching { healthConnectRepository.readSpO2(start, end) }.getOrDefault(emptyList())
        val googleFit = runCatching { googleFitRepository.readSpO2(start, end) }.getOrDefault(emptyList())
        return mergeAndLog("spo2", healthConnect, googleFit) { it.first }
    }

    override suspend fun readActiveCalories(start: Instant, end: Instant): List<Pair<Long, Double>> {
        val healthConnect = runCatching { healthConnectRepository.readActiveCalories(start, end) }.getOrDefault(emptyList())
        val googleFit = runCatching { googleFitRepository.readActiveCalories(start, end) }.getOrDefault(emptyList())
        return mergeAndLog("active_calories", healthConnect, googleFit) { it.first }
    }

    override suspend fun readSteps(start: Instant, end: Instant): List<Pair<Long, Long>> {
        val healthConnect = runCatching { healthConnectRepository.readSteps(start, end) }.getOrDefault(emptyList())
        val googleFit = runCatching { googleFitRepository.readSteps(start, end) }.getOrDefault(emptyList())
        return mergeAndLog("steps", healthConnect, googleFit) { it.first }
    }

    private fun <T, K : Comparable<K>> mergeAndLog(
        query: String,
        healthConnect: List<T>,
        googleFit: List<T>,
        keySelector: (T) -> K,
    ): List<T> {
        val merged = (healthConnect + googleFit)
            .distinctBy(keySelector)
            .sortedBy(keySelector)
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "$query hc=${healthConnect.size} fit=${googleFit.size} merged=${merged.size}"
            )
        }
        return merged
    }
}