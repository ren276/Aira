package com.aira.health.presentation.supplementary

import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import io.mockk.coEvery
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
class WhatIfViewModelTest {

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
    fun fallsBackToRecentDayWhenTodayHasNoMetrics() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeByDate(any()) } returns flowOf(null)
        coEvery { dao.getLast14Days() } returns listOf(
            DailyMetrics(
                date = "2026-04-15",
                recoveryScore = 78,
                sleepScore = 82,
                sleepDurationMin = 480,
                dataConfidence = 0.81f
            )
        )

        val viewModel = WhatIfViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertTrue(state.hasSufficientData)
        assertEquals(78, state.currentRecovery)
        assertEquals(8f, state.currentSleep)
        assertTrue(state.guidance.contains("most recent synced day"))
        assertFalse(state.isLoading)
    }

    @Test
    fun currentDayMetricsDriveSimulationWhenAvailable() = runTest {
        val dao = mockk<DailyMetricsDao>()
        every { dao.observeByDate(any()) } returns flowOf(
            DailyMetrics(
                date = "2026-04-16",
                recoveryScore = 84,
                sleepScore = 88,
                sleepDurationMin = 510,
                dataConfidence = 0.9f
            )
        )

        val viewModel = WhatIfViewModel(dao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertTrue(state.hasSufficientData)
        assertEquals(84, state.currentRecovery)
        assertEquals(8.5f, state.currentSleep)
        assertTrue(state.guidance.contains("today's synced physiology"))
    }
}