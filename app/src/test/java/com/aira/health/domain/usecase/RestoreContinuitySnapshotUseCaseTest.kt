package com.aira.health.domain.usecase

import com.aira.health.domain.model.ContinuitySnapshot
import com.aira.health.domain.model.UserSession
import com.aira.health.domain.repository.ContinuitySnapshotRepository
import com.aira.health.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestoreContinuitySnapshotUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var continuitySnapshotRepository: ContinuitySnapshotRepository
    private lateinit var useCase: RestoreContinuitySnapshotUseCase

    @Before
    fun setUp() {
        userRepository = mockk(relaxed = true)
        continuitySnapshotRepository = mockk(relaxed = true)
        useCase = RestoreContinuitySnapshotUseCase(userRepository, continuitySnapshotRepository)
    }

    @Test
    fun `fetchLatest returns latest cloud snapshot for active session`() = runTest {
        val snapshot = ContinuitySnapshot(
            snapshotId = "user-1-2026-04-18",
            capturedAtEpochMs = 1713398400000,
            recoveryScore = 72,
            sleepScore = 70,
            strainScore = 58,
            stressScore = 45,
            energyBankScore = 61,
            burnoutRiskIndex = 0.39f,
            dataConfidence = 0.81f,
            cloudBackupEnabled = true
        )
        coEvery { userRepository.getCurrentSession() } returns UserSession(
            userId = "user-1",
            email = "u@a.com",
            displayName = "U",
            avatarUrl = null,
            isGuest = false,
            isAuthenticated = true
        )
        coEvery { continuitySnapshotRepository.getLatestSnapshot("user-1") } returns Result.success(snapshot)

        val result = useCase.fetchLatest()

        assertTrue(result.isSuccess)
        assertEquals("user-1-2026-04-18", result.getOrNull()?.snapshotId)
    }

    @Test
    fun `applySelected returns restored false when snapshot is null`() = runTest {
        val result = useCase.applySelected(null)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull()?.restored)
    }
}
