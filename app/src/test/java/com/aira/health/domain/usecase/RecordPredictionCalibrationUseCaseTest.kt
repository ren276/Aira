package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.PredictionCalibrationDao
import com.aira.health.data.local.dao.WhatIfSimulationDao
import com.aira.health.data.local.db.AiraDatabase
import com.aira.health.data.local.model.PredictionCalibrationRecord
import com.aira.health.data.local.model.WhatIfSimulationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RecordPredictionCalibrationUseCaseTest {

    private lateinit var airaDatabase: AiraDatabase
    private lateinit var whatIfSimulationDao: WhatIfSimulationDao
    private lateinit var predictionCalibrationDao: PredictionCalibrationDao
    private lateinit var useCase: RecordPredictionCalibrationUseCase

    @Before
    fun setUp() {
        airaDatabase = mockk(relaxed = true)
        whatIfSimulationDao = mockk(relaxed = true)
        predictionCalibrationDao = mockk(relaxed = true)

        coEvery { airaDatabase.whatIfSimulationDao() } returns whatIfSimulationDao
        coEvery { airaDatabase.predictionCalibrationDao() } returns predictionCalibrationDao

        useCase = RecordPredictionCalibrationUseCase(airaDatabase = airaDatabase)
    }

    @Test
    fun `recordCalibration writes predicted-vs-observed errors with rolling mae`() = runTest {
        coEvery { whatIfSimulationDao.getLatestForTargetDate("2026-04-19") } returns WhatIfSimulationResult(
            id = 11,
            targetDate = "2026-04-19",
            baselineDate = "2026-04-18",
            baselineRecoveryScore = 60,
            baselineEnergyScore = 50,
            sleepDeltaHours = 1f,
            trainingLoadDeltaPercent = 10f,
            projectedRecoveryDelta = 8,
            projectedEnergyDelta = 6,
            projectedBurnoutTier = "MODERATE",
            projectedBurnoutTrajectory = "RISING",
            confidenceTier = "MEDIUM",
            confidenceScore = 0.62f,
            rationaleSignalKeys = "sleep_score|strain_score",
            simulatedAt = 100L
        )
        coEvery { predictionCalibrationDao.getRecent(any()) } returns emptyList()

        val captor = slot<PredictionCalibrationRecord>()
        coEvery { predictionCalibrationDao.upsert(capture(captor)) } returns Unit

        val record = useCase.recordCalibration(
            targetDate = "2026-04-19",
            observedRecoveryScore = 66,
            observedEnergyScore = 52
        )

        requireNotNull(record)
        assertEquals(6, record.observedRecoveryDelta)
        assertEquals(2, record.observedEnergyDelta)
        assertEquals(2, record.recoveryAbsoluteError)
        assertEquals(4, record.energyAbsoluteError)
        assertEquals(3f, record.rollingMeanAbsoluteError, 0.0001f)
        assertEquals("2026-04-19", captor.captured.targetDate)
    }

    @Test
    fun `missing prediction returns null and does not write calibration`() = runTest {
        coEvery { whatIfSimulationDao.getLatestForTargetDate("2026-04-19") } returns null

        val result = useCase.recordCalibration(
            targetDate = "2026-04-19",
            observedRecoveryScore = 65,
            observedEnergyScore = 58
        )

        assertNull(result)
        coVerify(exactly = 0) { predictionCalibrationDao.upsert(any()) }
    }
}
