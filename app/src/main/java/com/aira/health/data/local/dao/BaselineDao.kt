package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.Baseline
import kotlinx.coroutines.flow.Flow

@Dao
interface BaselineDao {
    @Query("SELECT * FROM baselines WHERE metric = :metric")
    fun observe(metric: String): Flow<Baseline?>

    @Query("SELECT * FROM baselines WHERE metric = :metric")
    suspend fun get(metric: String): Baseline?

    @Query("SELECT * FROM baselines")
    suspend fun getAll(): List<Baseline>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(baseline: Baseline)
}
