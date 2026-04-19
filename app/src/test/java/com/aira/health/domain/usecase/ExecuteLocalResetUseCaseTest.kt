package com.aira.health.domain.usecase

import com.aira.health.data.local.db.AiraDatabase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExecuteLocalResetUseCaseTest {

    private lateinit var uploadContinuitySnapshotUseCase: UploadContinuitySnapshotUseCase
    private lateinit var airaDatabase: AiraDatabase
    private lateinit var useCase: ExecuteLocalResetUseCase

    @Before
    fun setUp() {
        uploadContinuitySnapshotUseCase = mockk(relaxed = true)
        airaDatabase = mockk(relaxed = true)
        useCase = ExecuteLocalResetUseCase(uploadContinuitySnapshotUseCase, airaDatabase)
    }

    @Test
    fun `invoke blocks wipe when final upload fails without override`() = runTest {
        coEvery { uploadContinuitySnapshotUseCase.invoke(force = true) } returns Result.failure(
            IllegalStateException("cloud upload failed")
        )

        val result = useCase(allowIrreversibleOverride = false)

        assertTrue(result is LocalResetResult.Blocked)
        coVerify(exactly = 0) { airaDatabase.clearAllTables() }
    }

    @Test
    fun `invoke executes wipe when override is explicitly allowed after upload failure`() = runTest {
        coEvery { uploadContinuitySnapshotUseCase.invoke(force = true) } returns Result.failure(
            IllegalStateException("cloud upload failed")
        )

        val result = useCase(allowIrreversibleOverride = true)

        assertTrue(result is LocalResetResult.Completed)
        coVerify(exactly = 1) { airaDatabase.clearAllTables() }
    }
}
