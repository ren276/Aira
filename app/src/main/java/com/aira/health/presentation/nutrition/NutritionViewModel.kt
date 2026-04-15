package com.aira.health.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.model.NutritionLog
import com.aira.health.domain.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    fun onFoodNameChange(name: String) {
        _uiState.update { it.copy(quickAddFoodName = name, inputError = null) }
    }

    fun onCaloriesChange(calories: String) {
        _uiState.update { it.copy(quickAddCalories = calories, inputError = null) }
    }

    fun saveQuickAdd() {
        val state = uiState.value
        if (state.quickAddFoodName.isBlank()) {
            _uiState.update { it.copy(inputError = "Food name cannot be empty") }
            return
        }

        viewModelScope.launch {
            val log = NutritionLog(
                timestamp = System.currentTimeMillis(),
                foodName = state.quickAddFoodName,
                calories = state.quickAddCalories.toFloatOrNull() ?: 0f,
                logMethod = state.currentLogMethod
            )
            repository.addNutritionLog(log)
            _uiState.update { 
                it.copy(
                    quickAddFoodName = "",
                    quickAddCalories = "",
                    currentLogMethod = "manual",
                    inputError = null
                ) 
            }
        }
    }

    fun initiateDelete(id: Long) {
        _uiState.update { it.copy(showDeleteConfirmationForId = id) }
    }

    fun confirmDelete() {
        val id = uiState.value.showDeleteConfirmationForId ?: return
        viewModelScope.launch {
            repository.deleteNutritionLog(id)
            _uiState.update { it.copy(showDeleteConfirmationForId = null) }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmationForId = null) }
    }

    fun onScannerDraftReceived(foodName: String, calories: Float) {
        _uiState.update {
            it.copy(
                quickAddFoodName = foodName,
                quickAddCalories = calories.toString(),
                currentLogMethod = "barcode",
                inputError = null
            )
        }
    }
}
