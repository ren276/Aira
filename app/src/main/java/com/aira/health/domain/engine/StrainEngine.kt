package com.aira.health.domain.engine

import kotlin.math.ln
import kotlin.math.min

import javax.inject.Inject

/**
 * Pure Kotlin scoring engine for daily Strain — SCORE-04.
 *
 * Uses zone-based non-linear (exponential) scaling per D-01:
 *   - Zone 1 = very light / recovery
 *   - Zone 2 = aerobic base
 *   - Zone 3 = aerobic threshold
 *   - Zone 4 = lactate threshold / hard
 *   - Zone 5 = VO2 max / maximal effort
 *
 * Zone weights amplify high-zone contributions exponentially so
 * Zone-4/5 heavy sessions converge toward 100 faster than linear.
 *
 * Formula:
 *   weightedLoad = Σ(zoneMinutes_i × zoneWeight_i)
 *   rawStrain    = log(1 + weightedLoad) / log(1 + MAX_LOAD) × 100
 *   score        = rawStrain clamped to 0..100
 *
 * Missing zone inputs reduce confidence but never suppress the score.
 * No Android imports — pure domain logic.
 */
class StrainEngine @Inject constructor() {

    data class Result(
        /** Computed strain score, clamped to 0..100. */
        val score: Int,
        /**
         * Data confidence in 0.0..1.0 — fraction of data available.
         * Low confidence signals sparse zone data; never hides the score.
         */
        val confidence: Float
    )

    companion object {
        // Exponential zone weights (D-01: high zones disproportionately heavy)
        private val ZONE_WEIGHTS = mapOf(
            1 to 0.5f,   // Zone 1 — negligible load
            2 to 1.0f,   // Zone 2 — aerobic base
            3 to 2.0f,   // Zone 3 — moderate strain
            4 to 4.0f,   // Zone 4 — hard effort
            5 to 8.0f    // Zone 5 — maximal, extreme physiological cost
        )

        // Reference load: 60 min in Zone 5 → this is the denominator for log normalisation
        // 60 × 8 = 480 → log(481)/log(481) = 1.0 → score = 100
        private val MAX_LOAD: Float = 60f * ZONE_WEIGHTS[5]!!  // 480
    }

    /**
     * Compute the daily strain score.
     *
     * @param zone1Minutes      Minutes in HR Zone 1 (very light); null if unavailable
     * @param zone2Minutes      Minutes in HR Zone 2 (aerobic base); null if unavailable
     * @param zone3Minutes      Minutes in HR Zone 3 (aerobic threshold); null if unavailable
     * @param zone4Minutes      Minutes in HR Zone 4 (lactate threshold); null if unavailable
     * @param zone5Minutes      Minutes in HR Zone 5 (maximal); null if unavailable
     * @param totalActiveMinutes  Total active minutes from workout record (used for confidence)
     */
    fun compute(
        zone1Minutes: Float?,
        zone2Minutes: Float?,
        zone3Minutes: Float?,
        zone4Minutes: Float?,
        zone5Minutes: Float?,
        totalActiveMinutes: Float?
    ): Result {
        val zoneInputs = listOf(
            zone1Minutes to 1,
            zone2Minutes to 2,
            zone3Minutes to 3,
            zone4Minutes to 4,
            zone5Minutes to 5
        )

        val presentZones = zoneInputs.filter { it.first != null }
        val absentZones  = zoneInputs.count { it.first == null }

        if (presentZones.isEmpty()) {
            return Result(score = 0, confidence = 0f)
        }

        // Confidence decays for each missing zone (each zone = 20% of total zones)
        val confidence = ((presentZones.size.toFloat() / zoneInputs.size)).coerceIn(0f, 1f)

        // Weighted load using available zones
        val weightedLoad = presentZones.sumOf { (minutes, zone) ->
            (minutes!!.coerceAtLeast(0f) * ZONE_WEIGHTS[zone]!!).toDouble()
        }.toFloat()

        // Logarithmic normalisation to [0, 100] — compresses extreme values naturally
        val logLoad    = ln(1f + weightedLoad)
        val logMaxLoad = ln(1f + MAX_LOAD)
        val rawScore   = (logLoad / logMaxLoad) * 100f

        return Result(
            score = rawScore.toInt().coerceIn(0, 100),
            confidence = confidence
        )
    }
}
