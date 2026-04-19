package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aira.health.data.local.model.CorrectionInfluenceState

@Dao
interface CorrectionInfluenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(states: List<CorrectionInfluenceState>)

    @Query("SELECT * FROM correction_influence_state WHERE date = :date")
    suspend fun getByDate(date: String): List<CorrectionInfluenceState>
}
