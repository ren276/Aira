package com.aira.health.domain.engine

import javax.inject.Inject

/**
 * Pure Kotlin hybrid scoring engine for Energy Bank — D-05, D-06.
 *
 * Dual-output model per D-05:
 *  - [internalBalance]  — persistent internal depletion/recharge state (0..100)
 *  - [energyBankScore]  — user-visible score derived from internal state (0..100)
 *
 * Design (D-06):
 *  - Energy Bank is explicitly NOT Recovery or Strain/Stress.
 *  - It is derived from their interaction: Recovery recharges the bank,
 *    Strain and Stress deplete it, starting from the previous day's balance.
 *  - The public score reflects the direction and magnitude of this daily balance.
 *
 * Formula:
 *   recharge  = recovery × RECOVERY_FACTOR
 *   depletion = (strain × STRAIN_FACTOR) + (stress × STRESS_FACTOR)
 *   deltaBalance = recharge − depletion   (clamped so balance stays 0..100)
 *   newBalance   = previousBalance + deltaBalance, clamped 0..100
 *   energyBankScore = newBalance (same as internal; public and internal share the 0..100 range)
 *
 * Missing inputs reduce confidence never suppress the score (D-03, D-04, D-11, D-12).
 * No Android imports — pure domain logic.
 */
class EnergyBankEngine @Inject constructor() {

    data class Result(
        /** User-visible Energy Bank score, clamped 0..100. */
        val energyBankScore: Int,
        /**
         * Internal depletion/recharge balance, 0..100.
         * Preserved across days for subsequent computations.
         */
        val internalBalance: Float,
        /**
         * Data confidence 0..1 — reflects fraction of inputs present.
         * Parallel trust signal; never suppresses score visibility.
         */
        val confidence: Float
    )

    companion object {
        // How much Recovery (0–100) contributes as recharge per day (% of scale)
        private const val RECOVERY_FACTOR = 0.40f
        // How much Strain (0–100) depletes the bank per day
        private const val STRAIN_FACTOR   = 0.25f
        // How much Stress (0–100) depletes the bank per day
        private const val STRESS_FACTOR   = 0.20f

        // Weight of each input — used for confidence computation
        private const val W_RECOVERY = 0.50f  // Recovery is the primary driver
        private const val W_STRAIN   = 0.30f
        private const val W_STRESS   = 0.20f
    }

    /**
     * Compute the Energy Bank for one day.
     *
     * @param recoveryScore          Today's recovery score 0–100; null if unavailable
     * @param strainScore            Today's strain score 0–100; null if unavailable
     * @param stressScore            Today's stress score 0–100; null if unavailable
     * @param previousInternalBalance Yesterday's internal balance 0–100 (defaults to 50 for first run)
     */
    fun compute(
        recoveryScore: Float?,
        strainScore: Float?,
        stressScore: Float?,
        previousInternalBalance: Float = 50f
    ): Result {
        val availableWeight = listOf(
            recoveryScore to W_RECOVERY,
            strainScore   to W_STRAIN,
            stressScore   to W_STRESS
        ).filter { it.first != null }.sumOf { it.second.toDouble() }.toFloat()

        if (availableWeight == 0f) {
            return Result(
                energyBankScore = 0,
                internalBalance = previousInternalBalance.coerceIn(0f, 100f),
                confidence = 0f
            )
        }

        val confidence = availableWeight.coerceIn(0f, 1f)

        // Recharge from recovery
        val recharge = (recoveryScore?.coerceIn(0f, 100f) ?: 50f) * RECOVERY_FACTOR

        // Depletion from strain + stress (treat null as neutral midpoint to avoid over-penalising)
        val depletion = ((strainScore?.coerceIn(0f, 100f) ?: 0f) * STRAIN_FACTOR) +
                        ((stressScore?.coerceIn(0f, 100f) ?: 0f) * STRESS_FACTOR)

        // Net delta: positive = recharging, negative = depleting
        val delta = recharge - depletion

        // Dampen the swing: only apply `confidence` fraction of the swing
        // so partial data days don't artificially crash/spike the balance
        val dampedDelta = delta * confidence

        val newBalance = (previousInternalBalance + dampedDelta).coerceIn(0f, 100f)

        return Result(
            energyBankScore = newBalance.toInt().coerceIn(0, 100),
            internalBalance = newBalance,
            confidence = confidence
        )
    }
}
