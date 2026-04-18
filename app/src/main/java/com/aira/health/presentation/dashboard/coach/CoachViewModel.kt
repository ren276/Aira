package com.aira.health.presentation.dashboard.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.domain.model.PredictionConfidenceTier
import com.aira.health.domain.model.PredictionScenario
import com.aira.health.domain.model.WeeklyAthletePlanDraft
import com.aira.health.domain.usecase.BuildWeeklyAthletePlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val buildWeeklyAthletePlanUseCase: BuildWeeklyAthletePlanUseCase,
) : ViewModel() {

    private val _scenario = MutableStateFlow(
        ScenarioInput(
            targetDate = LocalDate.now().plusDays(1).toString(),
            sleepDeltaHours = 0f,
            trainingLoadDeltaPercent = 0f,
        )
    )

    private val _uiState = MutableStateFlow<CoachUiState>(CoachUiState.Loading)
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    private var recomputeJob: Job? = null

    init {
        scheduleRecompute(immediate = true)
    }

    fun onSleepDeltaChanged(value: Float) {
        _scenario.update {
            it.copy(
                sleepDeltaHours = value.coerceIn(
                    PredictionScenario.MIN_SLEEP_DELTA_HOURS,
                    PredictionScenario.MAX_SLEEP_DELTA_HOURS,
                )
            )
        }
        scheduleRecompute()
    }

    fun onTrainingLoadDeltaChanged(value: Float) {
        _scenario.update {
            it.copy(
                trainingLoadDeltaPercent = value.coerceIn(
                    PredictionScenario.MIN_TRAINING_LOAD_DELTA_PERCENT,
                    PredictionScenario.MAX_TRAINING_LOAD_DELTA_PERCENT,
                )
            )
        }
        scheduleRecompute()
    }

    fun onRecalculateRequested() {
        scheduleRecompute(immediate = true)
    }

    private fun scheduleRecompute(immediate: Boolean = false) {
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch {
            if (!immediate) {
                delay(RECOMPUTE_DEBOUNCE_MS)
            }
            recomputeCurrentScenario()
        }
    }

    private suspend fun recomputeCurrentScenario() {
        val scenario = _scenario.value
        setRefreshingState(scenario)

        val result = runCatching {
            buildWeeklyAthletePlanUseCase.build(
                scenario = PredictionScenario(
                    targetDate = scenario.targetDate,
                    sleepDeltaHours = scenario.sleepDeltaHours,
                    trainingLoadDeltaPercent = scenario.trainingLoadDeltaPercent,
                )
            )
        }

        _uiState.value = result.fold(
            onSuccess = { draft -> mapToReadyState(draft = draft, scenario = scenario) },
            onFailure = { throwable ->
                CoachUiState.Error(
                    message = throwable.message ?: "Unable to generate weekly coaching guidance.",
                    scenario = scenario,
                )
            },
        )
    }

    private fun setRefreshingState(scenario: ScenarioInput) {
        _uiState.update { current ->
            when (current) {
                is CoachUiState.Ready -> current.copy(
                    scenario = scenario,
                    isRefreshing = true,
                )

                is CoachUiState.Error -> current.copy(scenario = scenario)
                CoachUiState.Loading -> CoachUiState.Loading
            }
        }
    }

    private fun mapToReadyState(
        draft: WeeklyAthletePlanDraft,
        scenario: ScenarioInput,
    ): CoachUiState.Ready {
        val uncertaintyLabel = draft.uncertaintyLabel ?: if (draft.confidenceTier == PredictionConfidenceTier.LOW) {
            LOW_CONFIDENCE_LABEL
        } else {
            null
        }

        val sanitizedSummary = enforceNonDiagnosticLanguage(draft.guidanceSummary)
        val sanitizedActions = draft.priorityActions.map(::enforceNonDiagnosticLanguage)
        val sanitizedCautions = draft.cautionNotes.map(::enforceNonDiagnosticLanguage)

        return CoachUiState.Ready(
            scenario = scenario,
            projection = ProjectionCardModel(
                projectedRecoveryDelta = draft.projectedRecoveryDelta,
                projectedEnergyDelta = draft.projectedEnergyDelta,
                projectedBurnoutTier = draft.projectedBurnoutTier.name,
                projectedBurnoutTrajectory = draft.projectedBurnoutTrajectory.name,
                confidenceTier = draft.confidenceTier,
                confidenceScore = draft.confidenceScore,
                uncertaintyLabel = uncertaintyLabel,
            ),
            guidance = GuidanceCardModel(
                summary = sanitizedSummary,
                actions = sanitizedActions,
                citations = draft.citations,
                uncertaintyLabel = uncertaintyLabel,
            ),
            weeklyDraft = WeeklyDraftCardModel(
                targetDate = draft.targetDate,
                loadRecoveryBalanceSummary = enforceNonDiagnosticLanguage(draft.loadRecoveryBalanceSummary),
                weeklyFocus = enforceNonDiagnosticLanguage(draft.weeklyFocus),
                priorityActions = sanitizedActions,
                cautionNotes = sanitizedCautions,
                confidenceTier = draft.confidenceTier,
                confidenceScore = draft.confidenceScore,
                uncertaintyLabel = uncertaintyLabel,
            ),
            isRefreshing = false,
        )
    }

    private fun enforceNonDiagnosticLanguage(text: String): String {
        var value = text
        BLOCKED_DIAGNOSTIC_PATTERNS.forEach { (pattern, replacement) ->
            value = value.replace(pattern, replacement)
        }
        return value
    }

    companion object {
        private const val RECOMPUTE_DEBOUNCE_MS: Long = 350
        private const val LOW_CONFIDENCE_LABEL: String =
            "Confidence is limited; treat this as directional and non-clinical guidance."

        private val BLOCKED_DIAGNOSTIC_PATTERNS: List<Pair<Regex, String>> = listOf(
            Regex("\\bdiagnos(is|e|ed|ing)?\\b", RegexOption.IGNORE_CASE) to "assessment",
            Regex("\\bdiagnost(ic|ics)?\\b", RegexOption.IGNORE_CASE) to "clinical",
            Regex("\\bdisease(s)?\\b", RegexOption.IGNORE_CASE) to "condition trend",
            Regex("\\bsymptom(s)?\\b", RegexOption.IGNORE_CASE) to "signal",
            Regex("\\bprescri(ption|be|bed|bing)\\b", RegexOption.IGNORE_CASE) to "plan",
        )
    }
}
