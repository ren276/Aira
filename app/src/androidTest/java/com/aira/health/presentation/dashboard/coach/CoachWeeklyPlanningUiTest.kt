package com.aira.health.presentation.dashboard.coach

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.presentation.theme.AiraTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CoachWeeklyPlanningUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun coachCards_areVisibleInExpectedOrder() {
        composeRule.setContent {
            AiraTheme {
                CoachReadyContent(
                    state = sampleState(),
                    onSleepDeltaChanged = {},
                    onTrainingLoadDeltaChanged = {},
                    onRecalculate = {},
                )
            }
        }

        val scenario = composeRule.onNodeWithTag("coach-card-scenario").assertIsDisplayed().fetchSemanticsNode()
        val projection = composeRule.onNodeWithTag("coach-card-projection").assertIsDisplayed().fetchSemanticsNode()
        val guidance = composeRule.onNodeWithTag("coach-card-guidance").assertIsDisplayed().fetchSemanticsNode()
        val weekly = composeRule.onNodeWithTag("coach-card-weekly-draft").assertIsDisplayed().fetchSemanticsNode()

        assertTrue(scenario.boundsInRoot.top < projection.boundsInRoot.top)
        assertTrue(projection.boundsInRoot.top < guidance.boundsInRoot.top)
        assertTrue(guidance.boundsInRoot.top < weekly.boundsInRoot.top)
    }

    @Test
    fun scenarioControls_wireSliderAndRecalculateInteraction() {
        composeRule.setContent {
            AiraTheme {
                var sleepDelta by remember { mutableFloatStateOf(0f) }
                var loadDelta by remember { mutableFloatStateOf(0f) }
                var recalcCount by remember { mutableIntStateOf(0) }

                val state = sampleState(
                    scenario = ScenarioInput(
                        targetDate = "2026-04-22",
                        sleepDeltaHours = sleepDelta,
                        trainingLoadDeltaPercent = loadDelta,
                    )
                )

                CoachReadyContent(
                    state = state,
                    onSleepDeltaChanged = { sleepDelta = it },
                    onTrainingLoadDeltaChanged = { loadDelta = it },
                    onRecalculate = { recalcCount += 1 },
                )

                Text(text = "recalc:$recalcCount")
            }
        }

        composeRule.onNodeWithTag("coach-sleep-slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(1.5f)
            }

        composeRule.onNodeWithText("Sleep delta: +1.5 h").assertIsDisplayed()

        composeRule.onNodeWithTag("coach-recalculate-button").performClick()
        composeRule.onNodeWithText("recalc:1").assertIsDisplayed()
    }

    private fun sampleState(scenario: ScenarioInput = defaultScenario()): CoachUiState.Ready {
        return CoachUiState.Ready(
            scenario = scenario,
            projection = ProjectionCardModel(
                projectedRecoveryDelta = 3,
                projectedEnergyDelta = 1,
                projectedBurnoutTier = "MODERATE",
                projectedBurnoutTrajectory = "RISING",
                confidenceTier = PredictionConfidenceTier.MEDIUM,
                confidenceScore = 0.67f,
                uncertaintyLabel = "Confidence is moderate; treat as directional guidance.",
            ),
            guidance = GuidanceCardModel(
                summary = "Readiness is stable for controlled progression this week.",
                actions = listOf(
                    "Training: add load gradually.",
                    "Recovery: protect sleep consistency.",
                    "Nutrition: align carbs with session demand.",
                ),
                citations = listOf("recovery_score", "strain_score", "sleep_score"),
                uncertaintyLabel = "Confidence is moderate; adjust conservatively.",
            ),
            weeklyDraft = WeeklyDraftCardModel(
                targetDate = "2026-04-22",
                loadRecoveryBalanceSummary = "Load shift +10% with projected recovery +3 and energy +1.",
                weeklyFocus = "Balance moderate load with proactive recovery blocks.",
                priorityActions = listOf(
                    "Training: controlled progression.",
                    "Recovery: nightly wind-down.",
                    "Nutrition: post-session refuel.",
                ),
                cautionNotes = listOf(
                    "Burnout outlook: moderate risk with rising trajectory.",
                    "Confidence is moderate; keep plans flexible.",
                ),
                confidenceTier = PredictionConfidenceTier.MEDIUM,
                confidenceScore = 0.67f,
                uncertaintyLabel = "Confidence is moderate; use directional guidance.",
            ),
            isRefreshing = false,
        )
    }

    private fun defaultScenario(): ScenarioInput = ScenarioInput(
        targetDate = "2026-04-22",
        sleepDeltaHours = 0f,
        trainingLoadDeltaPercent = 0f,
    )
}
