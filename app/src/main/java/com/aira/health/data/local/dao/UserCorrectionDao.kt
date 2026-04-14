package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.UserCorrection

@Dao
interface UserCorrectionDao {
    @Insert
    suspend fun insert(correction: UserCorrection): Long

    @Query("SELECT * FROM user_corrections WHERE synced = 0")
    suspend fun getUnsynced(): List<UserCorrection>

    @Query("SELECT COUNT(*) FROM user_corrections WHERE recordType = :type")
    suspend fun getCountByType(type: String): Int

    @Query("UPDATE user_corrections SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
