package com.aira.health.presentation.nutrition

data class NutritionUiState(
    val quickAddFoodName: String = "",
    val quickAddCalories: String = "",
    val currentLogMethod: String = "manual",
    val inputError: String? = null,
    val showDeleteConfirmationForId: Long? = null
)
