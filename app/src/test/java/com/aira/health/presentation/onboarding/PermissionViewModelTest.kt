package com.aira.health.presentation.onboarding

import com.aira.health.util.permission.HealthConnectStatus
import com.aira.health.util.permission.HealthPermissionManager
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionViewModelTest {

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
    fun coreBatchGrantedAdvancesToBodyBatch() = runTest {
        val manager = mockk<HealthPermissionManager>()
        coEvery { manager.getHealthConnectStatus() } returns HealthConnectStatus.Available
        coEvery { manager.getGrantedPermissions() } returns setOf("core-permission")
        coEvery {
            manager.isBatchSatisfied(any(), HealthPermissionManager.PermissionBatch.CORE)
        } returns true
        coEvery {
            manager.isBatchSatisfied(any(), HealthPermissionManager.PermissionBatch.BODY)
        } returns false
        coEvery {
            manager.isBatchSatisfied(any(), HealthPermissionManager.PermissionBatch.ADVANCED)
        } returns false

        val viewModel = PermissionViewModel(manager)
        advanceUntilIdle()

        viewModel.onBatchPermissionsResult(setOf("core-permission"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCoreGranted)
        assertEquals(HealthPermissionManager.PermissionBatch.BODY, state.currentBatch)
        assertFalse(state.onboardingComplete)
    }

    @Test
    fun coreBatchDeniedKeepsUserOnCoreBatch() = runTest {
        val manager = mockk<HealthPermissionManager>()
        coEvery { manager.getHealthConnectStatus() } returns HealthConnectStatus.Available
        coEvery { manager.getGrantedPermissions() } returns emptySet()
        coEvery {
            manager.isBatchSatisfied(any(), HealthPermissionManager.PermissionBatch.CORE)
        } returns false
        coEvery {
            manager.isBatchSatisfied(any(), HealthPermissionManager.PermissionBatch.BODY)
        } returns false
        coEvery {
            manager.isBatchSatisfied(any(), HealthPermissionManager.PermissionBatch.ADVANCED)
        } returns false

        val viewModel = PermissionViewModel(manager)
        advanceUntilIdle()

        viewModel.onBatchPermissionsResult(emptySet())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCoreGranted)
        assertEquals(HealthPermissionManager.PermissionBatch.CORE, state.currentBatch)
        assertFalse(state.onboardingComplete)
    }

    @Test
    fun useLimitedModeCompletesOnboarding() = runTest {
        val manager = mockk<HealthPermissionManager>()
        coEvery { manager.getHealthConnectStatus() } returns HealthConnectStatus.Available

        val viewModel = PermissionViewModel(manager)
        advanceUntilIdle()

        viewModel.onUseLimitedModeTapped()

        val state = viewModel.uiState.value
        assertTrue(state.isLimitedModeSelected)
        assertTrue(state.onboardingComplete)
    }
}
