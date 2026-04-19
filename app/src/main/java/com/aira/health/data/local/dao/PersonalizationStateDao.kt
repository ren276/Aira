package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aira.health.data.local.model.PersonalizationState
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalizationStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PersonalizationState)

    @Query("SELECT * FROM personalization_state ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): PersonalizationState?

    @Query("SELECT * FROM personalization_state ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<PersonalizationState?>
}
