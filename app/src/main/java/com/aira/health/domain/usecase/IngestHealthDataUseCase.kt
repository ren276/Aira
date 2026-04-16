package com.aira.health.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import com.aira.health.data.local.dao.HrSampleDao
import com.aira.health.data.local.dao.HrvSampleDao
import com.aira.health.data.local.dao.SleepSessionDao
import com.aira.health.data.local.model.HrSample
import com.aira.health.data.local.model.HrvSample
import com.aira.health.data.local.model.SleepSession
import com.aira.health.data.model.ConfidenceRouter
import com.aira.health.domain.repository.HealthDataRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Orchestrates the full health data ingestion pipeline.
 *
 * Strategy:
 * - First sync: query last 14 days to bootstrap provisional baselines (avoids cold-start)
 * - Subsequent syncs: query from last successful sync timestamp stored in DataStore
 * - Conflict resolution: "Highest Confidence Source Wins" via [ConfidenceRouter]
 * - Data integrity: gaps are preserved (no fake entries inserted for missing data)
 * - Privacy: all data is persisted to local Room DB only — no network calls here
 */
class IngestHealthDataUseCase @Inject constructor(
    private val repository: HealthDataRepository,
    private val hrSampleDao: HrSampleDao,
    private val hrvSampleDao: HrvSampleDao,
    private val sleepSessionDao: SleepSessionDao,
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val LAST_SYNC_KEY = longPreferencesKey("health_last_sync_epoch_ms")
        /** 14 days in milliseconds — used for first-launch backfill. */
        private const val BACKFILL_DAYS = 14L
        /** Re-read a small overlap window to capture delayed writes and late source syncs. */
        private const val SYNC_OVERLAP_HOURS = 36L
        /** Maximum age of records to purge (90 days rolling window in raw tables). */
        private const val PURGE_AFTER_DAYS = 90L
    }

    /**
     * Execute the ingestion pipeline. Safe to call from WorkManager or foreground fast-sync.
     *
     * @return The number of records ingested across all data types.
     */
    suspend operator fun invoke(): Int {
        val now = Instant.now()
        val prefs = dataStore.data.first()
        val lastSyncMs = prefs[LAST_SYNC_KEY]

        // Determine the start of the sync window.
        // For non-first syncs, overlap by 36h so we do not miss delayed records from source apps.
        val backfillStart = now.minus(BACKFILL_DAYS, ChronoUnit.DAYS)
        val syncStart = if (lastSyncMs != null) {
            val overlapStart = Instant.ofEpochMilli(lastSyncMs).minus(SYNC_OVERLAP_HOURS, ChronoUnit.HOURS)
            if (overlapStart.isAfter(backfillStart)) overlapStart else backfillStart
        } else {
            backfillStart
        }

        val sourceAvailable = runCatching { repository.isAvailable() }.getOrDefault(false)

        var totalIngested = 0

        // ── Heart Rate ────────────────────────────────────────────────────────────
        val hrSamples = runCatching {
            repository.readHeartRate(syncStart, now)
        }.getOrDefault(emptyList())
        val resolvedHr = resolveHrConflicts(hrSamples)
        hrSampleDao.insertAll(resolvedHr)
        totalIngested += resolvedHr.size

        // ── HRV ──────────────────────────────────────────────────────────────────
        val hrvSamples = runCatching {
            repository.readHeartRateVariability(syncStart, now)
        }.getOrDefault(emptyList())
        val resolvedHrv = resolveHrvConflicts(hrvSamples)
        hrvSampleDao.insertAll(resolvedHrv)
        totalIngested += resolvedHrv.size

        // ── Sleep ─────────────────────────────────────────────────────────────────
        val sleepSessions = runCatching {
            repository.readSleepSessions(syncStart, now)
        }.getOrDefault(emptyList())
        val resolvedSleep = resolveSleepConflicts(sleepSessions)
        resolvedSleep.forEach { session ->
            val existing = sleepSessionDao.getRange(session.date, session.date)
            if (existing.isEmpty()) {
                sleepSessionDao.insert(session)
            } else {
                // Replace only if our new record has higher confidence
                val best = existing.maxByOrNull { it.confidence }
                if (best != null && session.confidence > best.confidence) {
                    sleepSessionDao.insert(session)
                }
            }
        }
        totalIngested += resolvedSleep.size

        // ── Purge old raw samples (rolling 90-day window) ─────────────────────────
        val purgeBeforeMs = now.minus(PURGE_AFTER_DAYS, ChronoUnit.DAYS).toEpochMilli()
        hrSampleDao.purgeOlderThan(purgeBeforeMs)
        hrvSampleDao.purgeOlderThan(purgeBeforeMs)

        // ── Persist last successful sync timestamp ─────────────────────────────────
        dataStore.edit { it[LAST_SYNC_KEY] = now.toEpochMilli() }

        return totalIngested
    }

    // Conflict resolution: for overlapping timestamps, keep only the highest-confidence sample
    private fun resolveHrConflicts(samples: List<HrSample>): List<HrSample> {
        return samples
            .groupBy { it.timestamp }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }
    }

    private fun resolveHrvConflicts(samples: List<HrvSample>): List<HrvSample> {
        return samples
            .groupBy { it.timestamp }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }
    }

    private fun resolveSleepConflicts(sessions: List<SleepSession>): List<SleepSession> {
        return sessions
            .groupBy { it.date }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }
    }
}
