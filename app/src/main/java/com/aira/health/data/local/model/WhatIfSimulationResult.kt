package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "what_if_simulation_results",
    indices = [Index(value = ["targetDate", "simulatedAt"])]
)
data class WhatIfSimulationResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetDate: String,
    val baselineDate: String,
    val baselineRecoveryScore: Int,
    val baselineEnergyScore: Int,
    val sleepDeltaHours: Float,
    val trainingLoadDeltaPercent: Float,
    val projectedRecoveryDelta: Int,
    val projectedEnergyDelta: Int,
    val projectedBurnoutTier: String,
    val projectedBurnoutTrajectory: String,
    val confidenceTier: String,
    val confidenceScore: Float,
    val rationaleSignalKeys: String,
    val simulatedAt: Long = System.currentTimeMillis()
)
