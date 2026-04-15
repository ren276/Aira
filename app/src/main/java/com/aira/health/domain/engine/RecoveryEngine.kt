package com.aira.health.domain.engine

import javax.inject.Inject

/**
 * Pure Kotlin scoring engine for daily Recovery — SCORE-01.
 *
 * Weighting (per REQUIREMENTS SCORE-01):
 *   HRV (morning RMSSD, baseline-relative)  → 40%
 *   RHR (resting HR, baseline-relative)     → 25%
 *   Sleep score (from SleepEngine, 0–100)   → 25%
 *   Prior-day Strain (0–100, inverted)      → 10%
 *
 * Design rules (from CONTEXT D-03, D-04, D-11, D-12):
 *   - Missing inputs reduce confidence but never suppress the score.
 *   - Available weights are renormalised so partial days still produce
 *     a meaningful 0–100 output.
 *   - Output is always deterministic and clamped to 0..100.
 *
 * All inputs are pre-normalised to 0.0..1.0 (higher = better) except
 * [sleepScore] which is already 0..100, and [priorStrainScore] which is
 * 0..100 and inverted (high strain → lower recovery contribution).
 *
 * No Android imports — pure domain logic.
 */
class RecoveryEngine @Inject constructor() {

    data class Result(
        /** Computed recovery score, clamped to 0..100. */
        val score: Int,
        /**
         * Data confidence in 0.0..1.0 — reflects what fraction of the
         * total weight was backed by real data. Never gates visibility.
         */
        val confidence: Float
    )

    /**
     * Compute the daily recovery score.
     *
     * @param hrvNormalized  HRV relative to personal EMA baseline (0.0 = far below, 1.0 = at/above)
     * @param rhrNormalized  RHR relative to personal EMA baseline (0.0 = far above, 1.0 = at/below)
     * @param sleepScore     Sleep quality score 0–100 from [SleepEngine]; null if unavailable
     * @param priorStrainScore  Yesterday's strain score 0–100; null if unavailable
     */
    fun compute(
        hrvNormalized: Float?,
        rhrNormalized: Float?,
        sleepScore: Float?,
        priorStrainScore: Float?
    ): Result {
        // Component weights
        val w = mapOf(
            "hrv"    to 0.40f,
            "rhr"    to 0.25f,
            "sleep"  to 0.25f,
            "strain" to 0.10f
        )

        // Normalise prior strain: 0 strain → full contribution (1.0), 100 strain → no contribution (0.0)
        val strainNorm = priorStrainScore?.let { 1f - (it.coerceIn(0f, 100f) / 100f) }
        val sleepNorm  = sleepScore?.let { it.coerceIn(0f, 100f) / 100f }

        val components = mapOf(
            "hrv"    to (hrvNormalized?.coerceIn(0f, 1f) to w["hrv"]!!),
            "rhr"    to (rhrNormalized?.coerceIn(0f, 1f) to w["rhr"]!!),
            "sleep"  to (sleepNorm                       to w["sleep"]!!),
            "strain" to (strainNorm                      to w["strain"]!!)
        )

        val availableWeight = components.values.filter { it.first != null }.sumOf { it.second.toDouble() }.toFloat()

        if (availableWeight == 0f) {
            return Result(score = 0, confidence = 0f)
        }

        // Renormalise across available components (missing inputs excluded)
        val weightedSum = components.values
            .filter { it.first != null }
            .sumOf { (value, weight) -> (value!! * weight).toDouble() }
            .toFloat()

        // Scale to full weight: if only some components present, divide by their total weight
        // to keep output range consistent, then re-weight to full 1.0 scale
        val rawScore = (weightedSum / availableWeight) * 100f

        return Result(
            score = rawScore.toInt().coerceIn(0, 100),
            confidence = availableWeight.coerceIn(0f, 1f)
        )
    }
}
