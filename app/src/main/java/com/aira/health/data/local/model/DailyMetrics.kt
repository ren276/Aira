package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_metrics")
data class DailyMetrics(
    @PrimaryKey val date: String, // "YYYY-MM-DD" format, UNIQUE
    val recoveryScore: Int = 0,
    val sleepScore: Int = 0,
    val strainScore: Int = 0,
    val stressScore: Int = 0,
    val energyBankScore: Int = 0,
    val readinessToLearnScore: Int = 0,
    val nutritionScore: Int = 0,
    val burnoutRiskIndex: Float = 0f,
    val compositeReadiness: Int = 0,
    val dataConfidence: Float = 0f,
    val hrvMorning: Float? = null,
    val rhrMorning: Float? = null,
    val sleepDurationMin: Int? = null,
    val sleepEfficiency: Float? = null,
    val totalSteps: Int? = null,
    val activeCalories: Int? = null,
    val calculatedAt: Long = System.currentTimeMillis()
)
