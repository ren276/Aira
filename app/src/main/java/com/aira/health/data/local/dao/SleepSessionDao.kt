package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.SleepSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_sessions WHERE date = :date ORDER BY startTime DESC LIMIT 1")
    fun observeForDate(date: String): Flow<SleepSession?>

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 30): List<SleepSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SleepSession): Long

    @Update
    suspend fun update(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getRange(startDate: String, endDate: String): List<SleepSession>
}
