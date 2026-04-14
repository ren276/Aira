package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baselines")
data class Baseline(
    @PrimaryKey val metric: String, // UNIQUE — "hrv_rmssd"|"rhr"|"sleep_duration"|etc.
    val value: Float,
    val emaAlpha: Float,
    val sampleCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val coldStartComplete: Boolean = false
)
