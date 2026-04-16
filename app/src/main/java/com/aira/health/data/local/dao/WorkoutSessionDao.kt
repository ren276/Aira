package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aira.health.data.local.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE startTime >= :startMs AND startTime <= :endMs ORDER BY startTime ASC")
    fun observeRange(startMs: Long, endMs: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE startTime >= :startMs AND startTime <= :endMs ORDER BY startTime ASC")
    suspend fun getRange(startMs: Long, endMs: Long): List<WorkoutSession>
}
