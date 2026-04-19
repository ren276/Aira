package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aira.health.data.local.model.CausalInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface CausalInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(insight: CausalInsight)

    @Query("SELECT * FROM causal_insights WHERE metricKey = :metricKey ORDER BY date DESC, calculatedAt DESC LIMIT 1")
    fun observeLatestByMetric(metricKey: String): Flow<CausalInsight?>

    @Query("SELECT * FROM causal_insights WHERE metricKey = :metricKey ORDER BY date DESC, calculatedAt DESC LIMIT 1")
    suspend fun getLatestByMetric(metricKey: String): CausalInsight?

    @Query("SELECT * FROM causal_insights WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, metricKey ASC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<CausalInsight>

    @Query("SELECT * FROM causal_insights WHERE metricKey = :metricKey AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByMetricAndDateRange(metricKey: String, startDate: String, endDate: String): List<CausalInsight>
}
