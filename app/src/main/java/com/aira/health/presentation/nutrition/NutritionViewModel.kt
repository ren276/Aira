package com.aira.health.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.model.NutritionLog
import com.aira.health.domain.repository.NutritionRepository
import com.aira.health.presentation.nutrition.scanner.BarcodeScannerGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: NutritionRepository,
    private val barcodeScannerGateway: BarcodeScannerGateway
) : ViewModel() {

    private val mutableState = MutableStateFlow(NutritionUiState())

    private val dayStartMs: Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    // Keep the upper bound open so newly inserted logs are visible immediately.
    private val dayEndMs: Long = Long.MAX_VALUE

    private val nutritionFlow = repository.observeNutrition(
        startMs = dayStartMs,
        endMs = dayEndMs
    )

    val uiState: StateFlow<NutritionUiState> = combine(
        mutableState,
        nutritionFlow
    ) { state, logs ->
        val sorted = logs.sortedByDescending { it.timestamp }
        state.copy(
            history = sorted,
            totalCalories = sorted.sumOf { it.calories.toDouble() }.toFloat(),
            totalProteinG = sorted.sumOf { it.proteinG.toDouble() }.toFloat(),
            totalCarbsG = sorted.sumOf { it.carbsG.toDouble() }.toFloat(),
            totalFatG = sorted.sumOf { it.fatG.toDouble() }.toFloat(),
            isHistoryLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState()
    )

    fun onFoodNameChange(name: String) {
        mutableState.update { it.copy(quickAddFoodName = name, inputError = null) }
    }

    fun onCaloriesChange(calories: String) {
        mutableState.update { it.copy(quickAddCalories = calories, inputError = null) }
    }

    fun saveQuickAdd() {
        val state = mutableState.value
        if (state.quickAddFoodName.isBlank()) {
            mutableState.update { it.copy(inputError = "Food name cannot be empty") }
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
            mutableState.update {
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
        mutableState.update { it.copy(showDeleteConfirmationForId = id) }
    }

    fun confirmDelete() {
        val id = mutableState.value.showDeleteConfirmationForId ?: return
        viewModelScope.launch {
            repository.deleteNutritionLog(id)
            mutableState.update { it.copy(showDeleteConfirmationForId = null) }
        }
    }

    fun cancelDelete() {
        mutableState.update { it.copy(showDeleteConfirmationForId = null) }
    }

    fun onScannerDraftReceived(foodName: String, calories: Float) {
        mutableState.update {
            it.copy(
                quickAddFoodName = foodName,
                quickAddCalories = calories.toString(),
                currentLogMethod = "barcode",
                inputError = null
            )
        }
    }

    fun requestScannerDraft() {
        viewModelScope.launch {
            val result = barcodeScannerGateway.scanBarcode()
            if (result == null) {
                mutableState.update {
                    it.copy(inputError = "Scanner is unavailable in this build. Enter food manually.")
                }
                return@launch
            }

            mutableState.update {
                it.copy(
                    quickAddFoodName = result.foodName.orEmpty(),
                    quickAddCalories = result.calories?.toString().orEmpty(),
                    currentLogMethod = "barcode",
                    inputError = null
                )
            }
        }
    }
}
