package com.aira.health.presentation.supplementary

import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.UserCorrectionDao
import com.aira.health.data.local.model.Baseline
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackRequest
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackResult
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackUseCase
import com.aira.health.domain.usecase.CorrectionInfluencePreview
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class DataCorrectionsViewModelTest {

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
    fun emitsCountsAndBaselinesFromLocalState() = runTest {
        val correctionsDao = mockk<UserCorrectionDao>()
        val baselineDao = mockk<BaselineDao>()
        val applyUseCase = mockk<ApplyUserCorrectionFeedbackUseCase>()
        coEvery { correctionsDao.getCountByType("sleep") } returns 2
        coEvery { correctionsDao.getCountByType("hrv") } returns 1
        coEvery { baselineDao.get("sleep_score") } returns Baseline("sleep_score", 78f, 0.2f)
        coEvery { baselineDao.get("hrv_rmssd") } returns Baseline("hrv_rmssd", 42f, 0.2f)
        every {
            applyUseCase.previewInfluence(any(), any())
        } returns CorrectionInfluencePreview(
            affectedParameter = "Sleep Need Baseline",
            influenceWindowDays = 14,
            influenceWindowLabel = "next 14 days",
            maxInfluenceCap = 0.2f
        )

        val viewModel = DataCorrectionsViewModel(correctionsDao, baselineDao, applyUseCase)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertEquals(2, state.sleepCorrections)
        assertEquals(1, state.hrvCorrections)
        assertEquals("78 score", state.sleepBaselineLabel)
        assertEquals("42 ms", state.hrvBaselineLabel)
        assertEquals("Sleep Need Baseline", state.preview?.affectedParameter)
        assertTrue(state.timelineMessage.contains("3 validated correction(s)"))
    }

    @Test
    fun emptyStateStaysExplicitWhenNoCorrectionsExist() = runTest {
        val correctionsDao = mockk<UserCorrectionDao>()
        val baselineDao = mockk<BaselineDao>()
        coEvery { correctionsDao.getCountByType(any()) } returns 0
        val applyUseCase = mockk<ApplyUserCorrectionFeedbackUseCase>()
        coEvery { baselineDao.get(any()) } returns null

        every {
            applyUseCase.previewInfluence(any(), any())
        } returns CorrectionInfluencePreview(
            affectedParameter = "Sleep Need Baseline",
            influenceWindowDays = 14,
            influenceWindowLabel = "next 14 days",
            maxInfluenceCap = 0.2f
        )
        val viewModel = DataCorrectionsViewModel(correctionsDao, baselineDao, applyUseCase)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value

        collector.cancel()

        assertFalse(state.timelineMessage.contains("validated correction"))
        assertEquals("not set", state.sleepBaselineLabel)
        assertEquals("not set", state.hrvBaselineLabel)
        assertTrue(state.insufficientData)
    }

    @Test
    fun previewIncludesAffectedParameterWindowAndCap() = runTest {
        val correctionsDao = mockk<UserCorrectionDao>()
        val baselineDao = mockk<BaselineDao>()
        val applyUseCase = mockk<ApplyUserCorrectionFeedbackUseCase>()

        coEvery { correctionsDao.getCountByType(any()) } returns 1
        coEvery { baselineDao.get(any()) } returns Baseline("sleep_score", 80f, 0.1f)
        every { applyUseCase.previewInfluence(any(), any()) } returns CorrectionInfluencePreview(
            affectedParameter = "Sleep Need Baseline",
            influenceWindowDays = 14,
            influenceWindowLabel = "next 14 days",
            maxInfluenceCap = 0.2f
        )

        val viewModel = DataCorrectionsViewModel(correctionsDao, baselineDao, applyUseCase)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.previewInfluence()
        advanceUntilIdle()

        val preview = viewModel.uiState.value.preview
        collector.cancel()

        requireNotNull(preview)
        assertEquals("Sleep Need Baseline", preview.affectedParameter)
        assertEquals(14, preview.influenceWindowDays)
        assertEquals(0.2f, preview.maxInfluenceCap, 0.0001f)
    }

    @Test
    fun submitRequiresConfirmationBeforeMutation() = runTest {
        val correctionsDao = mockk<UserCorrectionDao>(relaxed = true)
        val baselineDao = mockk<BaselineDao>()
        val applyUseCase = mockk<ApplyUserCorrectionFeedbackUseCase>()

        coEvery { correctionsDao.getCountByType(any()) } returns 0
        coEvery { baselineDao.get("sleep_score") } returns Baseline("sleep_score", 70f, 0.1f)
        coEvery { baselineDao.get("hrv_rmssd") } returns Baseline("hrv_rmssd", 45f, 0.1f)
        every { applyUseCase.previewInfluence(any(), any()) } returns CorrectionInfluencePreview(
            affectedParameter = "Sleep Need Baseline",
            influenceWindowDays = 14,
            influenceWindowLabel = "next 14 days",
            maxInfluenceCap = 0.2f
        )

        val viewModel = DataCorrectionsViewModel(correctionsDao, baselineDao, applyUseCase)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.updateOriginalValue("70")
        viewModel.updateCorrectedValue("74")
        viewModel.applyCorrection()
        advanceUntilIdle()

        collector.cancel()

        coVerify(exactly = 0) { applyUseCase.applyCorrection(any()) }
        assertTrue(viewModel.uiState.value.submitErrorMessage?.contains("confirm", ignoreCase = true) == true)
    }

    @Test
    fun successStateCommunicatesDelayedAdaptationEffect() = runTest {
        val correctionsDao = mockk<UserCorrectionDao>()
        val baselineDao = mockk<BaselineDao>()
        val applyUseCase = mockk<ApplyUserCorrectionFeedbackUseCase>()

        coEvery { correctionsDao.getCountByType(any()) } returnsMany listOf(0, 1)
        coEvery { baselineDao.get("sleep_score") } returns Baseline("sleep_score", 75f, 0.1f)
        coEvery { baselineDao.get("hrv_rmssd") } returns Baseline("hrv_rmssd", 44f, 0.1f)
        every { applyUseCase.previewInfluence(any(), any()) } returns CorrectionInfluencePreview(
            affectedParameter = "Sleep Need Baseline",
            influenceWindowDays = 14,
            influenceWindowLabel = "next 14 days",
            maxInfluenceCap = 0.2f
        )
        coEvery { applyUseCase.applyCorrection(any<ApplyUserCorrectionFeedbackRequest>()) } returns
            ApplyUserCorrectionFeedbackResult(
                success = true,
                message = "Correction saved. Future explanations will adapt over the next 14 days.",
                correctionId = 99L
            )

        val viewModel = DataCorrectionsViewModel(correctionsDao, baselineDao, applyUseCase)
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.updateOriginalValue("75")
        viewModel.updateCorrectedValue("80")
        viewModel.updateConfirmation(true)
        viewModel.applyCorrection()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        collector.cancel()

        assertTrue(state.submitSuccessMessage?.contains("next 14 days") == true)
        coVerify(exactly = 1) { applyUseCase.applyCorrection(any()) }
    }
}