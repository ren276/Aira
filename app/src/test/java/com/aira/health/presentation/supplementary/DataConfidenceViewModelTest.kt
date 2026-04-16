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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataConfidenceViewModelTest {

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
    fun emitsConfidenceDerivedFromLatestDailyMetricsRow() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeRecent(7) } returns flowOf(
            listOf(
                DailyMetrics(
                    date = "2026-04-15",
                    recoveryScore = 75,
                    sleepScore = 80,
                    strainScore = 30,
                    stressScore = 40,
                    dataConfidence = 0.84f
                )
            )
        )

        val viewModel = DataConfidenceViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertEquals(84, state.percent)
        assertTrue(state.sourceSummary.contains("recent local metrics"))
        assertTrue(state.guidance.contains("high"))
    }

    @Test
    fun emptyHistoryKeepsExplicitUnavailableCopy() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeRecent(7) } returns flowOf(emptyList())

        val viewModel = DataConfidenceViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertEquals(null, state.percent)
        assertEquals("Awaiting synced data", state.sourceSummary)
        assertTrue(state.guidance.contains("Sync at least one full day"))
    }
}