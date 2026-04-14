package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_corrections")
data class UserCorrection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordType: String,
    val recordDate: String,
    val fieldName: String,
    val originalValue: Float,
    val correctedValue: Float,
    val confidenceDelta: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
