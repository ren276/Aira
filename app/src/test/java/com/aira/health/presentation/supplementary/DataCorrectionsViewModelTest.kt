package com.aira.health.presentation.supplementary

import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.UserCorrectionDao
import com.aira.health.data.local.model.Baseline
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class DataCorrectionsViewModelTest {

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
    fun emitsCountsAndBaselinesFromLocalState() = runTest {
        val correctionsDao = mockk<UserCorrectionDao>()
        val baselineDao = mockk<BaselineDao>()
        coEvery { correctionsDao.getCountByType("sleep") } returns 2
        coEvery { correctionsDao.getCountByType("hrv") } returns 1
        coEvery { baselineDao.get("sleep_score") } returns Baseline("sleep_score", 78f, 0.2f)
        coEvery { baselineDao.get("hrv_rmssd") } returns Baseline("hrv_rmssd", 42f, 0.2f)

        val viewModel = DataCorrectionsViewModel(correctionsDao, baselineDao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertEquals(2, state.sleepCorrections)
        assertEquals(1, state.hrvCorrections)
        assertEquals("78 score", state.sleepBaselineLabel)
        assertEquals("42 ms", state.hrvBaselineLabel)
        assertTrue(state.timelineMessage.contains("3 validated correction(s)"))
    }

    @Test
    fun emptyStateStaysExplicitWhenNoCorrectionsExist() = runTest {
        val correctionsDao = mockk<UserCorrectionDao>()
        val baselineDao = mockk<BaselineDao>()
        coEvery { correctionsDao.getCountByType(any()) } returns 0
        coEvery { baselineDao.get(any()) } returns null

        val viewModel = DataCorrectionsViewModel(correctionsDao, baselineDao)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertFalse(state.timelineMessage.contains("validated correction"))
        assertEquals("not set", state.sleepBaselineLabel)
        assertEquals("not set", state.hrvBaselineLabel)
    }
}