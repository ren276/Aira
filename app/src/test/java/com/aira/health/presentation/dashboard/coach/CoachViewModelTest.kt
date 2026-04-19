package com.aira.health.presentation.dashboard.coach

import com.aira.health.domain.model.BurnoutRiskTier
import com.aira.health.domain.model.BurnoutTrajectory
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionScenario
import com.aira.health.domain.model.WeeklyAthletePlanDraft
import com.aira.health.domain.usecase.BuildWeeklyAthletePlanUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var buildWeeklyAthletePlanUseCase: BuildWeeklyAthletePlanUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        buildWeeklyAthletePlanUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `scenario controls update state and trigger refreshed projection`() = runTest(dispatcher) {
        coEvery { buildWeeklyAthletePlanUseCase.build(any()) } answers {
            val scenario = firstArg<PredictionScenario>()
            sampleDraft(
                scenario = scenario,
                projectedRecoveryDelta = scenario.sleepDeltaHours.toInt(),
                projectedEnergyDelta = (scenario.trainingLoadDeltaPercent / 10f).toInt(),
            )
        }

        val viewModel = CoachViewModel(buildWeeklyAthletePlanUseCase)
        advanceUntilIdle()

        viewModel.onSleepDeltaChanged(2f)
        viewModel.onTrainingLoadDeltaChanged(20f)
        advanceTimeBy(400)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CoachUiState.Ready
        assertEquals(2f, state.scenario.sleepDeltaHours)
        assertEquals(20f, state.scenario.trainingLoadDeltaPercent)
        assertEquals(2, state.projection.projectedRecoveryDelta)
        assertEquals(2, state.projection.projectedEnergyDelta)
    }

    @Test
    fun `weekly draft maps load recovery summary and action list`() = runTest(dispatcher) {
        coEvery { buildWeeklyAthletePlanUseCase.build(any()) } returns sampleDraft()

        val viewModel = CoachViewModel(buildWeeklyAthletePlanUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CoachUiState.Ready
        assertTrue(state.weeklyDraft.loadRecoveryBalanceSummary.contains("Load shift"))
        assertEquals(3, state.weeklyDraft.priorityActions.size)
        assertTrue(state.weeklyDraft.cautionNotes.isNotEmpty())
        assertEquals("2026-04-22", state.weeklyDraft.targetDate)
    }

    @Test
    fun `low confidence mapping keeps uncertainty label and removes diagnostic terms`() = runTest(dispatcher) {
        coEvery { buildWeeklyAthletePlanUseCase.build(any()) } returns sampleDraft(
            confidenceTier = PredictionConfidenceTier.LOW,
            confidenceScore = 0.22f,
            guidanceSummary = "Diagnosis suggests disease symptom progression.",
            priorityActions = listOf(
                "Prescribe easier training load.",
                "Symptom monitoring and sleep protection.",
                "Keep nutrition stable.",
            ),
            cautionNotes = listOf(
                "Disease outlook remains uncertain.",
                "Confidence is low and should remain non-diagnostic.",
            ),
            uncertaintyLabel = null,
        )

        val viewModel = CoachViewModel(buildWeeklyAthletePlanUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CoachUiState.Ready
        assertNotNull(state.guidance.uncertaintyLabel)
        assertTrue(state.guidance.uncertaintyLabel!!.contains("confidence", ignoreCase = true))

        val renderedText = buildString {
            append(state.guidance.summary)
            append(' ')
            append(state.guidance.actions.joinToString(" "))
            append(' ')
            append(state.weeklyDraft.cautionNotes.joinToString(" "))
        }.lowercase()

        assertFalse(renderedText.contains("diagnos"))
        assertFalse(renderedText.contains("disease"))
        assertFalse(renderedText.contains("symptom"))
        assertFalse(renderedText.contains("prescri"))
    }

    private fun sampleDraft(
        scenario: PredictionScenario = PredictionScenario(
            targetDate = "2026-04-22",
            sleepDeltaHours = 0f,
            trainingLoadDeltaPercent = 0f,
        ),
        projectedRecoveryDelta: Int = 3,
        projectedEnergyDelta: Int = 1,
        confidenceTier: PredictionConfidenceTier = PredictionConfidenceTier.MEDIUM,
        confidenceScore: Float = 0.62f,
        guidanceSummary: String = "Readiness is stable for a measured progression week.",
        priorityActions: List<String> = listOf(
            "Training: progress load by small increments.",
            "Recovery: protect sleep consistency.",
            "Nutrition: keep fueling matched to sessions.",
        ),
        cautionNotes: List<String> = listOf(
            "Burnout outlook: moderate risk with rising trajectory.",
            "Confidence is moderate; adapt if daily readiness drops.",
        ),
        uncertaintyLabel: String? = "Confidence is moderate; use directional guidance only.",
    ): WeeklyAthletePlanDraft {
        return WeeklyAthletePlanDraft(
            targetDate = "2026-04-22",
            scenario = scenario,
            projectedRecoveryDelta = projectedRecoveryDelta,
            projectedEnergyDelta = projectedEnergyDelta,
            projectedBurnoutTier = BurnoutRiskTier.MODERATE,
            projectedBurnoutTrajectory = BurnoutTrajectory.RISING,
            confidenceTier = confidenceTier,
            confidenceScore = confidenceScore,
            loadRecoveryBalanceSummary = "Load shift +10% with projected recovery +3 and energy +1.",
            weeklyFocus = "Balance moderate load with intentional recovery blocks.",
            guidanceSummary = guidanceSummary,
            priorityActions = priorityActions,
            cautionNotes = cautionNotes,
            citations = listOf("recovery_score", "strain_score"),
            uncertaintyLabel = uncertaintyLabel,
        )
    }
}
