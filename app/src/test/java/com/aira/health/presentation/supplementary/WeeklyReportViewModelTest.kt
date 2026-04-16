package com.aira.health.presentation.supplementary

import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyReportViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun summarizesRecentMetricsIntoWeeklyTrends() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeRecent(7) } returns flowOf(
            listOf(
                metric("2026-04-01", recovery = 80, hrv = 40f, strain = 10),
                metric("2026-04-02", recovery = 82, hrv = 50f, strain = 12),
                metric("2026-04-03", recovery = 90, hrv = 60f, strain = 20),
                metric("2026-04-04", recovery = 88, hrv = 70f, strain = 20)
            )
        )

        val viewModel = WeeklyReportViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertFalse(state.isLoading)
        assertEquals("Apr 01 - Apr 04", state.dateRange)
        assertEquals("Strong recovery baseline this week.", state.headline)
        assertEquals(55, state.avgHrv)
        assertEquals("+44% vs prior window", state.hrvTrend)
        assertEquals("+81% vs prior window", state.strainTrend)
        assertTrue(state.totalStrain > 50f)
    }

    @Test
    fun emptyWeekShowsExplicitNoDataHeadline() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeRecent(7) } returns flowOf(emptyList())

        val viewModel = WeeklyReportViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertFalse(state.isLoading)
        assertEquals("No data for this week.", state.headline)
    }

    private fun metric(
        date: String,
        recovery: Int,
        hrv: Float,
        strain: Int
    ) = DailyMetrics(
        date = date,
        recoveryScore = recovery,
        sleepScore = 70,
        strainScore = strain,
        stressScore = 35,
        hrvMorning = hrv,
        dataConfidence = 0.8f
    )
}