# Plan 03-03 — Baseline, Orchestration & Worker Wiring — SUMMARY

## What Was Built

Completed the baseline and orchestration layer connecting all scoring engines into a production pipeline.

### EmaEngine (SCORE-05, D-08)
- Stateless two-phase updater: expanding flat average for samples 1–7, then EMA smoothing
- Cold-start completes at exactly sample 7 (`coldStartComplete` transitions to `true`)
- Stateless by design: caller provides `(previousValue, sampleCount, coldStartComplete)` and receives the next state — enabling deterministic backfill replay (D-02)

### BaselineRecalculatorUseCase (D-02, D-07)
- Sequential per-day EMA recomputation from a given `fromDate` through `toDate`
- Covers 13 metric keys: `hrv_rmssd`, `rhr`, `sleep_duration_min`, `sleep_efficiency`, plus all score baselines (recovery, sleep, strain, stress, energy bank, readiness-to-learn, nutrition, burnout risk, composite readiness)
- Designed for backfill: calling `recomputeFrom("2026-01-03", today)` replays EMA from day 3 forward, correcting all subsequent baselines

### ComputeDailyScoresUseCase (D-03, D-04, D-09, D-10, D-11, D-12)
- Orchestrates all 5 engines (Recovery, Sleep, Strain, Stress, EnergyBank)
- Computes all 10 `DailyMetrics` score columns per run, including `compositeReadiness`, `readinessToLearnScore`, `burnoutRiskIndex`, `nutritionScore` (D-09, D-10)
- Composite formulas:
  - `compositeReadiness` = Recovery×50% + Sleep×30% + (100−Stress)×20%
  - `readinessToLearnScore` = compositeReadiness − stress penalty above 60
  - `burnoutRiskIndex` = average of load factor and energy depletion factor, 0.0–1.0
- Aggregated `dataConfidence` reflects all engine confidences; never gates score visibility (D-03, D-04, D-11, D-12)

### DAO Updates
- `DailyMetricsDao`: added `getPreviousDay(date)` — reads the prior row for cross-day Strain/EnergyBank state
- `BaselineDao`: existing `get()` and `upsert()` used as-is; no schema changes required

### HealthSyncWorker Wiring (Task 3)
- Injected `ComputeDailyScoresUseCase` via `@AssistedInject`
- `doWork()` now runs ingest → compute sequentially in one worker execution
- Retry/backoff semantics unchanged

### Tests
- `EmaEngineTest`: 6 tests — first sample, 7-sample cold-start, transition at exactly sample 7, EMA formula verification, convergence over 50 samples, determinism
- `BaselineRecalculatorUseCaseTest`: 4 tests — 5-sample incomplete cold-start, completed at 7 samples, backfill calls ≥16 upserts, metric set includes both input and score baselines
- `ComputeDailyScoresUseCaseTest`: 4 tests — full-shape persistence, missing HRV confidence reduction, all-null still persists, composite readiness bounded

## Files Created / Modified
| Action | File |
|--------|------|
| NEW | `domain/engine/EmaEngine.kt` |
| NEW | `domain/usecase/BaselineRecalculatorUseCase.kt` |
| NEW | `domain/usecase/ComputeDailyScoresUseCase.kt` |
| MOD | `data/local/dao/DailyMetricsDao.kt` (added `getPreviousDay`) |
| MOD | `data/worker/HealthSyncWorker.kt` (injected `ComputeDailyScoresUseCase`) |
| NEW | `test/.../engine/EmaEngineTest.kt` |
| NEW | `test/.../usecase/BaselineRecalculatorUseCaseTest.kt` |
| NEW | `test/.../usecase/ComputeDailyScoresUseCaseTest.kt` |

## Self-Check: PASSED
- EMA cold-start at 7 samples, EMA thereafter — deterministic across replay
- Backfill recomputes all subsequent days (not just the inserted date)
- Full DailyMetrics shape populated including nutrition/readiness/burnout/composite (D-09, D-10)
- Low-confidence / sparse data days remain visible; confidence is a parallel signal never a gate (D-03, D-04, D-11, D-12)
- Worker pipeline: ingest succeeds → compute runs → DailyMetrics upserted
