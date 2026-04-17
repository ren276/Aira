package com.aira.health.presentation.nutrition

import com.aira.health.data.local.model.NutritionLog

data class NutritionUiState(
    val quickAddFoodName: String = "",
    val quickAddCalories: String = "",
    val currentLogMethod: String = "manual",
    val inputError: String? = null,
    val showDeleteConfirmationForId: Long? = null,
    val history: List<NutritionLog> = emptyList(),
    val totalCalories: Float = 0f,
    val totalProteinG: Float = 0f,
    val totalCarbsG: Float = 0f,
    val totalFatG: Float = 0f,
    val calorieTarget: Int = 2400,
    val isHistoryLoading: Boolean = true
)
