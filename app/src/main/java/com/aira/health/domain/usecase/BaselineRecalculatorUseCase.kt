package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.Baseline
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.engine.EmaEngine
import javax.inject.Inject

/**
 * Recomputes EMA baselines for all days from a given start date through an end date.
 *
 * Use cases:
 *  - Initial bootstrap: run from earliest available date to today
 *  - Backfill (D-02): when late data arrives for date N, call recomputeFrom(N, today)
 *    to correct all subsequent baseline values that depend on that day's data
 *
 * Metric set (D-07): maintains baselines for both raw inputs (HRV, RHR) and all
 * score outputs (recovery, sleep, strain, stress, energy bank, readiness-to-learn,
 * nutrition, burnout risk, composite readiness).
 *
 * Cold-start rule (D-08, SCORE-05): first 7 samples use expanding flat average;
 * subsequent samples use EMA smoothing.
 *
 * This use case is deliberately sequential — each day's baseline feeds the next.
 * Parallelising this loop would corrupt the EMA chain.
 *
 * No Android imports — pure domain logic.
 */
class BaselineRecalculatorUseCase @Inject constructor(
    private val baselineDao: BaselineDao,
    private val dailyMetricsDao: DailyMetricsDao,
    private val emaEngine: EmaEngine
) {

    companion object {
        /** Standard EMA alpha for all metrics (≈ 14-day effective window). */
        const val DEFAULT_ALPHA = 0.2f

        /** All metric keys tracked as baselines (D-07 — full score + input set). */
        val ALL_METRIC_KEYS = listOf(
            // Raw inputs
            "hrv_rmssd",
            "rhr",
            "sleep_duration_min",
            "sleep_efficiency",
            // Score outputs
            "recovery_score",
            "sleep_score",
            "strain_score",
            "stress_score",
            "energy_bank_score",
            "readiness_to_learn_score",
            "nutrition_score",
            "burnout_risk_index",
            "composite_readiness"
        )
    }

    /**
     * Recompute baselines sequentially from [fromDate] to [toDate] (inclusive).
     *
     * Reads existing baseline state for each metric before the window starts,
     * then applies EMA day-by-day, persisting after each day.
     *
     * @param fromDate  Start of recomputation window ("YYYY-MM-DD")
     * @param toDate    End of recomputation window ("YYYY-MM-DD")
     */
    suspend fun recomputeFrom(fromDate: String, toDate: String) {
        val days = dailyMetricsDao.getRange(fromDate, toDate)
        if (days.isEmpty()) return

        // Load current baseline state for all tracked metrics
        val states: MutableMap<String, BaselineState> = ALL_METRIC_KEYS.associateWith { metric ->
            val existing = baselineDao.get(metric)
            BaselineState(
                value = existing?.value ?: 0f,
                sampleCount = existing?.sampleCount ?: 0,
                coldStartComplete = existing?.coldStartComplete ?: false
            )
        }.toMutableMap()

        // Sequential per-day EMA update (order is critical — each day feeds the next)
        for (day in days) {
            val measurements = extractMeasurements(day)

            for ((metric, measurement) in measurements) {
                if (measurement == null) continue

                val state = states[metric] ?: continue
                val updated = emaEngine.update(
                    previousValue = state.value,
                    previousSampleCount = state.sampleCount,
                    coldStartComplete = state.coldStartComplete,
                    newMeasurement = measurement,
                    alpha = DEFAULT_ALPHA
                )

                states[metric] = BaselineState(
                    value = updated.newValue,
                    sampleCount = updated.newSampleCount,
                    coldStartComplete = updated.coldStartComplete
                )

                baselineDao.upsert(
                    Baseline(
                        metric = metric,
                        value = updated.newValue,
                        emaAlpha = DEFAULT_ALPHA,
                        sampleCount = updated.newSampleCount,
                        coldStartComplete = updated.coldStartComplete
                    )
                )
            }
        }
    }

    /** Map a [DailyMetrics] row to the measurement values for each tracked metric key. */
    private fun extractMeasurements(day: DailyMetrics): Map<String, Float?> = mapOf(
        "hrv_rmssd"              to day.hrvMorning,
        "rhr"                    to day.rhrMorning,
        "sleep_duration_min"     to day.sleepDurationMin?.toFloat(),
        "sleep_efficiency"       to day.sleepEfficiency,
        "recovery_score"         to day.recoveryScore.toFloat(),
        "sleep_score"            to day.sleepScore.toFloat(),
        "strain_score"           to day.strainScore.toFloat(),
        "stress_score"           to day.stressScore.toFloat(),
        "energy_bank_score"      to day.energyBankScore.toFloat(),
        "readiness_to_learn_score" to day.readinessToLearnScore.toFloat(),
        "nutrition_score"        to day.nutritionScore.toFloat(),
        "burnout_risk_index"     to day.burnoutRiskIndex,
        "composite_readiness"    to day.compositeReadiness.toFloat()
    )

    private data class BaselineState(
        val value: Float,
        val sampleCount: Int,
        val coldStartComplete: Boolean
    )
}
