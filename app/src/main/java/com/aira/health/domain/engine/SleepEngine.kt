package com.aira.health.domain.engine

import javax.inject.Inject

/**
 * Pure Kotlin scoring engine for daily Sleep quality — SCORE-02.
 *
 * Weighting (per REQUIREMENTS SCORE-02):
 *   Duration (vs. target/EMA baseline)   → 30%
 *   Stage quality (deep + REM fraction)  → 30%
 *   Continuity (low fragmentation)       → 20%
 *   Consistency (stable sleep timing)    → 20%
 *
 * Design rules (from CONTEXT D-03, D-04, D-11, D-12):
 *   - Missing components reduce confidence but never suppress the score.
 *   - Available weights are renormalised so partial nights still produce
 *     a meaningful 0–100 output.
 *   - Confidence is always emitted as a parallel signal — not a gate.
 *
 * All inputs are pre-normalised to 0.0..1.0 (higher = better).
 * Out-of-range values are clamped before computation.
 *
 * No Android imports — pure domain logic.
 */
class SleepEngine @Inject constructor() {

    data class Result(
        /** Computed sleep score, clamped to 0..100. */
        val score: Int,
        /**
         * Data confidence in 0.0..1.0 — fraction of total weight backed by
         * real data. Low confidence signals sparse data; never hides the score.
         */
        val confidence: Float
    )

    /**
     * Compute the daily sleep score.
     *
     * @param durationNormalized      Sleep duration vs. personal EMA target (0.0 very short, 1.0 at or above target)
     * @param deepSleepNormalized     Deep + REM fraction vs. expected ratio (0.0 no restorative sleep, 1.0 ideal)
     * @param continuityNormalized    Sleep continuity — 1.0 means no interruptions, 0.0 highly fragmented
     * @param consistencyNormalized   Timing consistency vs. typical bedtime window (0.0 chaotic, 1.0 very consistent)
     */
    fun compute(
        durationNormalized: Float?,
        deepSleepNormalized: Float?,
        continuityNormalized: Float?,
        consistencyNormalized: Float?
    ): Result {
        val components = listOf(
            durationNormalized?.coerceIn(0f, 1f)    to 0.30f,
            deepSleepNormalized?.coerceIn(0f, 1f)   to 0.30f,
            continuityNormalized?.coerceIn(0f, 1f)  to 0.20f,
            consistencyNormalized?.coerceIn(0f, 1f) to 0.20f
        )

        val availableWeight = components
            .filter { it.first != null }
            .sumOf { it.second.toDouble() }
            .toFloat()

        if (availableWeight == 0f) {
            return Result(score = 0, confidence = 0f)
        }

        val weightedSum = components
            .filter { it.first != null }
            .sumOf { (value, weight) -> (value!! * weight).toDouble() }
            .toFloat()

        // Renormalise: divide by available weight to maintain 0..1 range,
        // then scale to 0..100
        val rawScore = (weightedSum / availableWeight) * 100f

        return Result(
            score = rawScore.toInt().coerceIn(0, 100),
            confidence = availableWeight.coerceIn(0f, 1f)
        )
    }
}
