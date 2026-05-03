package com.aira.health.domain.engine

import com.aira.health.domain.ml.MetricInferenceResult.InferenceSource
import com.aira.health.domain.ml.PhysiologicalModelProxy
import javax.inject.Inject

/**
 * HybridStrainEngine — Phase 12 ML integration.
 *
 * Strategy: ML mode when model available AND usableDays >= 14, else [StrainEngine] heuristic.
 */
class HybridStrainEngine @Inject constructor(
    private val model: PhysiologicalModelProxy,
    private val heuristic: StrainEngine
) {
    companion object { const val MIN_ML_DAYS = 14 }

    data class Result(val score: Int, val confidence: Float, val source: InferenceSource)

    fun compute(
        zone1Minutes: Float?,
        zone2Minutes: Float?,
        zone3Minutes: Float?,
        zone4Minutes: Float?,
        zone5Minutes: Float?,
        totalActiveMinutes: Float?,
        usableDays: Int
    ): Result {
        if (model.isAvailable && usableDays >= MIN_ML_DAYS) {
            val maxZoneMin = 120f   // reference max per zone
            val features = floatArrayOf(
                (zone1Minutes ?: 0f) / maxZoneMin,
                (zone2Minutes ?: 0f) / maxZoneMin,
                (zone3Minutes ?: 0f) / maxZoneMin,
                (zone4Minutes ?: 0f) / maxZoneMin,
                (zone5Minutes ?: 0f) / maxZoneMin,
                (totalActiveMinutes ?: 0f) / 120f
            )
            val r = model.infer(features)
            return Result(r.score, r.confidence, r.source)
        }

        val r = heuristic.compute(zone1Minutes, zone2Minutes, zone3Minutes, zone4Minutes, zone5Minutes, totalActiveMinutes)
        return Result(r.score, r.confidence, InferenceSource.HEURISTIC_FALLBACK)
    }
}
