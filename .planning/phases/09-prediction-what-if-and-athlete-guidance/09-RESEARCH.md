# Phase 09: Prediction, What-If, and Athlete Guidance - Research

Researched: 2026-04-18
Domain: Android Kotlin on-device prediction simulation, calibration, and athlete guidance
Overall confidence: MEDIUM

## Standard Stack

Use this stack for Phase 09 implementation, with no new external libraries unless a hard blocker appears. [VERIFIED: repository scan]

| Area                         | Use                                                                                     | Why this is standard here                                                                                                | Confidence | Evidence                                                                                                                                                                                                            |
| ---------------------------- | --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Prediction math              | Pure Kotlin engines in domain layer (`app/src/main/java/com/aira/health/domain/engine`) | Existing score engines are deterministic, testable, and Android-free; Phase 09 should follow the same seam.              | HIGH       | [VERIFIED: repository files `RecoveryEngine`, `SleepEngine`, `StrainEngine`, `StressEngine`, `EnergyBankEngine`]                                                                                                    |
| Orchestration                | Use cases in `app/src/main/java/com/aira/health/domain/usecase`                         | Existing architecture already routes business behavior through use cases, then persistence/UI.                           | HIGH       | [VERIFIED: repository files `ComputeDailyScoresUseCase`, `BaselineRecalculatorUseCase`, `UpdatePersonalizationStateUseCase`]                                                                                        |
| Local persistence            | Room entities + DAO + explicit migration in `AiraDatabase`                              | Room is the project standard for local persistence and migration; new Phase 09 tables should follow this pattern.        | HIGH       | [VERIFIED: repository `AiraDatabase`, `DatabaseModule`], [CITED: https://developer.android.com/training/data-storage/room], [CITED: https://developer.android.com/training/data-storage/room/migrating-db-versions] |
| Reactive state               | `Flow`/`StateFlow` with `stateIn(WhileSubscribed(5000))`                                | This pattern is already used throughout presentation and matches Android architecture recommendations.                   | HIGH       | [VERIFIED: repository `HomeViewModel`, `WeeklyReportViewModel`, `WhatIfViewModel`], [CITED: https://developer.android.com/topic/architecture/recommendations], [CITED: https://kotlinlang.org/docs/flow.html]       |
| Guidance generation pipeline | Existing `InferenceOrchestrator` + prompt assembler + deterministic fallback            | Reuse current safety and timeout contracts; add athlete-guidance-specific contracts rather than bypassing orchestration. | HIGH       | [VERIFIED: repository `InferenceOrchestrator`, `PromptAssembler`, `DeterministicSummaryService`]                                                                                                                    |
| Timeout and cancellation     | `withTimeout`, structured cancellation, reason-coded fallback                           | Existing orchestrator behavior already follows cancellable pattern and should remain Phase 09 baseline.                  | HIGH       | [VERIFIED: repository `InferenceOrchestrator`], [CITED: https://kotlinlang.org/docs/cancellation-and-timeouts.html]                                                                                                 |

Prescriptive decisions for Phase 09:

- Use bounded deltas (`projectedRecoveryDelta`, `projectedEnergyDelta`) plus confidence tier as first-class output contract, not absolute promises. [VERIFIED: Phase 09 context D-02]
- Use short-horizon burnout projection from recent windows (7-14 days) with tier + trajectory (`rising`, `stable`, `falling`). [VERIFIED: Phase 09 context D-04]
- Persist prediction outputs and calibration records locally in Room, then compute rolling error metrics from local tables only. [VERIFIED: Phase 09 context D-05, D-13]
- Reuse existing prompt safety policy and deterministic fallback pattern for low-confidence or runtime-failure guidance output. [VERIFIED: repository fallback/orchestration], [VERIFIED: Phase 09 context D-07, D-08]

Critical stack gap to resolve before implementation:

- Current runtime binding is cloud-backed (`GeminiCloudRuntimeGateway` + Supabase token endpoint), which conflicts with the on-device-only guidance requirement in this milestone and with Phase 09 D-14. [VERIFIED: repository `AiRuntimeModule`, `GeminiCloudRuntimeGateway`, `GeminiAuthTokenProvider`]

## Architecture Patterns

### Pattern A: Deterministic what-if projection pipeline

1. Read baseline state from local tables (`DailyMetrics`, `PersonalizationState`, `CausalInsight`). [VERIFIED: repository DAOs/entities]
2. Apply scenario deltas (sleep hours delta, training-load delta) in a pure domain engine.
3. Emit bounded next-day deltas + confidence + rationale signal keys.
4. Persist scenario and projection output in local Room table for audit and future calibration.

This keeps projection deterministic and testable under local-only constraints. [VERIFIED: repository architecture], [VERIFIED: Phase 09 D-01/D-02/D-03/D-13]

### Pattern B: Burnout projection with graceful degradation

1. Build short-horizon feature window from recent daily metrics.
2. Compute risk tier + trajectory.
3. If history is insufficient, downgrade confidence and emit low-confidence guidance payload instead of failing.

This matches existing sparse-data handling style used in score engines (`confidence` as parallel signal, no hard hide). [VERIFIED: repository engine patterns], [VERIFIED: Phase 09 D-04/D-06]

### Pattern C: Calibration loop in daily lifecycle

1. After `ComputeDailyScoresUseCase` writes observed metrics for day D, resolve prior prediction for day D.
2. Record error deltas (`predicted - observed`) in calibration table.
3. Update rolling metrics (for example MAE-style) used by UI confidence transparency.

Do not block score persistence if calibration update fails; follow best-effort resilience used by causal insight hook today. [VERIFIED: repository `ComputeDailyScoresUseCase` causal best-effort], [VERIFIED: Phase 09 D-05]

### Pattern D: Guidance generation with deterministic fallback and citation keys

1. Build guidance request from current local state + prediction outputs + causal/personalization references.
2. Generate through orchestrator with timeout and cancellation support.
3. On failure or low confidence, route to deterministic guidance service.
4. Enforce output contract with citation keys (only known local signals), uncertainty note when confidence is low, and non-diagnostic language.

This aligns with current prompt/fallback contracts and avoids fabricated causality. [VERIFIED: repository prompt/fallback/orchestration], [VERIFIED: Phase 09 D-08/D-09/D-12]

### Pattern E: Coach-surface integration without new navigation complexity

1. Introduce `CoachViewModel` state holder for scenario controls + projection + weekly draft.
2. Keep rendering inside existing coach/dashboard flow.
3. Reuse card-based UI style from existing dashboard/supplementary screens.

This respects D-10 and D-11 while reducing integration risk. [VERIFIED: existing `CoachScreen`, `HomeViewModel`, `WeeklyReportViewModel`], [VERIFIED: Phase 09 D-10/D-11]

## Don’t Hand-Roll

| Problem                             | Do not build                                        | Use instead                                                            | Why                                                                                                                                                                                                                                                                                 |
| ----------------------------------- | --------------------------------------------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Database schema upgrades            | Ad hoc SQL rewrite without migration tests          | Room `Migration` + `addMigrations` + migration tests                   | Room provides deterministic upgrade path and validation helpers; skipping this increases crash/data-loss risk. [CITED: https://developer.android.com/training/data-storage/room/migrating-db-versions]                                                                              |
| Async cancellation and timeouts     | Custom thread cancellation logic                    | Coroutines cancellation + `withTimeout` + structured exception mapping | Kotlin coroutine cancellation is cooperative and already integrated in current stack. [CITED: https://kotlinlang.org/docs/cancellation-and-timeouts.html], [VERIFIED: repository `InferenceOrchestrator`]                                                                           |
| Background reliability              | Custom scheduler/alarm logic for persistent work    | WorkManager (`CoroutineWorker` where needed)                           | WorkManager is the recommended API for reliable deferred/persistent work across restarts. [CITED: https://developer.android.com/topic/libraries/architecture/workmanager], [CITED: https://developer.android.com/topic/libraries/architecture/workmanager/advanced/coroutineworker] |
| Prompt safety and fallback          | UI-level string templates that bypass orchestration | Prompt contract/assembler + deterministic fallback services            | Existing chain already enforces timeout/failure mapping and safety tone constraints. [VERIFIED: repository prompt/fallback/orchestration]                                                                                                                                           |
| Confidence calibration transparency | One-off in-memory error stats                       | Local calibration table + rolling metric query in DAO                  | Requirement PRED-03 needs persistent predicted-vs-observed tracking. [VERIFIED: requirements PRED-03]                                                                                                                                                                               |

## Common Pitfalls

1. Predicting absolute scores instead of bounded deltas.
   - Why it fails: It overstates certainty and violates D-02.
   - Prevent it: Keep projection output centered on delta and confidence tier.
   - Confidence: HIGH. [VERIFIED: Phase 09 D-02]

2. No sparse-history degradation path.
   - Why it fails: New users or partial sync windows will fail hard or mislead.
   - Prevent it: Explicit minimum-history threshold and low-confidence fallback branch.
   - Confidence: HIGH. [VERIFIED: Phase 09 D-06], [VERIFIED: existing engine confidence pattern]

3. Calibration updates tied to UI-only flows.
   - Why it fails: Missed updates when UI is not opened.
   - Prevent it: Hook calibration in daily scoring pipeline (domain use case), not screen lifecycle.
   - Confidence: HIGH. [VERIFIED: repository score recompute lifecycle], [VERIFIED: requirements PRED-03]

4. Using `fallbackToDestructiveMigration()` in production migration path for new Phase 09 tables.
   - Why it fails: Data loss risk for stored simulation/calibration history.
   - Prevent it: Add explicit migration and migration tests before release hardening.
   - Confidence: HIGH. [VERIFIED: repository `AiraDatabase`], [CITED: https://developer.android.com/training/data-storage/room/migrating-db-versions]

5. Leaking raw biometrics into guidance prompt payloads.
   - Why it fails: Breaks privacy boundary and Phase constraints.
   - Prevent it: Keep prompt inputs aggregate-only DTOs and cite local signal keys only.
   - Confidence: HIGH. [VERIFIED: repository `PromptContract`/`PromptAssembler`], [VERIFIED: Phase 09 D-13]

6. Assuming current AI runtime is on-device when it is cloud-backed in code.
   - Why it fails: Architecture drift creates privacy/compliance mismatch in implementation plans.
   - Prevent it: Treat runtime binding as a blocking decision item in 09-02 planning.
   - Confidence: HIGH. [VERIFIED: repository `AiRuntimeModule`, `GeminiCloudRuntimeGateway`, `GeminiAuthTokenProvider`]

## Code Examples

The snippets below show implementation-shape patterns aligned to this repository architecture.

### 1) Bounded what-if projection contract and engine

```kotlin
data class PredictionScenario(
    val date: String,
    val sleepDeltaHours: Float,
    val trainingLoadDelta: Float
)

enum class ConfidenceTier { LOW, MEDIUM, HIGH }

data class PredictionProjection(
    val projectedRecoveryDelta: Int,
    val projectedEnergyDelta: Int,
    val confidenceTier: ConfidenceTier,
    val rationaleSignalKeys: List<String>
)

class WhatIfProjectionEngine {
    fun project(
        scenario: PredictionScenario,
        baselineRecovery: Int,
        baselineEnergy: Int,
        dataConfidence: Float,
        recoverySpeed: Float,
        rationaleSignalKeys: List<String>
    ): PredictionProjection {
        val rawRecoveryDelta = (scenario.sleepDeltaHours * 6f) - (scenario.trainingLoadDelta * 0.25f)
        val weightedRecoveryDelta = (rawRecoveryDelta * recoverySpeed).toInt().coerceIn(-20, 20)
        val weightedEnergyDelta = (weightedRecoveryDelta * 0.7f).toInt().coerceIn(-15, 15)

        val confidence = when {
            dataConfidence >= 0.8f -> ConfidenceTier.HIGH
            dataConfidence >= 0.5f -> ConfidenceTier.MEDIUM
            else -> ConfidenceTier.LOW
        }

        return PredictionProjection(
            projectedRecoveryDelta = weightedRecoveryDelta,
            projectedEnergyDelta = weightedEnergyDelta,
            confidenceTier = confidence,
            rationaleSignalKeys = rationaleSignalKeys.take(3)
        )
    }
}
```

Source alignment: [VERIFIED: repository engine style], [VERIFIED: Phase 09 D-01/D-02/D-03]

### 2) Calibration write on observed outcome

```kotlin
class RecordPredictionCalibrationUseCase(
    private val calibrationDao: PredictionCalibrationDao
) {
    suspend fun record(
        targetDate: String,
        predictedRecoveryDelta: Int,
        predictedEnergyDelta: Int,
        observedRecovery: Int,
        observedEnergy: Int,
        baselineRecovery: Int,
        baselineEnergy: Int
    ) {
        val observedRecoveryDelta = observedRecovery - baselineRecovery
        val observedEnergyDelta = observedEnergy - baselineEnergy

        calibrationDao.upsert(
            PredictionCalibrationRecord(
                date = targetDate,
                recoveryError = predictedRecoveryDelta - observedRecoveryDelta,
                energyError = predictedEnergyDelta - observedEnergyDelta,
                recordedAt = System.currentTimeMillis()
            )
        )
    }
}
```

Source alignment: [VERIFIED: requirements PRED-03], [VERIFIED: repository use-case and DAO pattern]

### 3) Guidance generation with low-confidence fallback

```kotlin
suspend fun generateGuidance(
    request: AthleteGuidanceRequest,
    orchestrator: InferenceOrchestrator,
    fallback: DeterministicGuidanceService
): AthleteGuidanceOutput {
    if (request.confidenceTier == ConfidenceTier.LOW) {
        return fallback.buildLowConfidenceGuidance(request)
    }

    return when (val outcome = orchestrator.run(request.snapshot).first()) {
        is InferenceOutcome.Complete -> request.toOutput(outcome.text)
        is InferenceOutcome.Fallback -> fallback.fromFallbackSummary(request, outcome.summary)
        is InferenceOutcome.Partial -> fallback.buildRuntimeInterruptedGuidance(request)
    }
}
```

Source alignment: [VERIFIED: repository `InferenceOrchestrator` + fallback pattern], [VERIFIED: Phase 09 D-07/D-08/D-12]

## Recommended Validation Checks

| Requirement              | Validation target                             | Command / check                                                                                                                                                                   | Expected                                                                   |
| ------------------------ | --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| PRED-01                  | What-if delta projection + confidence output  | `./gradlew.bat :app:testDevDebugUnitTest --tests "*WhatIfProjectionEngineTest" --tests "*RunWhatIfSimulationUseCaseTest"`                                                         | Bounded deltas, confidence tier, rationale keys present                    |
| PRED-02                  | Burnout tier + trajectory + sparse fallback   | `./gradlew.bat :app:testDevDebugUnitTest --tests "*BurnoutRiskProjectionEngineTest"`                                                                                              | Tier/trajectory computed; low-history path returns low-confidence guidance |
| PRED-03                  | Predicted vs observed calibration persistence | `./gradlew.bat :app:testDevDebugUnitTest --tests "*RecordPredictionCalibrationUseCaseTest" --tests "*ComputeDailyScoresUseCaseTest"`                                              | Calibration row written once; missing prediction does not break score run  |
| COCH-01 / COCH-02        | Daily summary and action guidance contract    | `./gradlew.bat :app:testDevDebugUnitTest --tests "*AthleteGuidancePromptAssemblerTest" --tests "*GenerateAthleteGuidanceUseCaseTest" --tests "*DeterministicGuidanceServiceTest"` | On-device contract behavior and deterministic fallback pass                |
| COCH-03                  | Weekly planning flow integration              | `./gradlew.bat :app:testDevDebugUnitTest --tests "*BuildWeeklyAthletePlanUseCaseTest" --tests "*CoachViewModelTest"`                                                              | Weekly draft combines prediction + guidance into card-ready state          |
| Phase integration safety | Compile guard after each plan wave            | `./gradlew.bat :app:compileDevDebugKotlin`                                                                                                                                        | No API or DI breakage                                                      |

Execution-environment checks for this machine:

- Java and Gradle wrapper are available (`java 23.0.2`, `Gradle 8.13`). [VERIFIED: terminal run 2026-04-18]
- `adb` command is not currently available in PATH, so connected Android instrumentation is environment-blocked until SDK platform-tools are available. [VERIFIED: terminal run 2026-04-18]
- Plan should treat device UI tests as conditional and keep unit/compile checks as mandatory gate. [VERIFIED: repository validation precedent in Phase 08]

### Sources

Primary sources:

- Repository planning and phase context files (`.planning/PROJECT.md`, `.planning/ROADMAP.md`, `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/phases/09-prediction-what-if-and-athlete-guidance/09-CONTEXT.md`). [VERIFIED: repository]
- Repository implementation baseline in domain/ai/data-local/presentation files (read during this research session). [VERIFIED: repository]
- Android architecture recommendations: https://developer.android.com/topic/architecture/recommendations [CITED]
- Room overview and migration guidance: https://developer.android.com/training/data-storage/room and https://developer.android.com/training/data-storage/room/migrating-db-versions [CITED]
- WorkManager guidance and CoroutineWorker threading: https://developer.android.com/topic/libraries/architecture/workmanager and https://developer.android.com/topic/libraries/architecture/workmanager/advanced/coroutineworker [CITED]
- Kotlin Flow and cancellation guidance: https://kotlinlang.org/docs/flow.html and https://kotlinlang.org/docs/cancellation-and-timeouts.html [CITED]
- Health Connect/Fit migration guidance: https://developer.android.com/health-and-fitness/guides/health-connect and https://developer.android.com/health-and-fitness/guides/health-connect/migrate/migration-guide [CITED]

## Plan Integration Notes

- 09-01 (prediction pipeline): Implement deterministic engines + local simulation/calibration entities and migration first, then wire calibration hook in daily scoring lifecycle. [VERIFIED: plan scaffold + this research]
- 09-02 (guidance contracts): Reuse orchestrator/fallback chain, add athlete-guidance-specific prompt/output contracts, and explicitly resolve cloud-runtime-vs-on-device gap before coding. [VERIFIED: plan scaffold + repository runtime binding]
- 09-03 (UX integration): Use a dedicated coach state holder and card-based rendering in existing coach flow; avoid introducing new navigation routes. [VERIFIED: plan scaffold + current coach/home architecture]

High-risk gaps plans must address explicitly:

1. Runtime privacy mismatch: current AI gateway is cloud-backed despite on-device requirement. [VERIFIED: repository runtime/DI]
2. Migration safety: database still uses `fallbackToDestructiveMigration`, unsafe for new prediction history tables at release quality. [VERIFIED: `AiraDatabase`]
3. Device test environment: `adb` unavailable currently, so instrumentation verification is gated by environment setup. [VERIFIED: terminal run 2026-04-18]
