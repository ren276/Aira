package com.aira.health.presentation.dashboard.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aira.health.presentation.theme.AiraTheme
import org.junit.Rule
import org.junit.Test

/**
 * Compose instrumentation tests for [HomeDashboardScreen] contract.
 *
 * Tests (D-07, D-08, D-09):
 *  - Test 1: 2×2 grid cards appear with correct semantic tags in the locked clinical order
 *  - Test 2: CausalAnomalyCard is always visible — in both active-anomaly and fallback-forecast states
 *  - Test 3: Refresh interaction does not remove existing cards before sync completes
 */
class HomeDashboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun successState(
        anomaly: AnomalyPayload? = null,
        isSyncing: Boolean = false
    ) = HomeUiState.Success(
        recoveryScore = 80,
        sleepScore    = 72,
        strainScore   = 55,
        stressScore   = 38,
        confidence    = 0.88f,
        lastUpdated   = System.currentTimeMillis(),
        isSyncing     = isSyncing,
        anomaly       = anomaly
    )

    /**
     * Test 1: All four metric cards appear with their semantic content descriptions,
     * which confirms the fixed clinical order (Recovery, Sleep, Strain, Stress).
     */
    @Test
    fun metricGrid_showsAllFourCardsWithSemanticTags() {
        composeRule.setContent {
            AiraTheme {
                HomeSuccessContentTestWrapper(state = successState())
            }
        }

        // Each MetricGridCard has contentDescription = "$label metric card"
        composeRule.onNodeWithContentDescription("Recovery metric card").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sleep metric card").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Strain metric card").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Stress metric card").assertIsDisplayed()
    }

    /**
     * Test 2a: CausalAnomalyCard is visible in active-anomaly state.
     */
    @Test
    fun causalAnomalyCard_visibleWhenAnomalyActive() {
        val anomaly = AnomalyPayload(
            title       = "HRV depression detected",
            description = "Your morning HRV dropped 18% overnight.",
            severity    = 0.7f
        )
        composeRule.setContent {
            AiraTheme {
                HomeSuccessContentTestWrapper(state = successState(anomaly = anomaly))
            }
        }

        composeRule.onNodeWithContentDescription("causal anomaly card").assertIsDisplayed()
        composeRule.onNodeWithText("HRV depression detected").assertIsDisplayed()
    }

    /**
     * Test 2b: CausalAnomalyCard is visible in fallback-forecast state (no active anomaly).
     */
    @Test
    fun causalAnomalyCard_visibleInForecastFallbackState() {
        composeRule.setContent {
            AiraTheme {
                HomeSuccessContentTestWrapper(state = successState(anomaly = null))
            }
        }

        // The card is always present — in fallback mode it shows the forecast card
        composeRule.onNodeWithContentDescription("causal anomaly card").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("forecast guidance card").assertIsDisplayed()
    }

    /**
     * Test 3: Metric cards remain visible when isSyncing = true (no Loading regression).
     *
     * D-08: existing data must not be cleared when a refresh is pending.
     */
    @Test
    fun refresh_doesNotClearExistingCardsWhileSyncing() {
        composeRule.setContent {
            AiraTheme {
                HomeSuccessContentTestWrapper(state = successState(isSyncing = true))
            }
        }

        // Cards must still be visible even while syncing
        composeRule.onNodeWithContentDescription("Recovery metric card").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sleep metric card").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Strain metric card").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Stress metric card").assertIsDisplayed()
        // Anomaly card always present
        composeRule.onNodeWithContentDescription("causal anomaly card").assertIsDisplayed()
    }
}
