package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prediction_calibration",
    indices = [
        Index(value = ["targetDate"], unique = true),
        Index(value = ["recordedAt"])
    ]
)
data class PredictionCalibrationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetDate: String,
    val predictedRecoveryDelta: Int,
    val observedRecoveryDelta: Int,
    val recoveryAbsoluteError: Int,
    val predictedEnergyDelta: Int,
    val observedEnergyDelta: Int,
    val energyAbsoluteError: Int,
    val rollingMeanAbsoluteError: Float,
    val recordedAt: Long = System.currentTimeMillis()
)
