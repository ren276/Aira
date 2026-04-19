---
plan: 09-02
phase: 09-prediction-what-if-and-athlete-guidance
status: complete
completed_at: 2026-04-18
commit: pending
---

# 09-02 Summary: Guidance Runtime Policy and On-Device Coaching Output

## What Was Built

Implemented the coaching guidance runtime policy gate, on-device guidance contracts, prompt assembly, deterministic fallback behavior, and regression tests for privacy and language safety.

### key-files.created

- app/src/main/java/com/aira/health/ai/runtime/AiRuntimePolicy.kt - Runtime policy contract for local-only coaching enforcement.
- app/src/main/java/com/aira/health/ai/runtime/AiRuntimePolicyGuard.kt - Policy guard that enforces runtime compliance and fallback routing decisions.
- app/src/main/java/com/aira/health/domain/model/AthleteGuidanceRequest.kt - Guidance input contract from local state and prediction context.
- app/src/main/java/com/aira/health/domain/model/AthleteGuidanceOutput.kt - Guidance output contract carrying summary, actions, confidence, and citation keys.
- app/src/main/java/com/aira/health/ai/prompt/AthleteGuidancePromptContract.kt - Prompt contract tailored for actionable athlete coaching generation.
- app/src/main/java/com/aira/health/ai/prompt/AthleteGuidancePromptAssembler.kt - Guidance prompt assembly with grounded citation and uncertainty policy behavior.
- app/src/main/java/com/aira/health/ai/fallback/DeterministicGuidanceService.kt - Deterministic local fallback guidance for blocked runtime/low-confidence paths.
- app/src/main/java/com/aira/health/domain/usecase/GenerateDailyAthleteSummaryUseCase.kt - Daily summary generation orchestration.
- app/src/main/java/com/aira/health/domain/usecase/GenerateActionGuidanceUseCase.kt - Action guidance generation orchestration.
- app/src/main/java/com/aira/health/domain/usecase/GenerateAthleteGuidanceUseCase.kt - Top-level guidance use case integrating policy, runtime orchestration, and fallback.
- app/src/test/java/com/aira/health/di/AiRuntimePolicyGuardTest.kt
- app/src/test/java/com/aira/health/ai/prompt/AthleteGuidancePromptAssemblerTest.kt
- app/src/test/java/com/aira/health/domain/usecase/GenerateAthleteGuidanceUseCaseTest.kt
- app/src/test/java/com/aira/health/ai/fallback/DeterministicGuidanceServiceTest.kt

### key-files.modified

- app/src/main/java/com/aira/health/ai/runtime/AiRuntimeGateway.kt - Extended runtime metadata needed for policy-aware guidance execution.
- app/src/main/java/com/aira/health/di/AiRuntimeModule.kt - Wired runtime policy guard and guidance-related providers.

## Requirements Covered

| Req     | Coverage                                                                                                                                  |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| COCH-01 | Daily guidance summary generation is available through local runtime orchestration with deterministic fallback behavior.                  |
| COCH-02 | Practical action guidance generation is grounded in local signals, confidence-aware, and safety-constrained for low-confidence scenarios. |

## Verification

1. ./gradlew.bat :app:testDevDebugUnitTest --tests "\*AiRuntimePolicyGuardTest"

- Result: PASS

2. ./gradlew.bat :app:testDevDebugUnitTest --tests "*AthleteGuidancePromptAssemblerTest" --tests "*GenerateAthleteGuidanceUseCaseTest" --tests "\*DeterministicGuidanceServiceTest"

- Result: PASS

3. ./gradlew.bat :app:compileDevDebugKotlin

- Result: PASS

4. ./gradlew.bat :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.dashboard.coach.CoachWeeklyPlanningUiTest

- Result: BLOCKED (adb unavailable in current environment)

## Notes

- Runtime policy is now explicit for coaching generation so disallowed runtime paths can be blocked and routed to deterministic local fallback.
- Guidance contracts preserve confidence and uncertainty semantics while avoiding unsupported/fabricated causal language.

## Self-Check: PASSED (with device-test environment limitation documented)
