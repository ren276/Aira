package com.aira.health.domain.engine

import com.aira.health.domain.ml.MetricInferenceResult.InferenceSource
import com.aira.health.domain.ml.PhysiologicalModelProxy
import javax.inject.Inject

/**
 * HybridSleepEngine — Phase 12 ML integration.
 *
 * Strategy: ML mode when model available AND usableDays >= 14, else [SleepEngine] heuristic.
 *
 * All four inputs mirror [SleepEngine.compute]'s normalised [0,1] expectations.
 */
class HybridSleepEngine @Inject constructor(
    private val model: PhysiologicalModelProxy,
    private val heuristic: SleepEngine
) {
    companion object { const val MIN_ML_DAYS = 14 }

    data class Result(val score: Int, val confidence: Float, val source: InferenceSource)

    /**
     * @param durationNormalized    Sleep duration vs. personal EMA target (0–1)
     * @param deepSleepNormalized   Deep+REM fraction vs. ideal (0–1)
     * @param continuityNormalized  Sleep continuity / fragmentation inverse (0–1)
     * @param consistencyNormalized Timing consistency vs. typical bedtime (0–1)
     */
    fun compute(
        durationNormalized: Float?,
        deepSleepNormalized: Float?,
        continuityNormalized: Float?,
        consistencyNormalized: Float?,
        usableDays: Int
    ): Result {
        if (model.isAvailable && usableDays >= MIN_ML_DAYS) {
            val features = floatArrayOf(
                durationNormalized    ?: 0f,
                deepSleepNormalized   ?: 0f,
                continuityNormalized  ?: 0f,
                consistencyNormalized ?: 0f
            )
            val r = model.infer(features)
            return Result(r.score, r.confidence, r.source)
        }

        val r = heuristic.compute(durationNormalized, deepSleepNormalized, continuityNormalized, consistencyNormalized)
        return Result(r.score, r.confidence, InferenceSource.HEURISTIC_FALLBACK)
    }
}

