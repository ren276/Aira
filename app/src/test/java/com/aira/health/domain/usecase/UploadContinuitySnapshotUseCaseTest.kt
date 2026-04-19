package com.aira.health.domain.usecase

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.aira.health.data.local.dao.ContinuitySyncStateDao
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.data.local.model.ContinuitySyncState
import com.aira.health.data.local.model.DailyMetrics
import com.aira.health.domain.model.ContinuitySnapshot
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.ContinuitySnapshotRepository
import com.aira.health.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files

class UploadContinuitySnapshotUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var dailyMetricsDao: DailyMetricsDao
    private lateinit var continuitySyncStateDao: ContinuitySyncStateDao
    private lateinit var continuitySnapshotRepository: ContinuitySnapshotRepository

    @Before
    fun setUp() {
        userRepository = mockk(relaxed = true)
        dailyMetricsDao = mockk(relaxed = true)
        continuitySyncStateDao = mockk(relaxed = true)
        continuitySnapshotRepository = mockk(relaxed = true)
    }

    @Test
    fun `invoke uploads latest derived snapshot and stores success sync state`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { Files.createTempFile("aira-continuity", ".preferences_pb").toFile() }
        )
        dataStore.edit { prefs ->
            prefs[CLOUD_BACKUP_ENABLED] = true
        }

        coEvery { userRepository.getCurrentSession() } returns UserSession(
            userId = "user-1",
            email = "u@a.com",
            displayName = "U",
            avatarUrl = null,
            isGuest = false,
            isAuthenticated = true
        )
        coEvery { dailyMetricsDao.observeRecent(1) } returns flowOf(
            listOf(
                DailyMetrics(
                    date = "2026-04-18",
                    recoveryScore = 74,
                    sleepScore = 76,
                    strainScore = 55,
                    stressScore = 42,
                    energyBankScore = 64,
                    burnoutRiskIndex = 0.31f,
                    dataConfidence = 0.83f
                )
            )
        )
        coEvery { continuitySnapshotRepository.uploadSnapshot(any(), any()) } returns Result.success(Unit)

        val useCase = UploadContinuitySnapshotUseCase(
            dataStore = dataStore,
            userRepository = userRepository,
            dailyMetricsDao = dailyMetricsDao,
            continuitySyncStateDao = continuitySyncStateDao,
            continuitySnapshotRepository = continuitySnapshotRepository
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        coVerify(exactly = 1) {
            continuitySnapshotRepository.uploadSnapshot("user-1", match<ContinuitySnapshot> {
                it.recoveryScore == 74 && it.sleepScore == 76 && it.cloudBackupEnabled
            })
        }
        coVerify(exactly = 1) {
            continuitySyncStateDao.upsert(match<ContinuitySyncState> {
                it.userId == "user-1" && it.retryCount == 0 && it.lastErrorCode == null
            })
        }
    }

    @Test
    fun `invoke returns false when cloud backup disabled and force not requested`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { Files.createTempFile("aira-continuity", ".preferences_pb").toFile() }
        )

        val useCase = UploadContinuitySnapshotUseCase(
            dataStore = dataStore,
            userRepository = userRepository,
            dailyMetricsDao = dailyMetricsDao,
            continuitySyncStateDao = continuitySyncStateDao,
            continuitySnapshotRepository = continuitySnapshotRepository
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
        coVerify(exactly = 0) { continuitySnapshotRepository.uploadSnapshot(any(), any()) }
    }

    private companion object {
        val CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
    }
}
