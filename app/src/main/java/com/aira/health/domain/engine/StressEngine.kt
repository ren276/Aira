package com.aira.health.domain.engine

import kotlin.math.exp
import kotlin.math.ln

import javax.inject.Inject

/**
 * Pure Kotlin scoring engine for physiological Stress — SCORE-03.
 *
 * Two outputs per D-01:
 *  1. [computeHourlyStress] — point-in-time stress for a single hour (HR + HRV vs. baselines)
 *  2. [computeDailyStress]  — non-linear daily aggregate that amplifies high-stress spike hours
 *
 * Non-linear amplification strategy (D-01):
 *   The daily stress aggregate uses a power/log transform so hours where
 *   hourly stress > threshold contribute disproportionately more to the
 *   final score — mirroring how a single high-stress event impacts the
 *   autonomic system beyond simple averaging.
 *
 * Missing hourly windows reduce confidence but never suppress the score. (D-03, D-04, D-11, D-12)
 *
 * No Android imports — pure domain logic.
 */
class StressEngine @Inject constructor() {

    data class HourlyResult(
        /** Stress index for this hour, clamped 0..100. */
        val stressScore: Int
    )

    data class DailyResult(
        /** Aggregate daily stress score, clamped 0..100. */
        val score: Int,
        /**
         * Data confidence 0..1 — fraction of expected hours covered.
         * Emitted as a parallel trust signal; never gates score visibility.
         */
        val confidence: Float
    )

    // ── Hourly computation ────────────────────────────────────────────────────

    /**
     * Compute the stress index for a single hour.
     *
     * Formula: stress = 0.5 × hrStress + 0.5 × hrvStress
     *   hrStress  = clamp((hr − baseline) / baseline, 0, 1) × 100
     *   hrvStress = clamp((baseline − hrv) / baseline, 0, 1) × 100
     *
     * Both components are symmetric: being below HR baseline or above HRV
     * baseline registers as 0 stress, not negative stress.
     *
     * @param hrBpm            Measured heart rate this hour (bpm)
     * @param hrBaselineBpm    Personal EMA baseline resting HR (bpm)
     * @param hrvRmssd         Measured HRV RMSSD this hour (ms)
     * @param hrvBaselineRmssd Personal EMA baseline HRV (ms)
     */
    fun computeHourlyStress(
        hrBpm: Float,
        hrBaselineBpm: Float,
        hrvRmssd: Float,
        hrvBaselineRmssd: Float
    ): HourlyResult {
        // HR component: elevated HR above resting baseline → stress
        val hrDelta = ((hrBpm - hrBaselineBpm) / hrBaselineBpm.coerceAtLeast(1f))
            .coerceIn(0f, 1f)

        // HRV component: suppressed HRV below baseline → stress
        val hrvDelta = ((hrvBaselineRmssd - hrvRmssd) / hrvBaselineRmssd.coerceAtLeast(1f))
            .coerceIn(0f, 1f)

        val rawStress = (0.5f * hrDelta + 0.5f * hrvDelta) * 100f

        return HourlyResult(stressScore = rawStress.toInt().coerceIn(0, 100))
    }

    // ── Daily aggregate ───────────────────────────────────────────────────────

    /**
     * Compute the daily stress score from a list of hourly stress values.
     *
     * Non-linear amplification (D-01):
     *   Uses a weighted power mean that penalises high-stress spikes:
     *     amplifiedSum = Σ (hourScore ^ AMP_POWER)
     *     aggregate    = (amplifiedSum / N) ^ (1/AMP_POWER)
     *
     *   AMP_POWER = 2 (quadratic mean / RMS) — a single hour at 90 outweighs
     *   nine hours at 30 in the final score, unlike a simple mean (27.5 vs 36).
     *
     * @param hourlyScores       Stress scores (0..100) for each measured hour
     * @param totalExpectedHours Expected hours in the day; defaults to 24.
     *                           Used to compute coverage confidence.
     */
    fun computeDailyStress(
        hourlyScores: List<Float>,
        totalExpectedHours: Int = 24
    ): DailyResult {
        if (hourlyScores.isEmpty()) {
            return DailyResult(score = 0, confidence = 0f)
        }

        val confidence = (hourlyScores.size.toFloat() / totalExpectedHours).coerceIn(0f, 1f)

        // Quadratic mean (RMS) — amplifies high-stress hours non-linearly
        val ssq = hourlyScores.sumOf { s ->
            val clamped = s.coerceIn(0f, 100f).toDouble()
            clamped * clamped
        }
        val rmsScore = Math.sqrt(ssq / hourlyScores.size).toFloat()

        return DailyResult(
            score = rmsScore.toInt().coerceIn(0, 100),
            confidence = confidence
        )
    }
}
