package com.aira.health.presentation.dashboard.details

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aira.health.data.local.dao.BaselineDao
import com.aira.health.data.local.dao.UserCorrectionDao
import com.aira.health.data.local.model.Baseline
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackRequest
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackResult
import com.aira.health.domain.usecase.ApplyUserCorrectionFeedbackUseCase
import com.aira.health.domain.usecase.CorrectionInfluencePreview
import com.aira.health.presentation.supplementary.DataCorrectionsScreen
import com.aira.health.presentation.supplementary.DataCorrectionsViewModel
import com.aira.health.presentation.theme.AiraTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class ExplainabilityUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun detailSurfaces_showConfidenceRecencyAndTopThreeFactors() {
        val state = demoState()

        val screens = listOf<@Composable () -> Unit>(
            { RecoveryDetailScreen(state = state, onNavigateBack = {}) },
            { SleepDetailScreen(state = state.copy(metricType = MetricType.SLEEP), onNavigateBack = {}) },
            { StrainDetailScreen(state = state.copy(metricType = MetricType.STRAIN), onNavigateBack = {}) },
            { StressDetailScreen(state = state.copy(metricType = MetricType.STRESS), onNavigateBack = {}) }
        )

        screens.forEach { screen ->
            composeTestRule.setContent {
                AiraTheme {
                    screen()
                }
            }

            composeTestRule.onNodeWithText("Confidence: High").assertIsDisplayed()
            composeTestRule.onNodeWithText("Window: last 7d").assertIsDisplayed()
            composeTestRule.onNodeWithTag("factor-row-1").assertIsDisplayed()
            composeTestRule.onNodeWithTag("factor-row-2").assertIsDisplayed()
            composeTestRule.onNodeWithTag("factor-row-3").assertIsDisplayed()
            composeTestRule.onNodeWithText("Increased", substring = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("41%", substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun correctionFlow_requiresConfirmation_thenShowsSuccessMessage() {
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
                correctionId = 22L
            )

        val viewModel = DataCorrectionsViewModel(
            userCorrectionDao = correctionsDao,
            baselineDao = baselineDao,
            applyUserCorrectionFeedbackUseCase = applyUseCase
        )

        composeTestRule.setContent {
            AiraTheme {
                DataCorrectionsScreen(onNavigateBack = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("original-value-input").performTextInput("75")
        composeTestRule.onNodeWithTag("corrected-value-input").performTextInput("80")
        composeTestRule.onNodeWithTag("apply-correction-button").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Please confirm before applying this correction.").assertIsDisplayed()
        coVerify(exactly = 0) { applyUseCase.applyCorrection(any()) }

        composeTestRule.onNodeWithTag("correction-confirmation-checkbox").performClick()
        composeTestRule.onNodeWithTag("apply-correction-button").performClick()

        composeTestRule.waitForIdle()

        coVerify(exactly = 1) { applyUseCase.applyCorrection(any()) }
        composeTestRule.onNodeWithTag("correction-success-message").assertIsDisplayed()
    }

    private fun demoState(): MetricDetailUiState.Success {
        return MetricDetailUiState.Success(
            metricType = MetricType.RECOVERY,
            currentScore = 82,
            trendDataPoints = listOf(72f, 76f, 82f),
            confidence = 0.81f,
            confidenceTierLabel = "High",
            recencyWindowText = "last 7d",
            rankedFactors = listOf(
                MetricDetailUiState.RankedFactor(
                    rank = 1,
                    name = "Sleep Debt",
                    direction = MetricDetailUiState.FactorDirection.INCREASED,
                    weight = 0.41f,
                    windowTag = "last 7d"
                ),
                MetricDetailUiState.RankedFactor(
                    rank = 2,
                    name = "HRV Trend",
                    direction = MetricDetailUiState.FactorDirection.DECREASED,
                    weight = 0.32f,
                    windowTag = "last 14d"
                ),
                MetricDetailUiState.RankedFactor(
                    rank = 3,
                    name = "Strain Carryover",
                    direction = MetricDetailUiState.FactorDirection.NEUTRAL,
                    weight = 0.18f,
                    windowTag = "last 3d"
                )
            ),
            whatChanged = "Recovery improved versus yesterday.",
            whyItMatters = "Better recovery supports adaptation.",
            whatToDoNext = "Proceed with planned training.",
            dataSources = listOf("Daily score model"),
            consideredData = listOf("Sleep duration", "HRV trend", "Strain score")
        )
    }
}