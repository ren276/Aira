package com.aira.health.domain.repository

import com.aira.health.data.local.model.NutritionLog
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for Nutrition manual and scanner-assisted mutations (D-13).
 */
interface NutritionRepository {
    fun observeNutrition(startMs: Long, endMs: Long): Flow<List<NutritionLog>>
    suspend fun getNutritionLog(id: Long): NutritionLog?
    suspend fun addNutritionLog(log: NutritionLog): Long
    suspend fun updateNutritionLog(log: NutritionLog)
    suspend fun deleteNutritionLog(id: Long)
    suspend fun getTotalCalories(startMs: Long, endMs: Long): Float
}
