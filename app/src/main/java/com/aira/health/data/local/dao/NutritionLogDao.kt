package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.NutritionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionLogDao {
    @Query("SELECT * FROM nutrition_log WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp ASC")
    fun observeRange(startMs: Long, endMs: Long): Flow<List<NutritionLog>>

    @Insert
    suspend fun insert(log: NutritionLog): Long

    @Query("SELECT SUM(calories) FROM nutrition_log WHERE timestamp BETWEEN :startMs AND :endMs")
    suspend fun getTotalCalories(startMs: Long, endMs: Long): Float?
}
