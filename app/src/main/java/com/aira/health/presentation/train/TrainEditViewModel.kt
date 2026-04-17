package com.aira.health.presentation.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainEditViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(TrainEditUiState())
    val uiState: StateFlow<TrainEditUiState> = mutableState.asStateFlow()

    private var loadedWorkout: WorkoutSession? = null
    private var loadedWorkoutId: Long? = null

    fun loadWorkout(workoutId: Long) {
        if (loadedWorkoutId == workoutId) return
        loadedWorkoutId = workoutId

        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, inputError = null) }
            val workout = workoutRepository.getWorkout(workoutId)
            loadedWorkout = workout
            if (workout == null) {
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
                    exercise = workout.exerciseType,
                    durationMin = workout.durationMin.toString(),
                    inputError = null
                )
            }
        }
    }

    fun onExerciseChange(value: String) {
        mutableState.update { it.copy(exercise = value, inputError = null) }
    }

    fun onDurationChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            mutableState.update { it.copy(durationMin = value, inputError = null) }
        }
    }

    fun saveChanges() {
        val current = loadedWorkout ?: return
        val state = mutableState.value

        val duration = state.durationMin.toIntOrNull()
        if (state.exercise.isBlank()) {
            mutableState.update { it.copy(inputError = "Exercise name cannot be empty.") }
            return
        }
        if (duration == null || duration !in 1..600) {
            mutableState.update { it.copy(inputError = "Duration must be between 1 and 600 minutes.") }
            return
        }

        viewModelScope.launch {
            val endTime = current.endTime
            val updated = current.copy(
                exerciseType = state.exercise.trim(),
                durationMin = duration,
                startTime = endTime - duration * 60_000L
            )
            workoutRepository.updateWorkout(updated)
            mutableState.update { it.copy(closeScreen = true) }
        }
    }

    fun deleteWorkout() {
        val current = loadedWorkout ?: return
        viewModelScope.launch {
            workoutRepository.deleteWorkout(current.id)
            mutableState.update { it.copy(closeScreen = true) }
        }
    }

    fun consumeCloseRequest() {
        mutableState.update { it.copy(closeScreen = false) }
    }
}

data class TrainEditUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val exercise: String = "",
    val durationMin: String = "",
    val inputError: String? = null,
    val closeScreen: Boolean = false
)
