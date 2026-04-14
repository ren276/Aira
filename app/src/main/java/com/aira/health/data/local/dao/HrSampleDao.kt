package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.HrSample
import kotlinx.coroutines.flow.Flow

@Dao
interface HrSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(samples: List<HrSample>)

    @Query("SELECT * FROM hr_samples WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp ASC")
    suspend fun getRange(startMs: Long, endMs: Long): List<HrSample>

    @Query("SELECT * FROM hr_samples WHERE context = :context AND timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp ASC")
    suspend fun getRangeByContext(context: String, startMs: Long, endMs: Long): List<HrSample>

    @Query("DELETE FROM hr_samples WHERE timestamp < :beforeMs")
    suspend fun purgeOlderThan(beforeMs: Long)
}
