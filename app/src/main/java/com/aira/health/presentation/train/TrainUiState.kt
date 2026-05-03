package com.aira.health.presentation.train

import com.aira.health.data.local.model.WorkoutSession

/**
 * UI State describing the Train screen's form inputs and historical context.
 * Focuses on quick-add parameters at the top level with an optional deep-edit navigation trigger.
 */
data class TrainUiState(
    // Quick Add Inputs
    val quickAddExercise: String = "",
    val quickAddDurationMin: String = "",
    
    // Validation
    val inputError: String? = null,
    
    // History and Biometrics
    val history: List<WorkoutSession> = emptyList(),
    val isHistoryLoading: Boolean = true,
    val vo2Max: Double? = null,
    
    // Modals/Dialogs
    val showDeleteConfirmationForId: Long? = null
)
