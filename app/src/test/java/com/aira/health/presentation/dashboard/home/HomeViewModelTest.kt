package com.aira.health.presentation.dashboard.home

import app.cash.turbine.test
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.aira.health.data.worker.HealthSyncWorker
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [HomeViewModel] covering:
 *  - Test 1: ViewModel emits cached Room-backed state immediately on subscription (D-08)
 *  - Test 2: Pull-to-refresh triggers HealthSyncWorker.scheduleImmediate without blocking UI (D-08)
 *  - Test 3: Score changes after sync produce deterministic delta payloads with confidence metadata (D-08)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel
    private lateinit var mockDao: DailyMetricsDao
    private lateinit var mockContext: Context

    // Backing flows for the mock DAO
    private val todayFlow = MutableStateFlow<DailyMetrics?>(null)
    private val recentFlow = MutableStateFlow<List<DailyMetrics>>(emptyList())

    private fun makeMetrics(
        recovery: Int = 75,
        sleep: Int = 68,
        strain: Int = 55,
        stress: Int = 42,
        confidence: Float = 0.85f
    ) = DailyMetrics(
        date           = "2025-01-01",
        recoveryScore  = recovery,
        sleepScore     = sleep,
        strainScore    = strain,
        stressScore    = stress,
        dataConfidence = confidence,
        calculatedAt   = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockDao = mockk(relaxed = true) {
            every { observeByDate(any()) } returns todayFlow
            every { observeRecent(any()) } returns recentFlow
        }
        mockContext = mockk(relaxed = true)
        mockkObject(HealthSyncWorker)
        every { HealthSyncWorker.scheduleImmediate(any()) } returns Unit

        viewModel = HomeViewModel(dailyMetricsDao = mockDao, context = mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test 1: ViewModel emits cached state immediately when today's metrics row exists.
     *
     * The ViewModel must NOT show Loading when Room already has a row — it renders immediately.
     */
    @Test
    fun `emits Success immediately when cached Room data exists`() = runTest(testDispatcher) {
        val cached = makeMetrics(recovery = 80)
        todayFlow.value = cached

        viewModel.uiState.test {
            advanceUntilIdle()
            val emissions = cancelAndConsumeRemainingEvents()
            val successState = emissions.filterIsInstance<app.cash.turbine.Event.Item<HomeUiState>>()
                .map { it.value }
                .filterIsInstance<HomeUiState.Success>()
                .firstOrNull()

            assertNotNull(successState, "Expected at least one Success emission")
            assertEquals(80, successState.recoveryScore)
            // Confidence and lastUpdated must always be present (D-08)
            assertTrue(successState.confidence >= 0f)
            assertTrue(successState.lastUpdated > 0L)
        }
    }

    /**
     * Test 2: requestRefresh triggers HealthSyncWorker.scheduleImmediate WITHOUT clearing UI.
     *
     * The isSyncing flag flips to true but the success state is retained (no Loading regression).
     */
    @Test
    fun `requestRefresh schedules immediate work without clearing existing UI data`() = runTest(testDispatcher) {
        todayFlow.value = makeMetrics()
        advanceUntilIdle()

        viewModel.requestRefresh()
        advanceUntilIdle()

        // Worker was called
        verify { HealthSyncWorker.scheduleImmediate(mockContext) }

        // Current state is still Success, not Loading — critical D-08 behaviour
        val state = viewModel.uiState.value
        assertIs<HomeUiState.Success>(state, "State must remain Success after refresh, not revert to Loading")
        assertTrue(state.isSyncing, "isSyncing must be true while sync is pending")
    }

    /**
     * Test 3: Score changes after a background sync produce ScoreDelta payloads with metadata.
     */
    @Test
    fun `score change after sync emits delta animation payloads with confidence metadata`() = runTest(testDispatcher) {
        // Initial cached state
        todayFlow.value = makeMetrics(recovery = 70, confidence = 0.80f)
        advanceUntilIdle()

        // Simulate sync result — Room emits a new row
        todayFlow.value = makeMetrics(recovery = 85, confidence = 0.92f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Success>(state)

        // Delta must be non-null because recovery changed
        val delta = state.recoveryDelta
        assertNotNull(delta, "Delta must be produced when score changes")
        assertEquals(70, delta.previous)
        assertEquals(85, delta.current)
        assertEquals(DeltaDirection.UP, delta.direction)

        // Confidence/lastUpdated always present after sync
        assertEquals(0.92f, state.confidence, 0.001f)
        assertTrue(state.lastUpdated > 0L)

        // Unchanged scores produce no delta
        assertNull(state.sleepDelta, "Unchanged sleep score must not produce a delta")
    }
}
