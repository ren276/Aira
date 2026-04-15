package com.aira.health.presentation.dashboard.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.material3.ExperimentalMaterial3Api
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class MetricDetailSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recoveryDetail_showsFullContentAndExplanation() {
        // Setup state for Recovery
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
            // Fake the viewmodel with direct state passing to route
            // For testing the route rendering directly we pass a fake lambda or mock
            // Alternatively, we test the visual primitives directly.
            // Since MetricDetailRoute requires a ViewModel via Hilt, we just test the explicit screen + Bottom sheet.
            
            // To be robust, we'll compose the Route content directly or use the specific screen.
            com.aira.health.presentation.dashboard.details.screens.RecoveryDetailScreen(state = state)
            
            // And append the ExplanationBottomSheet
            com.aira.health.presentation.dashboard.details.components.ExplanationBottomSheet(
                whatChanged = state.whatChanged,
                whyItMatters = state.whyItMatters,
                whatToDoNext = state.whatToDoNext,
                onDismissRequest = {}
            )
        }

        // Verify Metric specifics
        composeTestRule.onNodeWithText("Recovery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Score: 85 (Confidence: 0.9)").assertIsDisplayed()
        
        // Verify trend/factors/action are on screen
        composeTestRule.onNodeWithText("Trend Window").assertIsDisplayed()
        composeTestRule.onNodeWithText("Factor Breakdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggested Action").assertIsDisplayed()
        
        // Verify Explanation sheet content (fixed 3-part layout D-11)
        composeTestRule.onNodeWithText("What Changed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Increased by 5").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Why It Matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shows good adaptation").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("What To Do Next").assertIsDisplayed()
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
            com.aira.health.presentation.dashboard.details.screens.StressDetailScreen(state = state)
            com.aira.health.presentation.dashboard.details.components.ExplanationBottomSheet(
                whatChanged = state.whatChanged,
                whyItMatters = state.whyItMatters,
                whatToDoNext = state.whatToDoNext,
                onDismissRequest = {}
            )
        }

        composeTestRule.onNodeWithText("Stress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Score: 60 (Confidence: 0.8)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stress increased").assertIsDisplayed()
    }
}
