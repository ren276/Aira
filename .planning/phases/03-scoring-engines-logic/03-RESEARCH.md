# Phase 3: Scoring Engines & Logic - Research

## Objective
Research how to implement the math models for Recovery, Sleep, Hourly Stress, Strain, and EMA Baseline Engines based on defined inputs and scoring rules.

## Core Inputs & Engine Concepts

1. **Recovery Engine (SCORE-01)**
   - Inputs: HRV (40%), RHR (25%), Sleep Score (25%), Prior Strain (10%).
   - Mapping: HRV requires historical comparison (needs EMA baseline). RHR is inverted (lower is better, also needs EMA). Sleep is a direct 0-100 score. Strain from the previous day directly reduces recovery based on its exponential scale.
   
2. **Sleep Engine (SCORE-02)**
   - Inputs: Duration (30%), Stage/Deep (30%), Continuity (20%), Consistency (20%).
   - Mapping: These require comparison against age-based standard baselines or user-specific EMA (sleep duration goal).

3. **Strain & Stress Engines (SCORE-03 & SCORE-04)**
   - As per `03-CONTEXT.md` D-01, both Strain and Stress will use exponential non-linear scaling.
   - Strain: based on HR time-in-zones (1-5), where Zone 5 accumulation rapidly drives Strain closer to 100.
   - Stress: Hourly rolling calculation (likely derived from HRV + HR vs RHR).

4. **EMA Baseline Engine (SCORE-05)**
   - 7-day cold-start rule for flat averages.
   - Per contextual D-02: backfilling recalculates subsequent days.
   - Must cover the full score set, not just inputs.

5. **Energy Bank (SCORE-03 + CONTEXT D-05/D-06)**
   - Hybrid output: internal depletion state + user-visible score. Derived from Recovery (starting pool) minus Stress/Strain (depletion).

## Existing Integration Points
According to the context:
- Pure Kotlin under `domain/` for logic. No Android dependencies.
- Expected to read/write `DailyMetrics` and `Baseline` entities.
- Low confidence tracking must be mapped to the `dataConfidence` column of `DailyMetrics`.

## Validation Architecture
- Unit testing pure mathematical functions (TDD recommended for this phase as algorithms are discrete).
- Must have tests for exponential boundary logic (`0 <= score <= 100`).
- Must verify that `EMA` updates gracefully handle "cold-start" periods and backfill recalculations.

## Recommended Implementation Plan
- Create the core math models in a `domain/engine` package: `RecoveryEngine`, `StrainEngine`, `SleepEngine`, `StressEngine`, `EnergyBankEngine`, `EmaEngine`.
- Build the `BaselineRecalculatorUseCase` to fetch historical days and sequentially apply EMA rules for backfilled data.
- Integrate into a daily computation worker: `ComputeDailyScoresUseCase`.
- Use D-03/D-04 logic to construct `DailyMetrics` rows even if inputs are partial, appending confidence weights.
