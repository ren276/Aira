# Plan 03-02 — Strain, Stress & Energy Bank Engines — SUMMARY

## What Was Built

Implemented `StrainEngine`, `StressEngine`, and `EnergyBankEngine` as pure Kotlin components satisfying SCORE-03, SCORE-04, and D-05/D-06.

### StrainEngine (SCORE-04)
- Zone weights: Z1=0.5×, Z2=1×, Z3=2×, Z4=4×, Z5=8× (exponential per D-01)
- Logarithmic normalisation: `log(1 + weightedLoad) / log(1 + maxLoad) × 100`
- Reference: 60 minutes all-Zone-5 → score ≈ 100
- Confidence = fraction of zone bands with data (each zone = 20% of total)
- Null zones are excluded; remaining zones still produce a bounded, visible score

### StressEngine (SCORE-03)
- **Hourly**: HR elevation + HRV suppression vs. personal EMA baselines, equally weighted (50/50)
- **Daily**: Quadratic mean (RMS) of hourly scores — amplifies spike hours non-linearly (D-01)
  - Example: 4 hours at 90 + 20 hours at 10 → RMS ≈ 42 vs. simple mean 25
- Confidence = measured hours / 24 (expected day coverage)

### EnergyBankEngine (D-05, D-06)
- Dual output: `energyBankScore` (user-visible, 0–100) + `internalBalance` (float, persisted between days)
- Recharge: Recovery × 0.40; Depletion: Strain × 0.25 + Stress × 0.20
- Delta is damped by confidence — sparse-data days don't over-swing the balance
- Distinct from source engines: not equal to Recovery/Strain/Stress individually

### Tests
- `StrainEngineTest`: 8 tests — zero load, Zone-5-only, non-linear comparison, boundary clamp, monotonicity, null zones, all-null, determinism
- `StressEngineTest`: 9 tests — baseline hour, elevated HR+HRV, hourly bounds, daily all-calm, spike amplification, daily bounds, partial-day confidence, empty list, determinism
- `EnergyBankEngineTest`: 10 tests — high recovery, depletion, recharge vs depletion comparison, distinctness from Recovery, bounds, internal balance bounds, missing strain, all-null, determinism

## Files Created
- `app/src/main/java/com/aira/health/domain/engine/StrainEngine.kt`
- `app/src/main/java/com/aira/health/domain/engine/StressEngine.kt`
- `app/src/main/java/com/aira/health/domain/engine/EnergyBankEngine.kt`
- `app/src/test/java/com/aira/health/domain/engine/StrainEngineTest.kt`
- `app/src/test/java/com/aira/health/domain/engine/StressEngineTest.kt`
- `app/src/test/java/com/aira/health/domain/engine/EnergyBankEngineTest.kt`

## Self-Check: PASSED
- Non-linear scaling implemented and verified (Zone-5 heavy > Zone-3 heavy for same minutes)
- All scores bounded 0..100 including under extreme input values
- Energy Bank maintains visible/internal duality per D-05; isolated from source engine values per D-06
- Low-confidence days remain visible with confidence signaling per D-03, D-04, D-11, D-12
