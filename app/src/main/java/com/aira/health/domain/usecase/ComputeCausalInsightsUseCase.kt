package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.CausalInsightDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.CausalInsight
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.engine.CausalRankingEngine
import com.aira.health.domain.model.CausalInsightSnapshot
import java.time.LocalDate
import javax.inject.Inject

class ComputeCausalInsightsUseCase @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    private val causalInsightDao: CausalInsightDao,
    private val causalRankingEngine: CausalRankingEngine
) {

    suspend fun computeForDate(
        date: String,
        dailyMetrics: DailyMetrics? = null
    ): List<CausalInsightSnapshot> {
        val startDate = LocalDate.parse(date).minusDays(6).toString()
        val history = dailyMetricsDao.getRange(startDate, date)
        if (history.isEmpty()) return emptyList()

        val today = dailyMetrics ?: history.lastOrNull { it.date == date } ?: history.last()

        val snapshots = listOf(
            buildSnapshot("recovery", date, today, history),
            buildSnapshot("sleep", date, today, history),
            buildSnapshot("strain", date, today, history),
            buildSnapshot("stress", date, today, history)
        ).filterNotNull()

        snapshots.forEach { snapshot ->
            causalInsightDao.upsert(
                CausalInsight.fromFactors(
                    date = snapshot.date,
                    metricKey = snapshot.metricKey,
                    confidence = snapshot.confidence,
                    factors = snapshot.factors,
                    calculatedAt = snapshot.calculatedAt
                )
            )
        }

        return snapshots
    }

    private fun buildSnapshot(
        metricKey: String,
        date: String,
        today: DailyMetrics,
        history: List<DailyMetrics>
    ): CausalInsightSnapshot? {
        val candidates = mutableListOf<CausalRankingEngine.Candidate>()

        addWindowCandidates(metricKey, "24h", history.takeLast(1), today, candidates)
        addWindowCandidates(metricKey, "72h", history.takeLast(3), today, candidates)
        addWindowCandidates(metricKey, "7d", history.takeLast(7), today, candidates)

        val ranked = causalRankingEngine.rankTopFactors(candidates)
        if (ranked.isEmpty()) return null

        return CausalInsightSnapshot(
            metricKey = metricKey,
            date = date,
            confidence = today.dataConfidence,
            factors = ranked,
            calculatedAt = System.currentTimeMillis()
        )
    }

    private fun addWindowCandidates(
        metricKey: String,
        windowLabel: String,
        rows: List<DailyMetrics>,
        today: DailyMetrics,
        sink: MutableList<CausalRankingEngine.Candidate>
    ) {
        if (rows.isEmpty()) return

        val timestamp = rows.maxOfOrNull { it.calculatedAt } ?: return

        fun add(key: String, contribution: Float?) {
            val value = contribution ?: return
            sink += CausalRankingEngine.Candidate(
                key = key,
                contribution = value,
                windowLabel = windowLabel,
                windowTimestamp = timestamp
            )
        }

        val avgSleepEfficiency = rows.mapNotNull { it.sleepEfficiency }.averageOrNull()
        val avgSleepDuration = rows.mapNotNull { it.sleepDurationMin?.toFloat() }.averageOrNull()
        val avgHrv = rows.mapNotNull { it.hrvMorning }.averageOrNull()
        val avgRecovery = rows.map { it.recoveryScore.toFloat() }.averageOrNull()
        val avgStrain = rows.map { it.strainScore.toFloat() }.averageOrNull()
        val avgStress = rows.map { it.stressScore.toFloat() }.averageOrNull()

        when (metricKey) {
            "recovery" -> {
                add("sleep_efficiency", avgSleepEfficiency?.let { it - 0.85f })
                add("hrv_morning", avgHrv?.let { (it - (today.hrvMorning ?: it)) / it.coerceAtLeast(1f) })
                add("strain_pressure", avgStrain?.let { (60f - it) / 60f })
                add("stress_load", avgStress?.let { (55f - it) / 55f })
            }
            "sleep" -> {
                add("sleep_duration", avgSleepDuration?.let { (it - 420f) / 420f })
                add("sleep_efficiency", avgSleepEfficiency?.let { it - 0.85f })
                add("stress_carryover", avgStress?.let { (50f - it) / 50f })
            }
            "strain" -> {
                add("training_load", avgStrain?.let { (it - 45f) / 45f })
                add("recovery_buffer", avgRecovery?.let { (it - 60f) / 60f })
                add("stress_drive", avgStress?.let { (it - 50f) / 50f })
            }
            "stress" -> {
                add("stress_load", avgStress?.let { (it - 45f) / 45f })
                add("sleep_debt", avgSleepDuration?.let { (420f - it) / 420f })
                add("recovery_deficit", avgRecovery?.let { (60f - it) / 60f })
            }
        }
    }

    private fun List<Float>.averageOrNull(): Float? {
        if (isEmpty()) return null
        return average().toFloat()
    }
}
