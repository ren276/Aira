package com.aira.health.presentation.dashboard.details

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aira.health.data.local.dao.CausalInsightDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.dao.HrSampleDao
import com.aira.health.data.local.dao.HrvSampleDao
import com.aira.health.data.local.dao.SleepSessionDao
import com.aira.health.data.local.dao.WorkoutSessionDao
import com.aira.health.data.local.model.CausalInsight
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.model.CausalDirection
import com.aira.health.domain.model.CausalFactor
import io.mockk.coEvery
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
    private lateinit var mockCausalInsightDao: CausalInsightDao
    private lateinit var mockHrSampleDao: HrSampleDao
    private lateinit var mockHrvSampleDao: HrvSampleDao
    private lateinit var mockSleepSessionDao: SleepSessionDao
    private lateinit var mockWorkoutSessionDao: WorkoutSessionDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockDao = mockk(relaxed = true)
        mockCausalInsightDao = mockk(relaxed = true)
        mockHrSampleDao = mockk(relaxed = true)
        mockHrvSampleDao = mockk(relaxed = true)
        mockSleepSessionDao = mockk(relaxed = true)
        mockWorkoutSessionDao = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid metric id emits Error state`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("metricId" to "unknown_metric"))
        val viewModel = MetricDetailViewModel(
            dailyMetricsDao = mockDao,
            causalInsightDao = mockCausalInsightDao,
            hrSampleDao = mockHrSampleDao,
            hrvSampleDao = mockHrvSampleDao,
            sleepSessionDao = mockSleepSessionDao,
            workoutSessionDao = mockWorkoutSessionDao,
            savedStateHandle = savedStateHandle
        )

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
        val viewModel = MetricDetailViewModel(
            dailyMetricsDao = mockDao,
            causalInsightDao = mockCausalInsightDao,
            hrSampleDao = mockHrSampleDao,
            hrvSampleDao = mockHrvSampleDao,
            sleepSessionDao = mockSleepSessionDao,
            workoutSessionDao = mockWorkoutSessionDao,
            savedStateHandle = savedStateHandle
        )

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
            assertTrue("dataSources must be present", successState.dataSources.isNotEmpty())
            assertTrue("consideredData must be present", successState.consideredData.isNotEmpty())
        }
    }

    @Test
    fun `confidence thresholds map to fixed labels`() = runTest(testDispatcher) {
        val levels = listOf(
            0.75f to "High",
            0.40f to "Medium",
            0.39f to "Low"
        )

        levels.forEach { (confidence, expectedLabel) ->
            coEvery { mockDao.getRange(any(), any()) } returns emptyList()
            coEvery { mockDao.getLast14Days() } returns listOf(
                DailyMetrics(date = "2025-01-01", recoveryScore = 76, dataConfidence = confidence)
            )
            coEvery { mockCausalInsightDao.getLatestByMetric(any()) } returns null

            val savedStateHandle = SavedStateHandle(mapOf("metricId" to "recovery"))
            val viewModel = MetricDetailViewModel(
                dailyMetricsDao = mockDao,
                causalInsightDao = mockCausalInsightDao,
                hrSampleDao = mockHrSampleDao,
                hrvSampleDao = mockHrvSampleDao,
                sleepSessionDao = mockSleepSessionDao,
                workoutSessionDao = mockWorkoutSessionDao,
                savedStateHandle = savedStateHandle
            )

            viewModel.uiState.test {
                advanceUntilIdle()
                val success = cancelAndConsumeRemainingEvents()
                    .filterIsInstance<app.cash.turbine.Event.Item<MetricDetailUiState>>()
                    .map { it.value }
                    .filterIsInstance<MetricDetailUiState.Success>()
                    .lastOrNull()

                requireNotNull(success)
                assertEquals(expectedLabel, success.confidenceTierLabel)
            }
        }
    }

    @Test
    fun `recency label is explicit window text`() = runTest(testDispatcher) {
        val factors = listOf(
            CausalFactor(
                key = "sleep_debt",
                direction = CausalDirection.DECREASED,
                weight = 0.34f,
                windowLabel = "7d",
                windowTimestamp = 1L
            )
        )
        val insight = CausalInsight.fromFactors(
            date = "2025-01-01",
            metricKey = "recovery",
            confidence = 0.8f,
            factors = factors,
            calculatedAt = 123L
        )

        coEvery { mockDao.getRange(any(), any()) } returns emptyList()
        coEvery { mockDao.getLast14Days() } returns listOf(
            DailyMetrics(date = "2025-01-01", recoveryScore = 70, dataConfidence = 0.8f)
        )
        coEvery { mockCausalInsightDao.getLatestByMetric(any()) } returns insight

        val viewModel = MetricDetailViewModel(
            dailyMetricsDao = mockDao,
            causalInsightDao = mockCausalInsightDao,
            hrSampleDao = mockHrSampleDao,
            hrvSampleDao = mockHrvSampleDao,
            sleepSessionDao = mockSleepSessionDao,
            workoutSessionDao = mockWorkoutSessionDao,
            savedStateHandle = SavedStateHandle(mapOf("metricId" to "recovery"))
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val success = cancelAndConsumeRemainingEvents()
                .filterIsInstance<app.cash.turbine.Event.Item<MetricDetailUiState>>()
                .map { it.value }
                .filterIsInstance<MetricDetailUiState.Success>()
                .lastOrNull()

            requireNotNull(success)
            assertEquals("last 7d", success.recencyWindowText)
            assertTrue(success.recencyWindowText.startsWith("last "))
        }
    }

    @Test
    fun `ranked factors expose top three with direction and weight`() = runTest(testDispatcher) {
        val factors = listOf(
            CausalFactor("sleep_debt", CausalDirection.DECREASED, 0.41f, "last 7d", 3L),
            CausalFactor("hrv_trend", CausalDirection.INCREASED, 0.32f, "last 14d", 2L),
            CausalFactor("strain_carryover", CausalDirection.NEUTRAL, 0.21f, "last 3d", 1L)
        )
        val insight = CausalInsight.fromFactors(
            date = "2025-01-01",
            metricKey = "recovery",
            confidence = 0.9f,
            factors = factors,
            calculatedAt = 456L
        )

        coEvery { mockDao.getRange(any(), any()) } returns emptyList()
        coEvery { mockDao.getLast14Days() } returns listOf(
            DailyMetrics(date = "2025-01-01", recoveryScore = 81, dataConfidence = 0.9f)
        )
        coEvery { mockCausalInsightDao.getLatestByMetric(any()) } returns insight

        val viewModel = MetricDetailViewModel(
            dailyMetricsDao = mockDao,
            causalInsightDao = mockCausalInsightDao,
            hrSampleDao = mockHrSampleDao,
            hrvSampleDao = mockHrvSampleDao,
            sleepSessionDao = mockSleepSessionDao,
            workoutSessionDao = mockWorkoutSessionDao,
            savedStateHandle = SavedStateHandle(mapOf("metricId" to "recovery"))
        )

        viewModel.uiState.test {
            advanceUntilIdle()
            val success = cancelAndConsumeRemainingEvents()
                .filterIsInstance<app.cash.turbine.Event.Item<MetricDetailUiState>>()
                .map { it.value }
                .filterIsInstance<MetricDetailUiState.Success>()
                .lastOrNull()

            requireNotNull(success)
            assertEquals(3, success.rankedFactors.size)
            assertEquals(MetricDetailUiState.FactorDirection.DECREASED, success.rankedFactors[0].direction)
            assertTrue(success.rankedFactors[0].weight > 0f)
        }
    }
}
