package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.Baseline
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.engine.EmaEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BaselineRecalculatorUseCase] — SCORE-05, D-02, D-07.
 *
 * Covers:
 *  - 7-day cold-start behavior transitions at correct sample count
 *  - Backfill on day N recomputes baselines for all N+1..end days
 *  - Baseline metric set covers full score + input metric set (D-07)
 */
class BaselineRecalculatorUseCaseTest {

    private lateinit var baselineDao: BaselineDao
    private lateinit var dailyMetricsDao: DailyMetricsDao
    private lateinit var updatePersonalizationStateUseCase: UpdatePersonalizationStateUseCase
    private lateinit var useCase: BaselineRecalculatorUseCase

    @Before
    fun setUp() {
        baselineDao = mockk(relaxed = true)
        dailyMetricsDao = mockk(relaxed = true)
        updatePersonalizationStateUseCase = mockk(relaxed = true)
        useCase = BaselineRecalculatorUseCase(
            baselineDao = baselineDao,
            dailyMetricsDao = dailyMetricsDao,
            emaEngine = EmaEngine(),
            updatePersonalizationStateUseCase = updatePersonalizationStateUseCase
        )
    }

    // ── Cold-start behaviour ──────────────────────────────────────────────────

    @Test
    fun `cold-start processes less than 7 days without marking complete`() = runTest {
        val days = buildMetricsSeries("2026-01-01", count = 5, hrv = 55f, rhr = 62f)
        coEvery { dailyMetricsDao.getRange(any<String>(), any<String>()) } returns days
        coEvery { baselineDao.get(any<String>()) } returns null

        useCase.recomputeFrom("2026-01-01", "2026-01-05")

        val captured = mutableListOf<Baseline>()
        coVerify(atLeast = 1) { baselineDao.upsert(capture(captured)) }
        
        val lastHrvBaseline = captured.lastOrNull { it.metric == "hrv_rmssd" }
        assertNotNull(lastHrvBaseline)
        // With only 5 samples, cold start should not be complete
        assertFalse("Cold start should not be complete with 5 days", lastHrvBaseline!!.coldStartComplete)
    }

    @Test
    fun `cold-start completes after exactly 7 days`() = runTest {
        val days = buildMetricsSeries("2026-01-01", count = 7, hrv = 55f, rhr = 62f)
        coEvery { dailyMetricsDao.getRange(any<String>(), any<String>()) } returns days
        coEvery { baselineDao.get(any<String>()) } returns null

        useCase.recomputeFrom("2026-01-01", "2026-01-07")

        val captured = mutableListOf<Baseline>()
        coVerify(atLeast = 1) { baselineDao.upsert(capture(captured)) }
        val lastHrvBaseline = captured.lastOrNull { it.metric == "hrv_rmssd" }
        assertNotNull(lastHrvBaseline)
        assertTrue("Cold start complete after 7 days", lastHrvBaseline!!.coldStartComplete)
    }

    // ── Backfill recomputation (D-02) ─────────────────────────────────────────

    @Test
    fun `backfill triggers recomputation for all days from backfill date onwards`() = runTest {
        // 10 days of data; backfill is from day 3 → should process days 3–10
        val days = buildMetricsSeries("2026-01-01", count = 10, hrv = 60f, rhr = 60f)
        coEvery { dailyMetricsDao.getRange("2026-01-03", "2026-01-10") } returns days.drop(2)
        coEvery { baselineDao.get(any<String>()) } returns null

        useCase.recomputeFrom("2026-01-03", "2026-01-10")

        // 8 days × at least 2 metrics → ≥ 16 upsert calls
        coVerify(atLeast = 16) { baselineDao.upsert(any()) }
    }

    // ── Metric set coverage (D-07) ────────────────────────────────────────────

    @Test
    fun `recomputation updates both input and score baselines`() = runTest {
        val days = buildMetricsSeries("2026-01-01", count = 7, hrv = 55f, rhr = 62f)
        coEvery { dailyMetricsDao.getRange(any<String>(), any<String>()) } returns days
        coEvery { baselineDao.get(any<String>()) } returns null

        useCase.recomputeFrom("2026-01-01", "2026-01-07")

        val captured = mutableListOf<Baseline>()
        coVerify(atLeast = 1) { baselineDao.upsert(capture(captured)) }
        val updatedMetrics = captured.map { it.metric }.toSet()

        // Must include raw inputs
        assertTrue("Should update hrv_rmssd baseline", "hrv_rmssd" in updatedMetrics)
        assertTrue("Should update rhr baseline", "rhr" in updatedMetrics)
        // Must include score baselines (D-07)
        assertTrue("Should update recovery_score baseline", "recovery_score" in updatedMetrics)
        assertTrue("Should update sleep_score baseline", "sleep_score" in updatedMetrics)
    }

    @Test
    fun `daily recomputation invokes personalization update once per processed day`() = runTest {
        val days = buildMetricsSeries("2026-01-01", count = 4, hrv = 55f, rhr = 62f)
        coEvery { dailyMetricsDao.getRange(any<String>(), any<String>()) } returns days
        coEvery { baselineDao.get(any<String>()) } returns null

        useCase.recomputeFrom("2026-01-01", "2026-01-04")

        coVerify(exactly = 4) {
            updatePersonalizationStateUseCase.updateForDate(any(), any(), any())
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildMetricsSeries(
        startDate: String,
        count: Int,
        hrv: Float,
        rhr: Float
    ): List<DailyMetrics> {
        val start = java.time.LocalDate.parse(startDate)
        return (0 until count).map { i ->
            DailyMetrics(
                date = start.plusDays(i.toLong()).toString(),
                hrvMorning = hrv,
                rhrMorning = rhr,
                recoveryScore = 65,
                sleepScore = 70,
                strainScore = 40,
                stressScore = 30,
                energyBankScore = 55,
                dataConfidence = 1.0f
            )
        }
    }
}
