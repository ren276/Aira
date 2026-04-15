package com.aira.health.data.repository

import com.aira.health.data.local.dao.WorkoutSessionDao
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutSessionDao
) : WorkoutRepository {

    override fun observeWorkouts(startMs: Long, endMs: Long): Flow<List<WorkoutSession>> {
        return dao.observeRange(startMs, endMs)
    }

    override suspend fun getWorkout(id: Long): WorkoutSession? {
        return dao.getById(id)
    }

    override suspend fun addWorkout(session: WorkoutSession): Long {
        return dao.insert(session)
    }

    override suspend fun updateWorkout(session: WorkoutSession) {
        dao.update(session)
    }

    override suspend fun deleteWorkout(id: Long) {
        dao.deleteById(id)
    }
}
