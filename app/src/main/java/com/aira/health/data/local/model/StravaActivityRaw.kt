package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "strava_activities_raw",
    indices = [Index(value = ["startTime"])]
)
data class StravaActivityRaw(
    @PrimaryKey val activityId: Long,
    val startTime: Long,
    val endTime: Long,
    val sportType: String? = null,
    val distanceMeters: Float? = null,
    val movingTimeSec: Int? = null,
    val elapsedTimeSec: Int? = null,
    val steps: Int? = null,
    val averageHeartRate: Float? = null,
    val maxHeartRate: Float? = null,
    val calories: Float? = null,
    val kiloJoules: Float? = null,
    val sourcePackage: String,
    val rawJson: String,
    val syncedAt: Long = System.currentTimeMillis()
)
