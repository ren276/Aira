package com.aira.health.presentation.dashboard.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.material3.ExperimentalMaterial3Api
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class MetricDetailSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recoveryDetail_showsFullContentAndExplanation() {
        val state = MetricDetailUiState.Success(
            metricType = MetricType.RECOVERY,
            currentScore = 85,
            trendDataPoints = listOf(80f, 85f),
            confidence = 0.9f,
            whatChanged = "Increased by 5",
            whyItMatters = "Shows good adaptation",
            whatToDoNext = "Train hard"
        )

        composeTestRule.setContent {
            com.aira.health.presentation.dashboard.details.EnergyBankScreen(
                state = state,
                onNavigateBack = {}
            )

            com.aira.health.presentation.dashboard.details.components.ExplanationBottomSheet(
                whatChanged = state.whatChanged,
                whyItMatters = state.whyItMatters,
                whatToDoNext = state.whatToDoNext,
                onDismissRequest = {}
            )
        }

        composeTestRule.onNodeWithText("Energy Bank").assertIsDisplayed()
        composeTestRule.onNodeWithText("What changed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Increased by 5").assertIsDisplayed()

        composeTestRule.onNodeWithText("Why it matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shows good adaptation").assertIsDisplayed()

        composeTestRule.onNodeWithText("What to do next").assertIsDisplayed()
        composeTestRule.onNodeWithText("Train hard").assertIsDisplayed()
    }
    
    @Test
    fun stressDetail_showsFullContentAndExplanation() {
        val state = MetricDetailUiState.Success(
            metricType = MetricType.STRESS,
            currentScore = 60,
            trendDataPoints = listOf(55f, 60f),
            confidence = 0.8f,
            whatChanged = "Stress increased",
            whyItMatters = "Elevated cortisol",
            whatToDoNext = "Take a walk"
        )

        composeTestRule.setContent {
            com.aira.health.presentation.dashboard.details.StressDetailScreen(
                state = state,
                onNavigateBack = {}
            )
            com.aira.health.presentation.dashboard.details.components.ExplanationBottomSheet(
                whatChanged = state.whatChanged,
                whyItMatters = state.whyItMatters,
                whatToDoNext = state.whatToDoNext,
                onDismissRequest = {}
            )
        }

        composeTestRule.onNodeWithText("Stress").assertIsDisplayed()
        composeTestRule.onNodeWithText("What changed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stress increased").assertIsDisplayed()
    }
}
