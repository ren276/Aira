package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.CausalInsightDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.dao.PersonalizationStateDao
import com.aira.health.data.local.db.AiraDatabase
import com.aira.health.data.local.model.WhatIfSimulationResult
import com.aira.health.domain.engine.BurnoutRiskProjectionEngine
import com.aira.health.domain.engine.WhatIfProjectionEngine
import com.aira.health.domain.model.PredictionScenario
import java.time.LocalDate
import javax.inject.Inject

class RunWhatIfSimulationUseCase @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    private val personalizationStateDao: PersonalizationStateDao,
    private val causalInsightDao: CausalInsightDao,
    private val whatIfProjectionEngine: WhatIfProjectionEngine,
    private val burnoutRiskProjectionEngine: BurnoutRiskProjectionEngine,
    private val airaDatabase: AiraDatabase
) {

    suspend fun runScenario(scenario: PredictionScenario): WhatIfSimulationResult {
        val recentMetrics = dailyMetricsDao.getLast14Days()
        require(recentMetrics.isNotEmpty()) { "Cannot run simulation without recent daily metrics" }

        val baseline = recentMetrics.first()
        val personalization = personalizationStateDao.getLatest()

        val startDate = LocalDate.parse(baseline.date).minusDays(6).toString()
        val causalKeys = causalInsightDao.getByDateRange(startDate = startDate, endDate = baseline.date)
            .map { it.metricKey }

        val rationaleKeys = (causalKeys + listOf("recovery_score", "energy_bank_score", "strain_score"))
            .distinct()
            .take(3)

        val projection = whatIfProjectionEngine.project(
            scenario = scenario,
            baselineRecovery = baseline.recoveryScore,
            baselineEnergy = baseline.energyBankScore,
            dataConfidence = baseline.dataConfidence,
            recoverySpeed = personalization?.recoverySpeed ?: 1f,
            rationaleSignalKeys = rationaleKeys
        )

        val burnout = burnoutRiskProjectionEngine.projectRisk(recentMetrics)

        val result = WhatIfSimulationResult(
            targetDate = scenario.targetDate,
            baselineDate = baseline.date,
            baselineRecoveryScore = baseline.recoveryScore,
            baselineEnergyScore = baseline.energyBankScore,
            sleepDeltaHours = scenario.sleepDeltaHours,
            trainingLoadDeltaPercent = scenario.trainingLoadDeltaPercent,
            projectedRecoveryDelta = projection.projectedRecoveryDelta,
            projectedEnergyDelta = projection.projectedEnergyDelta,
            projectedBurnoutTier = burnout.tier.name,
            projectedBurnoutTrajectory = burnout.trajectory.name,
            confidenceTier = projection.confidenceTier.name,
            confidenceScore = projection.confidenceScore,
            rationaleSignalKeys = projection.rationaleSignalKeys.joinToString(separator = "|")
        )

        val id = airaDatabase.whatIfSimulationDao().upsert(result)
        return if (id > 0L) result.copy(id = id) else result
    }
}
