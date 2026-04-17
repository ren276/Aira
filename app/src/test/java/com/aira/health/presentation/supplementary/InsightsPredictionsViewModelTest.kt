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
class InsightsPredictionsViewModelTest {

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
    fun insufficientHistoryShowsUnavailableGuidance() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeRecent(7) } returns flowOf(emptyList())

        val viewModel = InsightsPredictionsViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertFalse(state.hasPrediction)
        assertEquals("Prediction Status", state.title)
        assertTrue(state.message.contains("Not enough trend depth yet"))
        assertTrue(state.action.contains("3 days"))
    }

    @Test
    fun sufficientHistoryBuildsForecastFromRecentData() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeRecent(7) } returns flowOf(
            listOf(
                metric("2026-04-01", recovery = 70, strain = 20),
                metric("2026-04-02", recovery = 80, strain = 30),
                metric("2026-04-03", recovery = 90, strain = 40)
            )
        )

        val viewModel = InsightsPredictionsViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertTrue(state.hasPrediction)
        assertEquals("Tomorrow Forecast: 83% readiness", state.title)
        assertTrue(state.message.contains("recovery trend (80%)"))
        assertTrue(state.message.contains("average strain (30)"))
    }

    private fun metric(
        date: String,
        recovery: Int,
        strain: Int
    ) = DailyMetrics(
        date = date,
        recoveryScore = recovery,
        sleepScore = 70,
        strainScore = strain,
        stressScore = 40,
        dataConfidence = 0.8f
    )
}