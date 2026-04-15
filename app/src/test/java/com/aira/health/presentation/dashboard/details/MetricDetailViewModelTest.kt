package com.aira.health.presentation.dashboard.details

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Unit tests verifying shared metric detail invariants (D-05, D-11).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MetricDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockDao: DailyMetricsDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockDao = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid metric id emits Error state`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("metricId" to "unknown_metric"))
        val viewModel = MetricDetailViewModel(mockDao, savedStateHandle)

        viewModel.uiState.test {
            advanceUntilIdle()
            val emissions = cancelAndConsumeRemainingEvents()
            val errorState = emissions.filterIsInstance<app.cash.turbine.Event.Item<MetricDetailUiState>>()
                .map { it.value }
                .filterIsInstance<MetricDetailUiState.Error>()
                .lastOrNull()
            requireNotNull(errorState) { "Expected at least one Error emission" }
            assertTrue("State must be Error for invalid metric IDs", errorState is MetricDetailUiState.Error)
        }
    }

    @Test
    fun `valid metric id emits Success state with 3 part explanation contract`() = runTest(testDispatcher) {
        // Setup mock data
        val recentMetrics = listOf(
            DailyMetrics(date = "2025-01-01", recoveryScore = 75, dataConfidence = 0.8f)
        )
        coEvery { mockDao.getRange(any(), any()) } returns emptyList() // Fallback to trend array
        coEvery { mockDao.getLast14Days() } returns recentMetrics

        val savedStateHandle = SavedStateHandle(mapOf("metricId" to "recovery"))
        val viewModel = MetricDetailViewModel(mockDao, savedStateHandle)

        viewModel.uiState.test {
            advanceUntilIdle()
            val emissions = cancelAndConsumeRemainingEvents()
            val successState = emissions.filterIsInstance<app.cash.turbine.Event.Item<MetricDetailUiState>>()
                .map { it.value }
                .filterIsInstance<MetricDetailUiState.Success>()
                .lastOrNull()

            requireNotNull(successState) { "Expected at least one Success emission" }

            // Validate metric type mapping
            assertEquals(MetricType.RECOVERY, successState.metricType)
            assertEquals(75, successState.currentScore)

            // D-11: Validate exactly 3 parts of the explanation contract are present and non-empty
            assertTrue("whatChanged must not be empty", successState.whatChanged.isNotBlank())
            assertTrue("whyItMatters must not be empty", successState.whyItMatters.isNotBlank())
            assertTrue("whatToDoNext must not be empty", successState.whatToDoNext.isNotBlank())
        }
    }
}
