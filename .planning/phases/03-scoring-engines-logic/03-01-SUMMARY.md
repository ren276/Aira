# Plan 03-01 — Recovery & Sleep Engines — SUMMARY

## What Was Built

Implemented `RecoveryEngine` and `SleepEngine` as pure Kotlin math components satisfying SCORE-01 and SCORE-02.

### RecoveryEngine (SCORE-01)
- Weighted formula: HRV 40%, RHR 25%, Sleep 25%, prior Strain 10%
- Inputs normalised to 0.0–1.0 before weighting (HRV/RHR are baseline-relative)
- Missing inputs trigger weight renormalisation; available weights still produce a bounded 0–100 score
- Confidence emitted as a parallel `Float` (0.0–1.0) — never gates score visibility (D-03, D-04, D-11, D-12)

### SleepEngine (SCORE-02)
- Weighted formula: Duration 30%, Deep/Stage 30%, Continuity 20%, Consistency 20%
- Same renormalisation pattern as RecoveryEngine for any missing component
- Full-input days produce confidence = 1.0; single-component days produce confidence ≈ 0.3

### Tests
- `RecoveryEngineTest`: 6 tests — full-input, mid-range, boundary, missing-one, missing-many, all-null, determinism
- `SleepEngineTest`: 7 tests — full-input, mid-range, worst-case, out-of-range clamp, missing-deep, missing-many, all-null, determinism

## Key Design Decisions
- Stateless: no constructor dependencies — engines are pure functions over inputs
- Renormalisation approach preserves score meaning across partial days: a day with only Sleep data produces a score based on the full 0–100 range, not a deflated partial-weight value
- `priorStrain` in Recovery is inverted: 0 strain → 1.0 contribution; 100 strain → 0.0

## Files Created
- `app/src/main/java/com/aira/health/domain/engine/RecoveryEngine.kt`
- `app/src/main/java/com/aira/health/domain/engine/SleepEngine.kt`
- `app/src/test/java/com/aira/health/domain/engine/RecoveryEngineTest.kt`
- `app/src/test/java/com/aira/health/domain/engine/SleepEngineTest.kt`

## Self-Check: PASSED
- RecoveryEngine and SleepEngine exist as pure Kotlin (no Android imports)
- Outputs are always bounded 0..100 and deterministic
- Missing-input days produce visible scores with reduced confidence (not null, not 0)
- All D-03, D-04, D-11, D-12 rules implemented in both engines
