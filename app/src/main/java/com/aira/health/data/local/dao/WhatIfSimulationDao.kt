package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aira.health.data.local.model.WhatIfSimulationResult

@Dao
interface WhatIfSimulationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: WhatIfSimulationResult): Long

    @Query("SELECT * FROM what_if_simulation_results WHERE targetDate = :targetDate ORDER BY simulatedAt DESC LIMIT 1")
    suspend fun getLatestForTargetDate(targetDate: String): WhatIfSimulationResult?

    @Query("SELECT * FROM what_if_simulation_results ORDER BY simulatedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 14): List<WhatIfSimulationResult>
}
