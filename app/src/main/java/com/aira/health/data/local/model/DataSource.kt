package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_sources")
data class DataSource(
    @PrimaryKey val packageName: String, // UNIQUE
    val displayName: String,
    val deviceType: String,
    val confidenceWeight: Float,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val active: Boolean = true
)
