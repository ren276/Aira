package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.CausalInsightDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.dao.PersonalizationStateDao
import com.aira.health.data.local.dao.WhatIfSimulationDao
import com.aira.health.data.local.db.AiraDatabase
import com.aira.health.data.local.model.CausalInsight
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.data.local.model.PersonalizationState
import com.aira.health.data.local.model.WhatIfSimulationResult
import com.aira.health.domain.engine.BurnoutRiskProjectionEngine
import com.aira.health.domain.engine.WhatIfProjectionEngine
import com.aira.health.domain.model.PredictionScenario
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunWhatIfSimulationUseCaseTest {

    private lateinit var dailyMetricsDao: DailyMetricsDao
    private lateinit var personalizationStateDao: PersonalizationStateDao
    private lateinit var causalInsightDao: CausalInsightDao
    private lateinit var airaDatabase: AiraDatabase
    private lateinit var whatIfSimulationDao: WhatIfSimulationDao
    private lateinit var useCase: RunWhatIfSimulationUseCase

    @Before
    fun setUp() {
        dailyMetricsDao = mockk(relaxed = true)
        personalizationStateDao = mockk(relaxed = true)
        causalInsightDao = mockk(relaxed = true)
        airaDatabase = mockk(relaxed = true)
        whatIfSimulationDao = mockk(relaxed = true)

        coEvery { airaDatabase.whatIfSimulationDao() } returns whatIfSimulationDao

        useCase = RunWhatIfSimulationUseCase(
            dailyMetricsDao = dailyMetricsDao,
            personalizationStateDao = personalizationStateDao,
            causalInsightDao = causalInsightDao,
            whatIfProjectionEngine = WhatIfProjectionEngine(),
            burnoutRiskProjectionEngine = BurnoutRiskProjectionEngine(),
            airaDatabase = airaDatabase
        )
    }

    @Test
    fun `runScenario persists projection with confidence and rationale keys`() = runTest {
        coEvery { dailyMetricsDao.getLast14Days() } returns sampleMetrics()
        coEvery { personalizationStateDao.getLatest() } returns PersonalizationState(
            date = "2026-04-18",
            sleepNeedMinutes = 430f,
            recoverySpeed = 1.1f,
            stressSensitivity = 1.0f,
            usableDays = 10,
            applied = true,
            skipReason = null,
            correctionInfluenceApplied = 0f
        )
        coEvery { causalInsightDao.getByDateRange(any(), any()) } returns listOf(
            CausalInsight(
                date = "2026-04-18",
                metricKey = "sleep_score",
                confidence = 0.82f,
                factor1Key = null,
                factor1Direction = null,
                factor1Weight = null,
                factor1WindowLabel = null,
                factor1WindowTimestamp = null,
                factor2Key = null,
                factor2Direction = null,
                factor2Weight = null,
                factor2WindowLabel = null,
                factor2WindowTimestamp = null,
                factor3Key = null,
                factor3Direction = null,
                factor3Weight = null,
                factor3WindowLabel = null,
                factor3WindowTimestamp = null,
                calculatedAt = 1L
            )
        )

        val captor = slot<WhatIfSimulationResult>()
        coEvery { whatIfSimulationDao.upsert(capture(captor)) } returns 101L

        val result = useCase.runScenario(
            PredictionScenario(
                targetDate = "2026-04-19",
                sleepDeltaHours = 1.0f,
                trainingLoadDeltaPercent = 10f
            )
        )

        val persisted = captor.captured
        assertEquals(101L, result.id)
        assertEquals("2026-04-19", persisted.targetDate)
        assertEquals("2026-04-18", persisted.baselineDate)
        assertTrue(persisted.projectedRecoveryDelta in -20..20)
        assertTrue(persisted.projectedEnergyDelta in -18..18)
        assertTrue(persisted.confidenceTier.isNotBlank())
        assertTrue(persisted.rationaleSignalKeys.contains("sleep_score"))
        assertTrue(persisted.projectedBurnoutTrajectory.isNotBlank())
    }

    private fun sampleMetrics(): List<DailyMetrics> = listOf(
        DailyMetrics(date = "2026-04-18", recoveryScore = 68, strainScore = 78, stressScore = 70, energyBankScore = 50, dataConfidence = 0.85f),
        DailyMetrics(date = "2026-04-17", recoveryScore = 70, strainScore = 75, stressScore = 67, energyBankScore = 54, dataConfidence = 0.85f),
        DailyMetrics(date = "2026-04-16", recoveryScore = 72, strainScore = 73, stressScore = 65, energyBankScore = 56, dataConfidence = 0.82f),
        DailyMetrics(date = "2026-04-15", recoveryScore = 74, strainScore = 70, stressScore = 63, energyBankScore = 58, dataConfidence = 0.82f),
        DailyMetrics(date = "2026-04-14", recoveryScore = 75, strainScore = 68, stressScore = 61, energyBankScore = 60, dataConfidence = 0.8f),
        DailyMetrics(date = "2026-04-13", recoveryScore = 76, strainScore = 65, stressScore = 59, energyBankScore = 62, dataConfidence = 0.8f),
        DailyMetrics(date = "2026-04-12", recoveryScore = 78, strainScore = 63, stressScore = 56, energyBankScore = 64, dataConfidence = 0.79f)
    )
}
