package com.aira.health.domain.usecase

import com.aira.health.data.local.dao.CorrectionInfluenceDao
import com.aira.health.data.local.dao.PersonalizationStateDao
import com.aira.health.data.local.dao.UserCorrectionDao
import com.aira.health.data.local.model.PersonalizationState
import com.aira.health.data.local.model.UserCorrection
import com.aira.health.domain.engine.CorrectionInfluenceEngine
import com.aira.health.domain.engine.PersonalizationUpdateEngine
import com.aira.health.domain.model.PersonalizationParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdatePersonalizationStateUseCaseTest {

    private lateinit var personalizationStateDao: PersonalizationStateDao
    private lateinit var correctionInfluenceDao: CorrectionInfluenceDao
    private lateinit var userCorrectionDao: UserCorrectionDao
    private lateinit var useCase: UpdatePersonalizationStateUseCase

    @Before
    fun setUp() {
        personalizationStateDao = mockk(relaxed = true)
        correctionInfluenceDao = mockk(relaxed = true)
        userCorrectionDao = mockk(relaxed = true)

        useCase = UpdatePersonalizationStateUseCase(
            personalizationStateDao = personalizationStateDao,
            correctionInfluenceDao = correctionInfluenceDao,
            userCorrectionDao = userCorrectionDao,
            personalizationUpdateEngine = PersonalizationUpdateEngine(),
            correctionInfluenceEngine = CorrectionInfluenceEngine()
        )
    }

    @Test
    fun `use case combines bounded ema output with correction influence before persist`() = runTest {
        coEvery { personalizationStateDao.getLatest() } returns PersonalizationState(
            date = "2026-04-17",
            sleepNeedMinutes = 420f,
            recoverySpeed = 1.0f,
            stressSensitivity = 1.0f,
            usableDays = 9,
            applied = true,
            skipReason = null,
            correctionInfluenceApplied = 0f
        )
        coEvery { userCorrectionDao.getByDateRange(any(), any()) } returns listOf(
            UserCorrection(
                id = 1,
                recordType = "daily",
                recordDate = "2026-04-17",
                fieldName = "sleep_duration",
                originalValue = 0f,
                correctedValue = 0f,
                confidenceDelta = 0.2f
            )
        )

        val state = useCase.updateForDate(
            date = "2026-04-18",
            observed = PersonalizationParameters(430f, 1.1f, 0.9f),
            usableDays = 10
        )

        assertTrue(state.applied)
        assertTrue(state.sleepNeedMinutes > 422f)
        assertTrue(state.correctionInfluenceApplied > 0f)
        coVerify(exactly = 1) { correctionInfluenceDao.insertAll(any()) }
    }

    @Test
    fun `if update is skipped previous state is retained with skip reason metadata`() = runTest {
        coEvery { personalizationStateDao.getLatest() } returns PersonalizationState(
            date = "2026-04-17",
            sleepNeedMinutes = 420f,
            recoverySpeed = 1.0f,
            stressSensitivity = 1.0f,
            usableDays = 3,
            applied = true,
            skipReason = null,
            correctionInfluenceApplied = 0f
        )

        val state = useCase.updateForDate(
            date = "2026-04-18",
            observed = PersonalizationParameters(500f, 2.0f, 0.1f),
            usableDays = 5
        )

        assertFalse(state.applied)
        assertEquals("INSUFFICIENT_HISTORY", state.skipReason)
        assertEquals(420f, state.sleepNeedMinutes, 0.0001f)
    }
}
