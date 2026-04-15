package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.DailyMetrics
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMetricsDao {
    @Query("SELECT * FROM daily_metrics WHERE date = :date")
    fun observeByDate(date: String): Flow<DailyMetrics?>

    @Query("SELECT * FROM daily_metrics ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<DailyMetrics>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metrics: DailyMetrics)

    @Query("SELECT * FROM daily_metrics WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getRange(startDate: String, endDate: String): List<DailyMetrics>

    @Query("SELECT * FROM daily_metrics ORDER BY date DESC LIMIT 14")
    suspend fun getLast14Days(): List<DailyMetrics>

    /**
     * Returns the most recent [DailyMetrics] row strictly before [date].
     * Used by score computation to read the previous day's strain/energy balance.
     */
    @Query("SELECT * FROM daily_metrics WHERE date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousDay(date: String): DailyMetrics?
}
