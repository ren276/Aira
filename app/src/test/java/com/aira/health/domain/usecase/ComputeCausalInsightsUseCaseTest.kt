package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.CausalInsightDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.CausalInsight
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.engine.CausalRankingEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ComputeCausalInsightsUseCaseTest {

    private lateinit var dailyMetricsDao: DailyMetricsDao
    private lateinit var causalInsightDao: CausalInsightDao
    private lateinit var useCase: ComputeCausalInsightsUseCase

    @Before
    fun setUp() {
        dailyMetricsDao = mockk(relaxed = true)
        causalInsightDao = mockk(relaxed = true)
        useCase = ComputeCausalInsightsUseCase(
            dailyMetricsDao = dailyMetricsDao,
            causalInsightDao = causalInsightDao,
            causalRankingEngine = CausalRankingEngine()
        )
    }

    @Test
    fun `persists one latest row per metric date with top three factors only`() = runTest {
        val date = "2026-04-18"
        val history = buildHistory(date)
        val persisted = mutableListOf<CausalInsight>()

        coEvery { dailyMetricsDao.getRange(any(), any()) } returns history
        coEvery { causalInsightDao.upsert(any()) } answers {
            persisted += firstArg<CausalInsight>()
            Unit
        }

        useCase.computeForDate(date)

        assertEquals(4, persisted.size)
        persisted.forEach { row ->
            val count = row.toFactors().size
            assertEquals(3, count)
            assertNotNull(row.factor1Key)
            assertNotNull(row.factor2Key)
            assertNotNull(row.factor3Key)
        }
    }

    @Test
    fun `persisted factors include source window labels and timestamps`() = runTest {
        val date = "2026-04-18"
        val history = buildHistory(date)
        val persisted = mutableListOf<CausalInsight>()

        coEvery { dailyMetricsDao.getRange(any(), any()) } returns history
        coEvery { causalInsightDao.upsert(any()) } answers {
            persisted += firstArg<CausalInsight>()
            Unit
        }

        useCase.computeForDate(date)

        persisted.flatMap { it.toFactors() }.forEach { factor ->
            assertTrue(factor.windowLabel in setOf("24h", "72h", "7d"))
            assertTrue(factor.windowTimestamp > 0L)
        }
    }

    @Test
    fun `does not persist static template fallback factors when evidence exists`() = runTest {
        val date = "2026-04-18"
        val history = buildHistory(date)

        coEvery { dailyMetricsDao.getRange(any(), any()) } returns history

        useCase.computeForDate(date)

        coVerify(exactly = 4) { causalInsightDao.upsert(withArg { row ->
            val keys = row.toFactors().map { it.key }
            assertFalse(keys.any { it.contains("template", ignoreCase = true) })
            assertTrue(keys.isNotEmpty())
        }) }
    }

    private fun buildHistory(lastDate: String): List<DailyMetrics> {
        val end = LocalDate.parse(lastDate)
        return (0..6).map { index ->
            val date = end.minusDays((6 - index).toLong())
            DailyMetrics(
                date = date.toString(),
                recoveryScore = 60 + index,
                sleepScore = 55 + index,
                strainScore = 45 + index,
                stressScore = 40 + index,
                dataConfidence = 0.82f,
                sleepDurationMin = 390 + (index * 5),
                sleepEfficiency = 0.76f + (index * 0.02f),
                hrvMorning = 45f + index,
                calculatedAt = 1_700_000_000_000L + (index * 60_000L)
            )
        }
    }
}
