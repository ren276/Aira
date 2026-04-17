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
class NutritionEditViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(NutritionEditUiState())
    val uiState: StateFlow<NutritionEditUiState> = mutableState.asStateFlow()

    private var loadedLog: NutritionLog? = null
    private var loadedEntryId: Long? = null

    fun loadEntry(entryId: Long) {
        if (loadedEntryId == entryId) return
        loadedEntryId = entryId

        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, inputError = null) }
            val entry = nutritionRepository.getNutritionLog(entryId)
            loadedLog = entry

            if (entry == null) {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        notFound = true
                    )
                }
                return@launch
            }

            mutableState.update {
                it.copy(
                    isLoading = false,
                    notFound = false,
                    foodName = entry.foodName,
                    calories = entry.calories.toString(),
                    proteinG = entry.proteinG.toString(),
                    carbsG = entry.carbsG.toString(),
                    fatG = entry.fatG.toString(),
                    inputError = null
                )
            }
        }
    }

    fun onFoodNameChange(value: String) {
        mutableState.update { it.copy(foodName = value, inputError = null) }
    }

    fun onCaloriesChange(value: String) {
        if (value.isFloatLikeInput()) {
            mutableState.update { it.copy(calories = value, inputError = null) }
        }
    }

    fun onProteinChange(value: String) {
        if (value.isFloatLikeInput()) {
            mutableState.update { it.copy(proteinG = value, inputError = null) }
        }
    }

    fun onCarbsChange(value: String) {
        if (value.isFloatLikeInput()) {
            mutableState.update { it.copy(carbsG = value, inputError = null) }
        }
    }

    fun onFatChange(value: String) {
        if (value.isFloatLikeInput()) {
            mutableState.update { it.copy(fatG = value, inputError = null) }
        }
    }

    fun saveChanges() {
        val current = loadedLog ?: return
        val state = mutableState.value

        if (state.foodName.isBlank()) {
            mutableState.update { it.copy(inputError = "Food name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            val updated = current.copy(
                foodName = state.foodName.trim(),
                calories = state.calories.toFloatOrNull() ?: 0f,
                proteinG = state.proteinG.toFloatOrNull() ?: 0f,
                carbsG = state.carbsG.toFloatOrNull() ?: 0f,
                fatG = state.fatG.toFloatOrNull() ?: 0f
            )
            nutritionRepository.updateNutritionLog(updated)
            mutableState.update { it.copy(closeScreen = true) }
        }
    }

    fun deleteEntry() {
        val current = loadedLog ?: return
        viewModelScope.launch {
            nutritionRepository.deleteNutritionLog(current.id)
            mutableState.update { it.copy(closeScreen = true) }
        }
    }

    fun consumeCloseRequest() {
        mutableState.update { it.copy(closeScreen = false) }
    }

    private fun String.isFloatLikeInput(): Boolean {
        return isEmpty() || matches(Regex("^\\d*\\.?\\d*$"))
    }
}

data class NutritionEditUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val foodName: String = "",
    val calories: String = "",
    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
    val inputError: String? = null,
    val closeScreen: Boolean = false
)
