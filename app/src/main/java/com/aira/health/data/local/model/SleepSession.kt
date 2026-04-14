package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val durationMin: Int,
    val remMin: Int = 0,
    val deepMin: Int = 0,
    val lightMin: Int = 0,
    val awakeMin: Int = 0,
    val efficiency: Float = 0f,
    val continuityScore: Float = 0f,
    val consistencyScore: Float = 0f,
    val timingScore: Float = 0f,
    val interruptions: Int = 0,
    val sleepDebt: Float = 0f,
    val sourcePackage: String,
    val confidence: Float = 1f,
    val corrected: Boolean = false
)
