package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records_raw")
data class HealthRecordRaw(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordType: String,
    val sourcePackage: String,
    val deviceDisplayName: String,
    val startTime: Long,
    val endTime: Long?,
    val valueJson: String,
    val confidence: Float,
    val syncedToSupabase: Boolean = false
)
