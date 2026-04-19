---
plan: 08-03
phase: 08-causal-insight-and-personalization-core
status: complete
completed_at: 2026-04-18
commit: pending
---

# 08-03 Summary: Explainability UI and Correction Flow

## What Was Built

Implemented the user-facing explainability surfaces and correction interaction flow, including confidence tiers, explicit recency labels, ranked-factor rendering, and correction preview/confirmation behavior.

### key-files.created

- `app/src/main/java/com/aira/health/domain/usecase/ApplyUserCorrectionFeedbackUseCase.kt` - Stable domain entrypoint for correction submission from presentation layer.
- `app/src/androidTest/java/com/aira/health/presentation/dashboard/details/ExplainabilityUiTest.kt` - Instrumentation test suite for explainability/correction UI behavior.

### key-files.modified

- `app/src/main/java/com/aira/health/presentation/dashboard/details/MetricDetailUiState.kt` - Expanded success-state contract to carry ranked factors, confidence tier, and recency window metadata.
- `app/src/main/java/com/aira/health/presentation/dashboard/details/MetricDetailViewModel.kt` - Added mapping logic for fixed confidence thresholds and explainability metadata projection.
- `app/src/main/java/com/aira/health/presentation/common/components/ConfidenceMetaRow.kt` - Updated confidence/recency metadata rendering for detail surfaces.
- `app/src/main/java/com/aira/health/presentation/dashboard/details/components/FactorBreakdownCard.kt` - Updated to render deterministic top-3 factor rows with direction/weight details.
- `app/src/main/java/com/aira/health/presentation/dashboard/details/RecoveryDetailScreen.kt`
- `app/src/main/java/com/aira/health/presentation/dashboard/details/SleepDetailScreen.kt`
- `app/src/main/java/com/aira/health/presentation/dashboard/details/StrainDetailScreen.kt`
- `app/src/main/java/com/aira/health/presentation/dashboard/details/StressDetailScreen.kt`
  - Wired screens to new explainability UI contracts.
- `app/src/main/java/com/aira/health/presentation/supplementary/DataCorrectionsViewModel.kt` - Added preview/confirmation/submission state machine for correction impact flow.
- `app/src/main/java/com/aira/health/presentation/supplementary/DataCorrectionsScreen.kt` - Added correction target chips, preview card, confirmation gate, and result messaging UI.
- `app/src/test/java/com/aira/health/presentation/dashboard/details/MetricDetailViewModelTest.kt` - Updated/added assertions for confidence tiers, recency labels, and factor contracts.
- `app/src/test/java/com/aira/health/presentation/supplementary/DataCorrectionsViewModelTest.kt` - Added coverage for preview, confirmation requirement, and success/error state transitions.

## Requirements Covered

| Req | Coverage |
|-----|----------|
| CAUS-03 | Explainability cards now surface confidence tiers and explicit recency metadata in metric detail UI. |
| PPM-03 | User correction flow now provides preview + confirmation and submits updates through a dedicated domain use case. |

## Verification

Commands executed:

1. `./gradlew.bat :app:testDevDebugUnitTest --tests "*MetricDetailViewModelTest" --tests "*DataCorrectionsViewModelTest"`
- Result: PASS

2. `./gradlew.bat :app:compileDevDebugKotlin`
- Result: PASS

3. `./gradlew.bat :app:connectedDevDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.dashboard.details.ExplainabilityUiTest'`
- Result: BLOCKED (no connected devices in current environment)

## Notes

- The Android instrumentation suite is implemented and wired, but device execution requires an attached/emulated device.

## Self-Check: PASSED (with device-test environment limitation documented)
