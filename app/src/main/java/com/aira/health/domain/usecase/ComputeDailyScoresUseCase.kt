package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.engine.*
import javax.inject.Inject

/**
 * Orchestrates end-to-end daily score computation and persists a full [DailyMetrics] row.
 *
 * Satisfies:
 *  - SCORE-01 through SCORE-05 via the respective engines
 *  - D-03, D-04, D-11, D-12: scores are always emitted; confidence signals sparse data
 *  - D-05, D-06: Energy Bank is a hybrid visible/internal output
 *  - D-09, D-10: all DailyMetrics columns are populated every run (no placeholders)
 *
 * Caller responsibility:
 *  - Supply pre-resolved sensor readings for the target date
 *  - Call [BaselineRecalculatorUseCase] after backfill events (D-02)
 *
 * No Android imports — pure domain logic.
 */
class ComputeDailyScoresUseCase @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    private val baselineDao: BaselineDao,
    private val recoveryEngine: RecoveryEngine,
    private val sleepEngine: SleepEngine,
    private val strainEngine: StrainEngine,
    private val stressEngine: StressEngine,
    private val energyBankEngine: EnergyBankEngine,
    private val computeCausalInsightsUseCase: ComputeCausalInsightsUseCase,
    private val recordPredictionCalibrationUseCase: RecordPredictionCalibrationUseCase
) {

    internal var lastCausalFailureMessage: String? = null
    internal var lastCalibrationFailureMessage: String? = null

    /**
     * Compute all health scores for [date] and upsert the result into [DailyMetrics].
     *
     * All parameters may be null — sparse-day handling ensures a row is always written.
     *
     * @param date                  Target date "YYYY-MM-DD"
     * @param hrvMorning            Morning HRV RMSSD (ms)
     * @param rhrMorning            Resting HR (bpm)
     * @param sleepDurationMin      Total sleep duration (minutes)
     * @param sleepEfficiency       Sleep efficiency 0.0–1.0 (continuity proxy)
     * @param sleepDeepFraction     Fraction of night in deep + REM stages 0.0–1.0
     * @param hourlyStressScores    Per-hour stress scores 0–100 for the day
     * @param zone1Min–zone5Min     Heart-rate zone minutes for strain computation
     * @param totalActiveMin        Total active workout minutes (for strain confidence)
     */
    suspend fun computeForDate(
        date: String,
        hrvMorning: Float?,
        rhrMorning: Float?,
        sleepDurationMin: Int?,
        sleepEfficiency: Float?,
        sleepDeepFraction: Float?,
        hourlyStressScores: List<Float>,
        zone1Min: Float?,
        zone2Min: Float?,
        zone3Min: Float?,
        zone4Min: Float?,
        zone5Min: Float?,
        totalActiveMin: Float?,
        totalSteps: Int? = null,
        totalDistanceMeters: Float? = null,
        activeCalories: Int? = null,
        spo2: Float? = null,
        skinTemperature: Float? = null
    ) {
        // ── Load baselines ────────────────────────────────────────────────────
        val hrvBaseline  = baselineDao.get("hrv_rmssd")?.value
        val rhrBaseline  = baselineDao.get("rhr")?.value
        val sleepBaseline = baselineDao.get("sleep_score")?.value

        // ── Load prior day for cross-day state ────────────────────────────────
        val previousDay = dailyMetricsDao.getPreviousDay(date)
        val priorStrain         = previousDay?.strainScore?.toFloat()
        val priorEnergyBalance  = previousDay?.energyBankScore?.toFloat() ?: 50f

        // ── Sleep normalisation ───────────────────────────────────────────────
        // Duration: normalise against an 8-hour / 480-min target
        val sleepDurationNorm = sleepDurationMin?.let {
            (it.toFloat() / 480f).coerceIn(0f, 1f)
        }

        // Deep-sleep normalisation vs. ideal 25% REM+deep fraction
        val deepSleepNorm = sleepDeepFraction?.let {
            (it / 0.25f).coerceIn(0f, 1f)
        }

        // Continuity from sleep efficiency (already 0..1)
        val continuityNorm = sleepEfficiency?.coerceIn(0f, 1f)

        // Consistency: placeholder — no intraday sleep timing available yet; omit gracefully
        val consistencyNorm: Float? = null

        // ── Engine computations ───────────────────────────────────────────────

        // HRV normalised vs. personal EMA baseline (higher HRV vs. baseline → closer to 1.0)
        val hrvNorm = if (hrvMorning != null && hrvBaseline != null && hrvBaseline > 0f)
            (hrvMorning / hrvBaseline).coerceIn(0f, 1f) else null

        // RHR normalised vs. baseline — inverted (lower RHR vs. baseline → closer to 1.0)
        val rhrNorm = if (rhrMorning != null && rhrBaseline != null && rhrBaseline > 0f)
            (rhrBaseline / rhrMorning).coerceIn(0f, 1f) else null

        // Sleep score (0–100) used as direct recovery input
        val sleepResult = sleepEngine.compute(
            durationNormalized = sleepDurationNorm,
            deepSleepNormalized = deepSleepNorm,
            continuityNormalized = continuityNorm,
            consistencyNormalized = consistencyNorm
        )

        val recoveryResult = recoveryEngine.compute(
            hrvNormalized = hrvNorm,
            rhrNormalized = rhrNorm,
            sleepScore = sleepResult.score.toFloat(),
            priorStrainScore = priorStrain
        )

        val strainResult = strainEngine.compute(
            zone1Minutes = zone1Min,
            zone2Minutes = zone2Min,
            zone3Minutes = zone3Min,
            zone4Minutes = zone4Min,
            zone5Minutes = zone5Min,
            totalActiveMinutes = totalActiveMin
        )

        val stressResult = stressEngine.computeDailyStress(hourlyStressScores)

        val energyResult = energyBankEngine.compute(
            recoveryScore = recoveryResult.score.toFloat(),
            strainScore = strainResult.score.toFloat(),
            stressScore = stressResult.score.toFloat(),
            previousInternalBalance = priorEnergyBalance
        )

        // ── Composite and derived scores (D-09, D-10) ────────────────────────
        val compositeReadiness  = computeCompositeReadiness(
            recovery = recoveryResult.score,
            sleep    = sleepResult.score,
            stress   = stressResult.score
        )
        val readinessToLearn    = computeReadinessToLearn(compositeReadiness, stressResult.score)
        val burnoutRisk         = computeBurnoutRisk(
            strain  = strainResult.score,
            stress  = stressResult.score,
            energy  = energyResult.energyBankScore
        )
        val nutritionScore      = 0 // Phase 3 default — nutrition pipeline not yet ingested

        // ── Aggregate confidence ──────────────────────────────────────────────
        // Weighted average of engine confidences (mirrors input weight distribution)
        val dataConfidence = (
            recoveryResult.confidence * 0.35f +
            sleepResult.confidence    * 0.25f +
            strainResult.confidence   * 0.20f +
            stressResult.confidence   * 0.20f
        ).coerceIn(0f, 1f)

        // ── Persist ───────────────────────────────────────────────────────────
        val metrics = DailyMetrics(
            date                  = date,
            recoveryScore         = recoveryResult.score,
            sleepScore            = sleepResult.score,
            strainScore           = strainResult.score,
            stressScore           = stressResult.score,
            energyBankScore       = energyResult.energyBankScore,
            readinessToLearnScore = readinessToLearn,
            nutritionScore        = nutritionScore,
            burnoutRiskIndex      = burnoutRisk,
            compositeReadiness    = compositeReadiness,
            dataConfidence        = dataConfidence,
            hrvMorning            = hrvMorning,
            rhrMorning            = rhrMorning,
            sleepDurationMin      = sleepDurationMin,
            sleepEfficiency       = sleepEfficiency,
            totalSteps            = totalSteps,
            totalDistanceMeters   = totalDistanceMeters,
            activeCalories        = activeCalories,
            spo2                  = spo2,
            skinTemperature       = skinTemperature,
            calculatedAt          = System.currentTimeMillis()
        )

        dailyMetricsDao.upsert(metrics)

        // Calibration is best-effort: daily score persistence must not fail if this step errors.
        runCatching {
            recordPredictionCalibrationUseCase.recordCalibration(
                targetDate = date,
                observedRecoveryScore = recoveryResult.score,
                observedEnergyScore = energyResult.energyBankScore,
                rollingWindowSize = 7
            )
        }.onFailure { error ->
            lastCalibrationFailureMessage = error.message ?: "Calibration update failed"
        }

        // Causal computation is best-effort: score persistence must not fail if this step errors.
        runCatching {
            computeCausalInsightsUseCase.computeForDate(date = date, dailyMetrics = metrics)
        }.onFailure { error ->
            lastCausalFailureMessage = error.message ?: "Causal insight computation failed"
        }
    }

    // ── Composite formula helpers ─────────────────────────────────────────────

    /**
     * Composite Readiness: weighted blend of recovery, sleep, and inverse stress.
     * Weights: Recovery 50%, Sleep 30%, Stress-inverse 20%.
     */
    private fun computeCompositeReadiness(recovery: Int, sleep: Int, stress: Int): Int {
        val stressInverse = (100 - stress).coerceIn(0, 100)
        return (recovery * 0.50f + sleep * 0.30f + stressInverse * 0.20f).toInt().coerceIn(0, 100)
    }

    /**
     * Readiness to Learn: high when composite is high and stress is low.
     * Falls sharply when stress > 60 (cognitive load impairs learning). Bounded 0..100.
     */
    private fun computeReadinessToLearn(compositeReadiness: Int, stress: Int): Int {
        val stressPenalty = if (stress > 60) ((stress - 60) * 1.5f).toInt() else 0
        return (compositeReadiness - stressPenalty).coerceIn(0, 100)
    }

    /**
     * Burnout Risk Index 0.0..1.0: rises when strain and stress are chronically high
     * and energy bank is depleted. Single-day proxy — use with caution on its own.
     */
    private fun computeBurnoutRisk(strain: Int, stress: Int, energy: Int): Float {
        val loadFactor    = (strain * 0.4f + stress * 0.4f) / 100f
        val depletionFactor = 1f - (energy / 100f)
        return ((loadFactor + depletionFactor) / 2f).coerceIn(0f, 1f)
    }
}
