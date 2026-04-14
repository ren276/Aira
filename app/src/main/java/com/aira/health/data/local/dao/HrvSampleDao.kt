package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.HrvSample

@Dao
interface HrvSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(samples: List<HrvSample>)

    @Query("SELECT * FROM hrv_samples WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp ASC")
    suspend fun getRange(startMs: Long, endMs: Long): List<HrvSample>

    @Query("SELECT AVG(rmssd) FROM hrv_samples WHERE timestamp BETWEEN :startMs AND :endMs")
    suspend fun getAverageRmssd(startMs: Long, endMs: Long): Float?

    @Query("DELETE FROM hrv_samples WHERE timestamp < :beforeMs")
    suspend fun purgeOlderThan(beforeMs: Long)
}
