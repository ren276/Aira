package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personalization_state")
data class PersonalizationState(
    @PrimaryKey val date: String,
    val sleepNeedMinutes: Float,
    val recoverySpeed: Float,
    val stressSensitivity: Float,
    val usableDays: Int,
    val applied: Boolean,
    val skipReason: String?,
    val correctionInfluenceApplied: Float,
    val updatedAt: Long = System.currentTimeMillis()
)
