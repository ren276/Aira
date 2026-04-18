package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.Baseline
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.data.local.model.PredictionCalibrationRecord
import com.aira.health.domain.engine.*
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ComputeDailyScoresUseCase] — D-03, D-04, D-09, D-10, D-11, D-12.
 *
 * Covers:
 *  - All DailyMetrics score fields are populated (D-09, D-10)
 *  - Missing inputs still persist visible scores with lowered dataConfidence (D-03, D-04, D-11, D-12)
 *  - Composite outputs are computed deterministically and bounded
 */
class ComputeDailyScoresUseCaseTest {

    private lateinit var dailyMetricsDao: DailyMetricsDao
    private lateinit var baselineDao: BaselineDao
    private lateinit var computeCausalInsightsUseCase: ComputeCausalInsightsUseCase
    private lateinit var recordPredictionCalibrationUseCase: RecordPredictionCalibrationUseCase
    private lateinit var useCase: ComputeDailyScoresUseCase

    @Before
    fun setUp() {
        dailyMetricsDao = mockk(relaxed = true)
        baselineDao = mockk(relaxed = true)
        computeCausalInsightsUseCase = mockk(relaxed = true)
        recordPredictionCalibrationUseCase = mockk(relaxed = true)
        useCase = ComputeDailyScoresUseCase(
            dailyMetricsDao = dailyMetricsDao,
            baselineDao = baselineDao,
            recoveryEngine = RecoveryEngine(),
            sleepEngine = SleepEngine(),
            strainEngine = StrainEngine(),
            stressEngine = StressEngine(),
            energyBankEngine = EnergyBankEngine(),
            computeCausalInsightsUseCase = computeCausalInsightsUseCase,
            recordPredictionCalibrationUseCase = recordPredictionCalibrationUseCase
        )
        coEvery { computeCausalInsightsUseCase.computeForDate(any(), any()) } returns emptyList()
        coEvery {
            recordPredictionCalibrationUseCase.recordCalibration(any(), any(), any(), any())
        } returns null
    }

    // ── Full shape persistence (D-09, D-10) ───────────────────────────────────

