---
phase: 09
slug: prediction-what-if-and-athlete-guidance
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-18
---

# Phase 09 - Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Local metrics and personalization to prediction engines | Sensitive local trend features enter what-if and burnout projections. | Aggregate scores, bounded deltas, confidence values |
| Prediction outputs to local persistence | Simulation and calibration outputs are stored for later guidance and auditability. | Aggregate projection and error metrics only |
| Guidance runtime policy to generation path | Runtime selection controls whether coaching generation remains local-only. | Runtime mode and policy decision |
| Domain plan output to Coach UI | Domain guidance/projection fields map into user-visible cards. | Summary text, actions, citations, uncertainty labels |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-09-01 | T | Scenario input bounds | mitigate | `PredictionScenario.validate()` enforces bounded sleep/load deltas; sliders and ViewModel clamp to same ranges in [app/src/main/java/com/aira/health/presentation/dashboard/coach/components/WhatIfScenarioCard.kt](app/src/main/java/com/aira/health/presentation/dashboard/coach/components/WhatIfScenarioCard.kt) and [app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt](app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt). | closed |
| T-09-02 | I | Prediction/calibration tables | mitigate | New entities store aggregate-only values in [app/src/main/java/com/aira/health/data/local/model/WhatIfSimulationResult.kt](app/src/main/java/com/aira/health/data/local/model/WhatIfSimulationResult.kt) and [app/src/main/java/com/aira/health/data/local/model/PredictionCalibrationRecord.kt](app/src/main/java/com/aira/health/data/local/model/PredictionCalibrationRecord.kt). | closed |
| T-09-03 | R | Calibration auditability | mitigate | Date-linked calibration records with timestamps are persisted in [app/src/main/java/com/aira/health/domain/usecase/RecordPredictionCalibrationUseCase.kt](app/src/main/java/com/aira/health/domain/usecase/RecordPredictionCalibrationUseCase.kt). | closed |
| T-09-04 | D | Burnout projection loop | mitigate | Burnout computation uses bounded short horizon (`takeLast(14).takeLast(7)`) and fixed computations in [app/src/main/java/com/aira/health/domain/engine/BurnoutRiskProjectionEngine.kt](app/src/main/java/com/aira/health/domain/engine/BurnoutRiskProjectionEngine.kt). | closed |
| T-09-05 | E | Schema migration path | mitigate | DB builder uses explicit migrations including `MIGRATION_09_X` and no destructive fallback in [app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt](app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt); migration coverage exists in [app/src/androidTest/java/com/aira/health/data/local/db/AiraDatabaseMigrationTest.kt](app/src/androidTest/java/com/aira/health/data/local/db/AiraDatabaseMigrationTest.kt). | closed |
| T-09-06 | S | Runtime provider identity | mitigate | Local-only policy enforced via [app/src/main/java/com/aira/health/ai/runtime/AiRuntimePolicyGuard.kt](app/src/main/java/com/aira/health/ai/runtime/AiRuntimePolicyGuard.kt) and configured in [app/src/main/java/com/aira/health/di/AiRuntimeModule.kt](app/src/main/java/com/aira/health/di/AiRuntimeModule.kt). | closed |
| T-09-07 | T | Prompt and output contract | mitigate | Prompt assembler validates citation keys and rejects unsupported claims in [app/src/main/java/com/aira/health/ai/prompt/AthleteGuidancePromptAssembler.kt](app/src/main/java/com/aira/health/ai/prompt/AthleteGuidancePromptAssembler.kt). | closed |
| T-09-08 | I | Guidance input payload | mitigate | Guidance request is aggregate-only by contract in [app/src/main/java/com/aira/health/domain/model/AthleteGuidanceRequest.kt](app/src/main/java/com/aira/health/domain/model/AthleteGuidanceRequest.kt); raw event payloads are excluded. | closed |
| T-09-09 | D | Runtime availability | mitigate | Timeout and deterministic fallback paths are implemented via runtime/orchestrator and guidance fallback services in [app/src/main/java/com/aira/health/ai/runtime/AiRuntimeGateway.kt](app/src/main/java/com/aira/health/ai/runtime/AiRuntimeGateway.kt), [app/src/main/java/com/aira/health/ai/orchestration/InferenceOrchestrator.kt](app/src/main/java/com/aira/health/ai/orchestration/InferenceOrchestrator.kt), and [app/src/main/java/com/aira/health/domain/usecase/GenerateAthleteGuidanceUseCase.kt](app/src/main/java/com/aira/health/domain/usecase/GenerateAthleteGuidanceUseCase.kt). | closed |
| T-09-10 | R | Safety policy compliance | mitigate | Non-diagnostic/uncertainty enforcement is covered by guidance and prompt tests including [app/src/test/java/com/aira/health/ai/prompt/AthleteGuidancePromptAssemblerTest.kt](app/src/test/java/com/aira/health/ai/prompt/AthleteGuidancePromptAssemblerTest.kt) and [app/src/test/java/com/aira/health/domain/usecase/GenerateAthleteGuidanceUseCaseTest.kt](app/src/test/java/com/aira/health/domain/usecase/GenerateAthleteGuidanceUseCaseTest.kt). | closed |
| T-09-11 | T | Coach state mapping | mitigate | ViewModel mapping preserves confidence and uncertainty fields in [app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt](app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt) with coverage in [app/src/test/java/com/aira/health/presentation/dashboard/coach/CoachViewModelTest.kt](app/src/test/java/com/aira/health/presentation/dashboard/coach/CoachViewModelTest.kt). | closed |
| T-09-12 | I | Card rendering content | mitigate | Coach UI models only render aggregate card fields (summary/actions/citations/confidence), not raw biometrics, in [app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachUiState.kt](app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachUiState.kt). | closed |
| T-09-13 | D | Scenario interaction loop | mitigate | Recompute jobs are cancelled/debounced and bounded in [app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt](app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt). | closed |
| T-09-14 | R | Language policy compliance | mitigate | Low-confidence uncertainty and non-diagnostic wording are enforced and tested in [app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt](app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachViewModel.kt) and [app/src/test/java/com/aira/health/presentation/dashboard/coach/CoachViewModelTest.kt](app/src/test/java/com/aira/health/presentation/dashboard/coach/CoachViewModelTest.kt). | closed |
| T-09-15 | E | Navigation scope | mitigate | Coach remains in dashboard navigation scope with explicit in-app route and Insights entry point, without privileged/remote flows, in [app/src/main/java/com/aira/health/presentation/navigation/AiraRoutes.kt](app/src/main/java/com/aira/health/presentation/navigation/AiraRoutes.kt), [app/src/main/java/com/aira/health/presentation/navigation/AiraNavHost.kt](app/src/main/java/com/aira/health/presentation/navigation/AiraNavHost.kt), and [app/src/main/java/com/aira/health/presentation/dashboard/body/BodyScreen.kt](app/src/main/java/com/aira/health/presentation/dashboard/body/BodyScreen.kt). | closed |

Status values: open, closed
Disposition values: mitigate (implementation required), accept (documented risk), transfer (third-party)

---

## Accepted Risks Log

No accepted risks.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-04-18 | 15 | 15 | 0 | GitHub Copilot |

---

## Sign-Off

- [x] All threats have a disposition (mitigate, accept, transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] threats_open: 0 confirmed
- [x] status: verified set in frontmatter

Approval: verified 2026-04-18
