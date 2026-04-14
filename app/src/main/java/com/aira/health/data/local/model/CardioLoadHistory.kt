package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cardio_load_history")
data class CardioLoadHistory(
    @PrimaryKey val date: String,
    val acuteLoad: Float = 0f,
    val chronicLoad: Float = 0f,
    val tsb: Float = 0f,
    val status: String = "maintaining" // "detraining"|"maintaining"|"productive"|"peaking"|"fatigued"|"overtraining"
)
