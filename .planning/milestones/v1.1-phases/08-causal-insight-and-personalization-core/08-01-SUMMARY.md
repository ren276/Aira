---
phase: 08-causal-insight-and-personalization-core
plan: 01
status: completed
completed_at: 2026-04-18
requirements: [CAUS-01, CAUS-02]
---

# Phase 08 Plan 01 Summary

Implemented deterministic causal ranking and local persistence so daily scoring now emits top-3 explainability factors per metric from recent local windows.

## What Was Built

- Added ranked causal contracts:
  - `CausalFactor` with direction, weight, window label, and source timestamp.
  - `CausalInsightSnapshot` for per-metric/day output.
- Added `CausalRankingEngine` with:
  - normalization to `[0,1]`,
  - deterministic sorting,
  - tie-break policy for near-equal weights (`abs(delta) < 0.03`): recency desc, then key asc.
- Added local Room persistence for explainability:
  - `CausalInsight` entity (`causal_insights`),
  - `CausalInsightDao` (`upsert`, latest-by-metric, date-range reads),
  - Room migration `4 -> 5`, DAO wiring in `AiraDatabase` and `DatabaseModule`.
- Added `ComputeCausalInsightsUseCase`:
  - consumes 24h/72h/7d local windows from `DailyMetrics`,
  - computes per-metric ranked factors (`recovery`, `sleep`, `strain`, `stress`),
  - persists one row per metric/date with top-3 factors.
- Wired causal computation into `ComputeDailyScoresUseCase` post-upsert hook with safe error containment.

## Verification

Commands executed:

1. `./gradlew.bat :app:testDevDebugUnitTest --tests "*CausalRankingEngineTest" --tests "*ComputeCausalInsightsUseCaseTest"`

- Result: PASS

2. `./gradlew.bat :app:testDevDebugUnitTest --tests "*ComputeDailyScoresUseCaseTest"`

- Result: PASS

3. `./gradlew.bat :app:compileDevDebugKotlin`

- Result: PASS

## Tests Added/Updated

- Added `CausalRankingEngineTest`:
  - top-3 selection,
  - direction/weight contract,
  - deterministic tie-break rule.
- Added `ComputeCausalInsightsUseCaseTest`:
  - one persisted row per metric/date,
  - persisted window labels/timestamps,
  - no static-template fallback factors with available evidence.
- Updated `ComputeDailyScoresUseCaseTest`:
  - verifies causal update is invoked after score persistence,
  - verifies causal failure is contained and does not break score persistence.

## Deviations

- Requested execution branch was `gsd/phase-07-on-device-ai-runtime-foundation`, but checkout was blocked by pre-existing local modifications in `.planning/STATE.md`.
- To preserve unrelated planning edits, implementation was completed on current branch `gsd/phase-08-causal-insight-and-personalization-core`.

## Known Stubs

- None.
