---
plan: 09-03
phase: 09-prediction-what-if-and-athlete-guidance
status: complete
completed_at: 2026-04-18
commit: pending
---

# 09-03 Summary: Coach Weekly Planning UX Integration

## What Was Built

Integrated Phase 09 prediction and guidance outputs into the Coach experience using an in-flow card layout, scenario controls, and confidence-aware weekly planning presentation.

### key-files.created

- app/src/main/java/com/aira/health/domain/model/WeeklyAthletePlanDraft.kt - Domain contract for card-ready weekly planning output.
- app/src/main/java/com/aira/health/domain/usecase/BuildWeeklyAthletePlanUseCase.kt - Composes simulation output and guidance output into weekly planning draft sections.
- app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachUiState.kt - Dedicated Coach state models for scenario, projection, guidance, and weekly draft cards.
- app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt - Scenario interaction orchestration, debounced recompute, and policy-safe UI mapping.
- app/src/main/java/com/aira/health/presentation/dashboard/coach/components/WhatIfScenarioCard.kt - Scenario control card with sliders and recalculate action.
- app/src/main/java/com/aira/health/presentation/dashboard/coach/components/PredictionProjectionCard.kt - Projection card for recovery/energy deltas, burnout outlook, and confidence metadata.
- app/src/main/java/com/aira/health/presentation/dashboard/coach/components/GuidanceNarrativeCard.kt - Guidance summary/action/citation card with uncertainty messaging.
- app/src/main/java/com/aira/health/presentation/dashboard/coach/components/WeeklyPlanDraftCard.kt - Weekly planning draft card for load/recovery summary, priorities, and cautions.
- app/src/test/java/com/aira/health/domain/usecase/BuildWeeklyAthletePlanUseCaseTest.kt
- app/src/test/java/com/aira/health/presentation/dashboard/coach/CoachViewModelTest.kt
- app/src/androidTest/java/com/aira/health/presentation/dashboard/coach/CoachWeeklyPlanningUiTest.kt

### key-files.modified

- app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachScreen.kt - Replaced legacy chat-style coach surface with card-based weekly planning flow powered by CoachViewModel.

## Requirements Covered

| Req     | Coverage                                                                                                                                                       |
| ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| COCH-03 | Coach screen now supports what-if controls, prediction projection display, guidance narrative, and weekly plan draft rendering within existing dashboard flow. |

## Verification

1. ./gradlew.bat :app:testDevDebugUnitTest --tests "*BuildWeeklyAthletePlanUseCaseTest" --tests "*CoachViewModelTest"

- Result: PASS

2. ./gradlew.bat :app:compileDevDebugKotlin

- Result: PASS

3. ./gradlew.bat :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.dashboard.coach.CoachWeeklyPlanningUiTest

- Result: BLOCKED (adb unavailable in current environment)

## Notes

- Coach view-model mapping sanitizes diagnostic-style language tokens in rendered guidance/caution text and preserves low-confidence uncertainty metadata.
- Scenario interaction is debounced to avoid recompute storms while still supporting explicit recalculate.

## Self-Check: PASSED (with device-test environment limitation documented)