    @Test
    fun `full inputs produce DailyMetrics with all score fields populated`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = 55f, rhr = 62f)
        stubPreviousDay(today, recoveryScore = 70, strainScore = 40, energyBankScore = 55)

        val captor = slot<DailyMetrics>()
        coEvery { dailyMetricsDao.upsert(capture(captor)) } returns Unit

        useCase.computeForDate(
            date = today,
            hrvMorning = 60f,
            rhrMorning = 58f,
            sleepDurationMin = 440,
            sleepEfficiency = 0.88f,
            sleepDeepFraction = 0.22f,
            hourlyStressScores = List(24) { 30f },
            zone1Min = 5f, zone2Min = 25f, zone3Min = 15f, zone4Min = 8f, zone5Min = 2f,
            totalActiveMin = 55f
        )

        val persisted = captor.captured
        assertEquals(today, persisted.date)
        // All score fields must be non-zero for a good-input day
        assertTrue("recoveryScore > 0: ${persisted.recoveryScore}", persisted.recoveryScore > 0)
        assertTrue("sleepScore > 0: ${persisted.sleepScore}", persisted.sleepScore > 0)
        assertTrue("strainScore > 0: ${persisted.strainScore}", persisted.strainScore > 0)
        assertTrue("stressScore > 0: ${persisted.stressScore}", persisted.stressScore > 0)
        assertTrue("energyBankScore > 0: ${persisted.energyBankScore}", persisted.energyBankScore > 0)
        // Composite / extra fields (D-09, D-10) must also be present
        assertTrue("compositeReadiness > 0: ${persisted.compositeReadiness}", persisted.compositeReadiness > 0)
        assertTrue("readinessToLearnScore > 0: ${persisted.readinessToLearnScore}", persisted.readinessToLearnScore > 0)
        assertTrue("burnoutRiskIndex ≥ 0: ${persisted.burnoutRiskIndex}", persisted.burnoutRiskIndex >= 0f)
        assertTrue("nutritionScore ≥ 0: ${persisted.nutritionScore}", persisted.nutritionScore >= 0)
        // Full data → confidence near 1.0
        assertTrue("dataConfidence near 1.0: ${persisted.dataConfidence}", persisted.dataConfidence >= 0.7f)
    }

    // ── Sparse data — score visibility (D-03, D-04, D-11, D-12) ─────────────

    @Test
    fun `missing HRV still produces visible recovery score with reduced confidence`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = null, rhr = 62f)
        stubPreviousDay(today)

        val captor = slot<DailyMetrics>()
        coEvery { dailyMetricsDao.upsert(capture(captor)) } returns Unit

        useCase.computeForDate(
            date = today,
            hrvMorning = null,   // HRV missing
            rhrMorning = 60f,
            sleepDurationMin = 430,
            sleepEfficiency = 0.85f,
            sleepDeepFraction = 0.20f,
            hourlyStressScores = List(16) { 25f },  // partial day
            zone1Min = null, zone2Min = 20f, zone3Min = 10f, zone4Min = null, zone5Min = null,
            totalActiveMin = 30f
        )

        val persisted = captor.captured
        // Score must be present (not 0 from suppression)
        assertTrue("Recovery must be visible even without HRV: ${persisted.recoveryScore}", persisted.recoveryScore > 0)
        // Confidence must be less than 1.0
        assertTrue("Confidence reduced without HRV: ${persisted.dataConfidence}", persisted.dataConfidence < 1.0f)
        assertTrue("Confidence > 0", persisted.dataConfidence > 0f)
    }

    @Test
    fun `all inputs missing still persists a row with zero scores and zero confidence`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = null, rhr = null)
        stubPreviousDay(today)

        val captor = slot<DailyMetrics>()
        coEvery { dailyMetricsDao.upsert(capture(captor)) } returns Unit

        useCase.computeForDate(
            date = today,
            hrvMorning = null, rhrMorning = null,
            sleepDurationMin = null, sleepEfficiency = null, sleepDeepFraction = null,
            hourlyStressScores = emptyList(),
            zone1Min = null, zone2Min = null, zone3Min = null, zone4Min = null, zone5Min = null,
            totalActiveMin = null
        )

        val persisted = captor.captured
        // Row must still be persisted
        assertNotNull(persisted)
        assertEquals(today, persisted.date)
        assertTrue(
            "Confidence should remain low on fully sparse day: ${persisted.dataConfidence}",
            persisted.dataConfidence in 0f..0.35f
        )
    }

    // ── Composite outputs bounded (D-09, D-10) ────────────────────────────────

    @Test
    fun `composite readiness is bounded 0 to 100`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = 55f, rhr = 62f)
        stubPreviousDay(today)

        val captor = slot<DailyMetrics>()
        coEvery { dailyMetricsDao.upsert(capture(captor)) } returns Unit

        useCase.computeForDate(
            date = today, hrvMorning = 70f, rhrMorning = 55f,
            sleepDurationMin = 480, sleepEfficiency = 0.92f, sleepDeepFraction = 0.25f,
            hourlyStressScores = List(24) { 10f },
            zone1Min = 0f, zone2Min = 10f, zone3Min = 5f, zone4Min = 0f, zone5Min = 0f,
            totalActiveMin = 15f
        )

        val p = captor.captured
        assertTrue("compositeReadiness ≤ 100", p.compositeReadiness <= 100)
        assertTrue("compositeReadiness ≥ 0", p.compositeReadiness >= 0)
        assertTrue("readinessToLearnScore ≤ 100", p.readinessToLearnScore <= 100)
        assertTrue("burnoutRiskIndex ≤ 1.0", p.burnoutRiskIndex <= 1.0f)
        assertTrue("burnoutRiskIndex ≥ 0", p.burnoutRiskIndex >= 0f)
        assertTrue("nutritionScore ≥ 0", p.nutritionScore >= 0)
    }

    @Test
    fun `computeForDate triggers causal insight update after score persistence`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = 55f, rhr = 62f)
        stubPreviousDay(today)

        useCase.computeForDate(
            date = today,
            hrvMorning = 60f,
            rhrMorning = 58f,
            sleepDurationMin = 440,
            sleepEfficiency = 0.88f,
            sleepDeepFraction = 0.22f,
            hourlyStressScores = List(24) { 30f },
            zone1Min = 5f, zone2Min = 25f, zone3Min = 15f, zone4Min = 8f, zone5Min = 2f,
            totalActiveMin = 55f
        )

        coVerifyOrder {
            dailyMetricsDao.upsert(any())
            recordPredictionCalibrationUseCase.recordCalibration(today, any(), any(), 7)
            computeCausalInsightsUseCase.computeForDate(today, any())
        }
    }

    @Test
    fun `daily score run records calibration when a matching prediction exists`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = 55f, rhr = 62f)
        stubPreviousDay(today)

        coEvery {
            recordPredictionCalibrationUseCase.recordCalibration(today, any(), any(), 7)
        } returns PredictionCalibrationRecord(
            targetDate = today,
            predictedRecoveryDelta = 5,
            observedRecoveryDelta = 4,
            recoveryAbsoluteError = 1,
            predictedEnergyDelta = 3,
            observedEnergyDelta = 2,
            energyAbsoluteError = 1,
            rollingMeanAbsoluteError = 1f
        )

        useCase.computeForDate(
            date = today,
            hrvMorning = 60f,
            rhrMorning = 58f,
            sleepDurationMin = 440,
            sleepEfficiency = 0.88f,
            sleepDeepFraction = 0.22f,
            hourlyStressScores = List(24) { 30f },
            zone1Min = 5f, zone2Min = 25f, zone3Min = 15f, zone4Min = 8f, zone5Min = 2f,
            totalActiveMin = 55f
        )

        coVerify(exactly = 1) {
            recordPredictionCalibrationUseCase.recordCalibration(today, any(), any(), 7)
        }
    }

    @Test
    fun `missing prior prediction does not break daily score computation`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = 55f, rhr = 62f)
        stubPreviousDay(today)
        coEvery {
            recordPredictionCalibrationUseCase.recordCalibration(today, any(), any(), 7)
        } returns null

        useCase.computeForDate(
            date = today,
            hrvMorning = 60f,
            rhrMorning = 58f,
            sleepDurationMin = 440,
            sleepEfficiency = 0.88f,
            sleepDeepFraction = 0.22f,
            hourlyStressScores = List(24) { 30f },
            zone1Min = 5f, zone2Min = 25f, zone3Min = 15f, zone4Min = 8f, zone5Min = 2f,
            totalActiveMin = 55f
        )

        coVerify(exactly = 1) { dailyMetricsDao.upsert(any()) }
        coVerify(exactly = 1) {
            recordPredictionCalibrationUseCase.recordCalibration(today, any(), any(), 7)
        }
    }

    @Test
    fun `causal update failure is contained and does not leak exception`() = runTest {
        val today = "2026-04-15"
        stubBaselines(hrv = 55f, rhr = 62f)
        stubPreviousDay(today)
        coEvery {
            computeCausalInsightsUseCase.computeForDate(any(), any())
        } throws IllegalStateException("causal pipeline unavailable")

        useCase.computeForDate(
            date = today,
            hrvMorning = 60f,
            rhrMorning = 58f,
            sleepDurationMin = 440,
            sleepEfficiency = 0.88f,
            sleepDeepFraction = 0.22f,
            hourlyStressScores = List(24) { 30f },
            zone1Min = 5f, zone2Min = 25f, zone3Min = 15f, zone4Min = 8f, zone5Min = 2f,
            totalActiveMin = 55f
        )

        coVerify(exactly = 1) { dailyMetricsDao.upsert(any()) }
        assertTrue(useCase.lastCausalFailureMessage?.contains("causal pipeline") == true)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun stubBaselines(hrv: Float?, rhr: Float?) {
        io.mockk.coEvery { baselineDao.get(any<String>()) } returns null
        io.mockk.coEvery { baselineDao.get("hrv_rmssd") } returns hrv?.let { Baseline("hrv_rmssd", it, 0.2f, 10, coldStartComplete = true) }
        io.mockk.coEvery { baselineDao.get("rhr") } returns rhr?.let { Baseline("rhr", it, 0.2f, 10, coldStartComplete = true) }
        io.mockk.coEvery { baselineDao.get("sleep_score") } returns Baseline("sleep_score", 70f, 0.2f, 10, coldStartComplete = true)
    }

    private suspend fun stubPreviousDay(
        date: String,
        recoveryScore: Int = 60,
        strainScore: Int = 35,
        energyBankScore: Int = 55
    ) {
        val prev = DailyMetrics(
            date = java.time.LocalDate.parse(date).minusDays(1).toString(),
            recoveryScore = recoveryScore,
            strainScore = strainScore,
            energyBankScore = energyBankScore
        )
        coEvery { dailyMetricsDao.getPreviousDay(date) } returns prev
    }
}
