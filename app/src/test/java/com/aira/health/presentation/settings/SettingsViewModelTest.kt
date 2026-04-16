package com.aira.health.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.UserRepository
import com.aira.health.util.permission.HealthConnectStatus
import com.aira.health.util.permission.HealthPermissionManager
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun emitsRuntimeProfileConfidenceAndSyncState() = runTest {
        val dataStore = createDataStore()
        val dailyMetricsDao = mockk<DailyMetricsDao>()
        val userRepository = mockk<UserRepository>()
        val permissionManager = mockk<HealthPermissionManager>()
        val authState = MutableStateFlow<AuthState>(
            AuthState.Authenticated(
                UserSession(
                    userId = "user-1",
                    email = "alex@aira.app",
                    displayName = "Alex",
                    avatarUrl = null,
                    isGuest = false,
                    isAuthenticated = true
                )
            )
        )

        every { userRepository.observeAuthState() } returns authState
        every { dailyMetricsDao.observeRecent(7) } returns flowOf(
            listOf(
                DailyMetrics(
                    date = "2026-04-16",
                    recoveryScore = 78,
                    sleepScore = 80,
                    strainScore = 30,
                    stressScore = 40,
                    dataConfidence = 0.81f
                )
            )
        )
        coEvery { permissionManager.isCoreGranted() } returns true
        every { permissionManager.getHealthConnectStatus() } returns HealthConnectStatus.Available

        val viewModel = SettingsViewModel(dataStore, dailyMetricsDao, userRepository, permissionManager)

        viewModel.uiState.test {
            assertTrue(awaitItem().loading)

            val state = awaitItem()

            assertEquals("Alex", state.profileName)
            assertEquals("Firebase account connected", state.planStatus)
            assertTrue(state.healthConnectSyncEnabled)
            assertEquals("Local model ready (81% confidence)", state.localModelStatus)
            assertEquals(81, state.confidencePercent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun preferenceTogglesPersistThroughViewModelState() = runTest {
        val dataStore = createDataStore()
        val dailyMetricsDao = mockk<DailyMetricsDao>()
        val userRepository = mockk<UserRepository>()
        val permissionManager = mockk<HealthPermissionManager>()

        every { userRepository.observeAuthState() } returns MutableStateFlow(AuthState.Guest)
        every { dailyMetricsDao.observeRecent(7) } returns flowOf(emptyList())
        coEvery { permissionManager.isCoreGranted() } returns false
        every { permissionManager.getHealthConnectStatus() } returns HealthConnectStatus.NotInstalled

        val viewModel = SettingsViewModel(dataStore, dailyMetricsDao, userRepository, permissionManager)

        viewModel.uiState.test {
            assertTrue(awaitItem().loading)

            val initialState = awaitItem()
            assertFalse(initialState.healthConnectSyncEnabled)

            viewModel.setForceOledDarkTheme(true)
            viewModel.setCloudBackupEnabled(true)
            advanceUntilIdle()

            var updatedState = awaitItem()
            var attempts = 0
            while (
                (updatedState.forceOledDarkTheme != true || !updatedState.cloudBackupEnabled) &&
                attempts < 4
            ) {
                updatedState = awaitItem()
                attempts++
            }

            assertEquals(true, updatedState.forceOledDarkTheme)
            assertTrue(updatedState.cloudBackupEnabled)
            assertFalse(updatedState.healthConnectSyncEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { Files.createTempFile("settings", ".preferences_pb").toFile() }
        )
    }
}