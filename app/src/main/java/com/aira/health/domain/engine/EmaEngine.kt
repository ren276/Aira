package com.aira.health.domain.engine

import javax.inject.Inject

/**
 * Pure Kotlin EMA (Exponential Moving Average) engine — SCORE-05, D-07, D-08.
 *
 * Implements a two-phase baseline update rule:
 *
 * Phase 1 — Cold Start (first [COLD_START_SAMPLES] samples):
 *   Uses an expanding flat average to avoid extreme EMA sensitivity on sparse data.
 *   At exactly [COLD_START_SAMPLES], [coldStartComplete] transitions to true.
 *
 * Phase 2 — EMA (samples > [COLD_START_SAMPLES]):
 *   new = α × measurement + (1 − α) × previous
 *
 * Stateless by design: caller passes previous state and receives the next state,
 * enabling deterministic multi-day replay for backfill recomputation (D-02).
 *
 * No Android imports — pure domain logic.
 */
class EmaEngine @Inject constructor() {

    companion object {
        /** Number of samples required to complete the cold-start phase (D-08). */
        const val COLD_START_SAMPLES = 7
    }

    data class UpdateResult(
        /** New baseline value after incorporating the measurement. */
        val newValue: Float,
        /** Updated sample count (incremented by 1). */
        val newSampleCount: Int,
        /** True once [COLD_START_SAMPLES] samples have been accumulated. */
        val coldStartComplete: Boolean
    )

    /**
     * Update a baseline with a new measurement.
     *
     * @param previousValue      Current baseline value (0 on first call)
     * @param previousSampleCount  Number of samples seen so far (0 on first call)
     * @param coldStartComplete  Whether the cold-start phase has completed
     * @param newMeasurement     The new observed value to incorporate
     * @param alpha              EMA smoothing factor (0 < α < 1); used only post cold-start
     */
    fun update(
        previousValue: Float,
        previousSampleCount: Int,
        coldStartComplete: Boolean,
        newMeasurement: Float,
        alpha: Float
    ): UpdateResult {
        val newCount = previousSampleCount + 1

        return if (!coldStartComplete) {
            // Cold-start: expanding flat average
            // newAvg = prevAvg + (newMeasurement - prevAvg) / newCount
            val newAvg = if (newCount == 1) {
                newMeasurement
            } else {
                previousValue + (newMeasurement - previousValue) / newCount
            }
            UpdateResult(
                newValue = newAvg,
                newSampleCount = newCount,
                coldStartComplete = newCount >= COLD_START_SAMPLES
            )
        } else {
            // EMA smoothing
            val ema = alpha * newMeasurement + (1f - alpha) * previousValue
            UpdateResult(
                newValue = ema,
                newSampleCount = newCount,
                coldStartComplete = true
            )
        }
    }
}
