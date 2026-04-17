package com.aira.health.domain.repository

import com.aira.health.data.local.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for Train quick-add, deep-edit, and history mutations (D-13).
 */
interface WorkoutRepository {
    fun observeWorkouts(startMs: Long, endMs: Long): Flow<List<WorkoutSession>>
    suspend fun getWorkout(id: Long): WorkoutSession?
    suspend fun addWorkout(session: WorkoutSession): Long
    suspend fun updateWorkout(session: WorkoutSession)
    suspend fun deleteWorkout(id: Long)
}
