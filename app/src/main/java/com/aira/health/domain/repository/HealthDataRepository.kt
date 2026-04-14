package com.aira.health.domain.repository

import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.HrvSample
import com.aira.health.data.local.model.SleepSession
import java.time.Instant

/**
 * Pure Kotlin interface for reading biometric data from any available source.
 * Implementations live in the data layer (Health Connect, Google Fit).
 * No Android health SDK imports here.
 */
interface HealthDataRepository {

    /** Read heart rate samples within the given time range. */
    suspend fun readHeartRate(start: Instant, end: Instant): List<HrSample>

    /** Read HRV (RMSSD) samples within the given time range. */
    suspend fun readHeartRateVariability(start: Instant, end: Instant): List<HrvSample>

    /** Read sleep sessions within the given time range. */
    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSession>

    /** Read SpO2 readings within the given time range. Returns list of (timestamp, percent) pairs. */
    suspend fun readSpO2(start: Instant, end: Instant): List<Pair<Long, Float>>

    /** Read active calories burned within the given time range. */
    suspend fun readActiveCalories(start: Instant, end: Instant): List<Pair<Long, Double>>

    /** Read steps within the given time range. */
    suspend fun readSteps(start: Instant, end: Instant): List<Pair<Long, Long>>

    /** Check whether the underlying data source is available on this device. */
    suspend fun isAvailable(): Boolean
}
