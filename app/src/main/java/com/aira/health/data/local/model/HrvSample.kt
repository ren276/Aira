package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hrv_samples",
    indices = [Index("timestamp")]
)
data class HrvSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rmssd: Float,
    val sourcePackage: String,
    val confidence: Float = 1f
)
