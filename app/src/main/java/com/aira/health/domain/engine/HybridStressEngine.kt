package com.aira.health.domain.engine

import com.aira.health.domain.ml.MetricInferenceResult.InferenceSource
import com.aira.health.domain.ml.PhysiologicalModelProxy
import javax.inject.Inject

/**
 * HybridStressEngine — Phase 12 ML integration.
 *
 * Two execution paths:
 *  - **ML mode** (≥14 days history): 4-feature TFLite inference on normalised scalar inputs.
 *  - **Heuristic fallback**: delegates to [StressEngine.computeDailyStress] using pre-computed hourlyScores.
 *
 * Normalisation reference for ML features:
 *  - hrv: 20-120 ms RMSSD
 *  - sleepQuality: 0-100
 *  - steps: 0-25000
 *  - calorieDeficit: -1000 to +1000 kcal
 */
class HybridStressEngine @Inject constructor(
    private val model: PhysiologicalModelProxy,
    private val heuristic: StressEngine
) {
    companion object { const val MIN_ML_DAYS = 14 }

    data class Result(val score: Int, val confidence: Float, val source: InferenceSource)

    /**
     * @param hrv              Morning HRV RMSSD (ms); normalised internally.
     * @param sleepQuality     Sleep quality score 0-100; normalised internally.
     * @param steps            Daily total step count; normalised internally.
     * @param calorieDeficit   Caloric deficit in kcal (negative = surplus); normalised internally.
     * @param hourlyScores     Pre-computed per-hour stress values (0-100) for the heuristic fallback.
     * @param usableDays       Days of history available.
     */
    fun compute(
        hrv: Float?,
        sleepQuality: Float?,
        steps: Float?,
        calorieDeficit: Float?,
        hourlyScores: List<Float>,
        usableDays: Int
    ): Result {
        if (model.isAvailable && usableDays >= MIN_ML_DAYS) {
            val features = floatArrayOf(
                ((hrv ?: 0f) - 20f).coerceAtLeast(0f) / 100f,
                (sleepQuality ?: 0f) / 100f,
                (steps ?: 0f) / 25000f,
                ((calorieDeficit ?: 0f) + 1000f) / 2000f
            )
            val r = model.infer(features)
            return Result(r.score, r.confidence, r.source)
        }

        // Heuristic: use pre-computed hourly scores from the caller
        val r = heuristic.computeDailyStress(hourlyScores)
        return Result(r.score, r.confidence, InferenceSource.HEURISTIC_FALLBACK)
    }
}

