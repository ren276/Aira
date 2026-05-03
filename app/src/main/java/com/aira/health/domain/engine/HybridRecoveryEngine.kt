package com.aira.health.domain.engine

import com.aira.health.domain.ml.MetricInferenceResult
import com.aira.health.domain.ml.MetricInferenceResult.InferenceSource
import com.aira.health.domain.ml.PhysiologicalModelProxy
import javax.inject.Inject

/**
 * HybridRecoveryEngine — Phase 12 ML integration.
 *
 * Strategy:
 *  1. If [model] is available AND usableDays >= MIN_ML_DAYS → run TFLite inference.
 *  2. Otherwise → delegate to the original deterministic [RecoveryEngine].
 *
 * This guarantees that new users and devices without the .tflite asset still
 * receive a valid 0-100 score via the heuristic formula.
 *
 * @param model         Injected [PhysiologicalModelProxy] (may be a no-op stub if asset absent).
 * @param heuristic     The original [RecoveryEngine] used as fallback.
 */
class HybridRecoveryEngine @Inject constructor(
    private val model: PhysiologicalModelProxy,
    private val heuristic: RecoveryEngine
) {
    companion object {
        /** Minimum days of history required before the ML model activates. */
        const val MIN_ML_DAYS = 14
    }

    data class Result(
        val score: Int,
        val confidence: Float,
        val source: InferenceSource
    )

    /**
     * Compute recovery score.
     *
     * All normalised inputs are in [0.0, 1.0].
     * @param usableDays  Number of days with sufficient health data in the local DB.
     */
    fun compute(
        hrvNormalized: Float?,
        rhrNormalized: Float?,
        sleepScore: Float?,
        priorStrainScore: Float?,
        usableDays: Int
    ): Result {
        // ML mode: model loaded and sufficient history
        if (model.isAvailable && usableDays >= MIN_ML_DAYS) {
            val features = floatArrayOf(
                hrvNormalized  ?: 0f,
                rhrNormalized  ?: 0f,
                sleepScore?.let { it / 100f } ?: 0f,
                priorStrainScore?.let { it / 100f } ?: 0f
            )
            val mlResult = model.infer(features)
            return Result(mlResult.score, mlResult.confidence, mlResult.source)
        }

        // Heuristic fallback
        val r = heuristic.compute(hrvNormalized, rhrNormalized, sleepScore, priorStrainScore)
        return Result(r.score, r.confidence, InferenceSource.HEURISTIC_FALLBACK)
    }
}
