package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_log")
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mealType: String = "unknown",
    val foodName: String,
    val calories: Float = 0f,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val fibreG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val hydrationMl: Float = 0f,
    val logMethod: String = "manual", // "barcode"|"camera"|"describe"|"search"|"manual"
    val confidence: Float = 1f
)
