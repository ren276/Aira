package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aira.health.data.local.model.ContinuitySyncState

@Dao
interface ContinuitySyncStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ContinuitySyncState)

    @Query("SELECT * FROM continuity_sync_state WHERE key = 'primary' LIMIT 1")
    suspend fun get(): ContinuitySyncState?
}
