package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hr_samples",
    indices = [Index("timestamp"), Index("context")]
)
data class HrSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val bpm: Int,
    val sourcePackage: String,
    val context: String = "unknown", // "sleep"|"rest"|"workout"|"unknown"
    val confidence: Float = 1f
)
