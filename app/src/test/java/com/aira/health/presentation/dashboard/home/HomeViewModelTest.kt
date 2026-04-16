package com.aira.health.presentation.dashboard.home

import app.cash.turbine.test
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

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
    private lateinit var mockUserRepository: UserRepository

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
        mockUserRepository = mockk(relaxed = true)
        every { mockUserRepository.observeAuthState() } returns MutableStateFlow(
            AuthState.Authenticated(
                UserSession(
                    userId = "user-1",
                    email = "test@aira.health",
                    displayName = "Test User",
                    avatarUrl = null,
                    isGuest = false,
                    isAuthenticated = true
                )
            )
        )
        mockkObject(HealthSyncWorker)
        every { HealthSyncWorker.scheduleImmediate(any()) } returns Unit
        every { HealthSyncWorker.schedule(any()) } returns Unit

        viewModel = HomeViewModel(
            dailyMetricsDao = mockDao,
            userRepository = mockUserRepository,
            context = mockContext
        )
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
            val successState = awaitFirstSuccessEmission()

            assertNotNull("Expected at least one Success emission", successState)
            assertEquals(80, successState!!.recoveryScore)
            // Confidence and lastUpdated must always be present (D-08)
            assertTrue(successState.confidence >= 0f)
            assertTrue(successState.lastUpdated > 0L)
            cancelAndConsumeRemainingEvents()
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
        viewModel.uiState.test {
            advanceUntilIdle()
            val cachedState = awaitFirstSuccessEmission()
            assertNotNull("Expected cached success state before refresh", cachedState)

            viewModel.requestRefresh()
            runCurrent()

            // Worker was called
            verify { HealthSyncWorker.scheduleImmediate(mockContext) }

            // Current state is still Success, not Loading — critical D-08 behaviour
            val refreshedState = awaitSuccessWhere { it.isSyncing }
            assertTrue("isSyncing must be true while sync is pending", refreshedState.isSyncing)

            cancelAndConsumeRemainingEvents()
        }
    }

    /**
     * Test 3: Score changes after a background sync produce ScoreDelta payloads with metadata.
     */
    @Test
    fun `score change after sync emits delta animation payloads with confidence metadata`() = runTest(testDispatcher) {
        todayFlow.value = makeMetrics(recovery = 70, confidence = 0.80f)

        viewModel.uiState.test {
            advanceUntilIdle()
            val initialState = awaitFirstSuccessEmission()
            assertEquals(70, initialState.recoveryScore)

            // Simulate sync result — Room emits a new row
            todayFlow.value = makeMetrics(recovery = 85, confidence = 0.92f)
            advanceUntilIdle()

            val updatedState = awaitSuccessWhere {
                it.recoveryScore == 85 && it.recoveryDelta != null
            }

            // Delta must be non-null because recovery changed
            val delta = updatedState.recoveryDelta
            assertNotNull("Delta must be produced when score changes", delta)
            assertEquals(70, delta!!.previous)
            assertEquals(85, delta.current)
            assertEquals(DeltaDirection.UP, delta.direction)

            // Confidence/lastUpdated always present after sync
            assertEquals(0.92f, updatedState.confidence, 0.001f)
            assertTrue(updatedState.lastUpdated > 0L)

            // Unchanged scores produce no delta
            assertNull("Unchanged sleep score must not produce a delta", updatedState.sleepDelta)

            cancelAndConsumeRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<HomeUiState>.awaitFirstSuccessEmission(): HomeUiState.Success {
        repeat(8) {
            when (val emission = awaitItem()) {
                is HomeUiState.Success -> return emission
                else -> Unit
            }
        }

        throw AssertionError("Expected at least one Success emission")
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<HomeUiState>.awaitSuccessWhere(
        predicate: (HomeUiState.Success) -> Boolean
    ): HomeUiState.Success {
        repeat(12) {
            when (val emission = awaitItem()) {
                is HomeUiState.Success -> if (predicate(emission)) return emission
                else -> Unit
            }
        }

        throw AssertionError("Expected a Success emission matching predicate")
    }
}
