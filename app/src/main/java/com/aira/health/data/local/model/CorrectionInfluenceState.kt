package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "correction_influence_state")
data class CorrectionInfluenceState(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val parameterKey: String,
    val sourceCorrectionId: Long,
    val sourceFieldName: String,
    val ageDays: Long,
    val decayWeight: Float,
    val rawInfluence: Float,
    val decayedInfluence: Float,
    val cappedInfluence: Float,
    val createdAt: Long
)
