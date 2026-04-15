package com.aira.health.presentation.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TrainViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(TrainUiState())

    // Observe trailing 30 days of workouts
    private val workoutsFlow = repository.observeWorkouts(
        startMs = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli(),
        endMs = Instant.now().toEpochMilli()
    )

    val uiState: StateFlow<TrainUiState> = combine(
        mutableState,
        workoutsFlow
    ) { state, workouts ->
        state.copy(
            history = workouts.sortedByDescending { it.startTime },
            isHistoryLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrainUiState()
    )

    fun onExerciseChange(exercise: String) {
        mutableState.update { it.copy(quickAddExercise = exercise, inputError = null) }
    }

    fun onDurationChange(duration: String) {
        // T-04-13: Validate duration format/bounds lightly on input
        if (duration.isEmpty() || duration.all { it.isDigit() }) {
            mutableState.update { it.copy(quickAddDurationMin = duration, inputError = null) }
        }
    }

    fun saveQuickAdd() {
        val state = mutableState.value
        val duration = state.quickAddDurationMin.toIntOrNull()

        if (duration == null || duration <= 0 || duration > 600) { // arbitrary validation upper limit 10h
            mutableState.update { it.copy(inputError = "Invalid duration. Enter 1-600 minutes.") }
            return
        }
        if (state.quickAddExercise.isBlank()) {
            mutableState.update { it.copy(inputError = "Exercise name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            val now = Instant.now()
            val session = WorkoutSession(
                startTime = now.minus(duration.toLong(), ChronoUnit.MINUTES).toEpochMilli(),
                endTime = now.toEpochMilli(),
                exerciseType = state.quickAddExercise.trim(),
                durationMin = duration,
                sourcePackage = "com.aira.health.manual",
                confidence = 1.0f 
            )
            repository.addWorkout(session)
            
            // Reset fields
            mutableState.update { it.copy(quickAddDurationMin = "", quickAddExercise = "") }
        }
    }

    fun initiateDelete(id: Long) {
        mutableState.update { it.copy(showDeleteConfirmationForId = id) }
    }

    fun cancelDelete() {
        mutableState.update { it.copy(showDeleteConfirmationForId = null) }
    }

    fun confirmDelete() {
        val idToDelete = mutableState.value.showDeleteConfirmationForId ?: return
        viewModelScope.launch {
            // T-04-14: Explicit destructive confirmation
            repository.deleteWorkout(idToDelete)
            cancelDelete()
        }
    }
}
