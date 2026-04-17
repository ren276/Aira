package com.aira.health.data.repository

import com.aira.health.data.local.dao.NutritionLogDao
import com.aira.health.data.local.model.NutritionLog
import com.aira.health.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NutritionRepositoryImpl @Inject constructor(
    private val dao: NutritionLogDao
) : NutritionRepository {

    override fun observeNutrition(startMs: Long, endMs: Long): Flow<List<NutritionLog>> {
        return dao.observeRange(startMs, endMs)
    }

    override suspend fun getNutritionLog(id: Long): NutritionLog? {
        return dao.getById(id)
    }

    override suspend fun addNutritionLog(log: NutritionLog): Long {
        return dao.insert(log)
    }

    override suspend fun updateNutritionLog(log: NutritionLog) {
        dao.update(log)
    }

    override suspend fun deleteNutritionLog(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun getTotalCalories(startMs: Long, endMs: Long): Float {
        return dao.getTotalCalories(startMs, endMs) ?: 0f
    }
}
