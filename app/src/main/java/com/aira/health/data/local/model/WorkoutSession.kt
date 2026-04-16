package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["sourcePackage", "externalId"], unique = true)]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val exerciseType: String,
    val durationMin: Int,
    val activeCalories: Int = 0,
    val avgHr: Int = 0,
    val maxHr: Int = 0,
    val strainScore: Float = 0f,
    val cardioLoadContribution: Float = 0f,
    val muscularStrainJson: String = "{}",
    val hrZoneTimeJson: String = "{}",
    val sourcePackage: String,
    val externalId: String? = null,
    val confidence: Float = 1f
)
