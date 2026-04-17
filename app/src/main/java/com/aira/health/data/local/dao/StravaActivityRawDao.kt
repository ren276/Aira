package com.aira.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aira.health.data.local.model.StravaActivityRaw

@Dao
interface StravaActivityRawDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: StravaActivityRaw)

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN steps IS NULL THEN 0 ELSE steps END), 0) " +
            "FROM strava_activities_raw WHERE startTime >= :startMs AND startTime <= :endMs"
    )
    suspend fun sumStepsInRange(startMs: Long, endMs: Long): Long

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN distanceMeters IS NULL THEN 0 ELSE distanceMeters END), 0) " +
            "FROM strava_activities_raw WHERE startTime >= :startMs AND startTime <= :endMs"
    )
    suspend fun sumDistanceMetersInRange(startMs: Long, endMs: Long): Float
}
