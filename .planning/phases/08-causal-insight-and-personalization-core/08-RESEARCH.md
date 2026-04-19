# Phase 8: Causal Insight and Personalization Core - Research

**Researched:** 2026-04-18
**Domain:** Explainable health-factor attribution and adaptive personalization on Android
**Confidence:** Medium

## Scope Lock

Phase 8 must satisfy only these requirements:
- CAUS-01: ranked contributing factors
- CAUS-02: factors tied to recent user windows
- CAUS-03: confidence and recency metadata in insight cards
- PPM-01: baseline sleep need adaptation
- PPM-02: recovery/stress sensitivity adaptation
- PPM-03: user corrections influence future personalization

Out-of-scope for this phase:
- multi-day what-if simulation (Phase 9)
- full coaching generation expansion (Phase 9)
- cloud continuity snapshot flows (Phase 10)

## Locked Product Decisions From Context

- Insight cards show top 3 weighted factors with directional impact.
- Confidence tiers are fixed: High >= 0.75, Medium 0.40-0.74, Low < 0.40.
- Recency is shown explicitly as data-window text (for example, last 7d).
- Personalization updates are daily EMA-style with guardrails:
  - no updates before 7 days of usable data
  - max +/-3% daily parameter delta
- User corrections decay over 14 days and are capped at 20% influence.

## Existing Code and Integration Seams

Likely reuse points from current codebase:
- Baseline recalculation orchestration in `BaselineRecalculatorUseCase`.
- Correction entities in `UserCorrection` and related DAOs.
- Aggregate confidence/score persistence in `DailyMetrics`.
- AI output orchestration seam in `InferenceOrchestrator`.

Recommended boundary split:
- Domain layer: feature extraction, weighting, adaptation math, correction blending.
- Data layer: Room entities and DAOs for factors, personalization state, correction influence state.
- Presentation layer: view models map persisted outputs to confidence/recency UI contracts.

## Recommended Technical Approach

### 1) Causal Ranking Pipeline

Inputs per metric family:
- rolling windows (24h, 72h, 7d)
- baseline deltas (z-score or bounded relative delta)
- note-derived or event-derived signals already normalized

Ranking approach:
- generate candidate factors with signed effect (+/-)
- normalize each factor to bounded contribution score in [0, 1]
- combine with recency weighting so stale factors are down-weighted
- output top 3 factors with deterministic tie-breakers

Tie-break order recommendation:
1. higher absolute contribution
2. more recent evidence window
3. stable factor priority list

### 2) Confidence and Recency Computation

Confidence recommendation:
- build from signal completeness, consistency across windows, and model stability
- keep output scalar in [0, 1], then map to tier labels using locked thresholds

Recency recommendation:
- include exact window string based on latest contributing data
- always store machine-readable timestamp + display text

### 3) Personalization Adaptation Engine

State parameters:
- baseline sleep need
- recovery-speed coefficient
- stress-sensitivity coefficient

Daily update skeleton:
- if usable history days < 7: skip update
- compute daily residual between expected and observed outcomes
- update via bounded EMA
- clamp per-parameter change to +/-3% per day

Illustrative update:
- EMA update: theta_t = alpha * obs_t + (1 - alpha) * theta_(t-1)
- bounded delta: delta_t = clamp(theta_t - theta_(t-1), -0.03 * theta_(t-1), +0.03 * theta_(t-1))

### 4) User Correction Influence

Correction ingestion:
- map correction to target parameter(s)
- compute correction weight with time decay over 14 days
- cap net correction contribution at 20%

Decay recommendation:
- exponential decay over 14 days (or equivalent discrete decay table)
- keep correction provenance in persistence for explainability audits

## Persistence and Data Contract Recommendations

Add local entities for phase outputs:
- causal_factors_daily: metric date, factor key, direction, weight, recency window, confidence
- personalization_state: parameter snapshots, update timestamp, source stats
- correction_influence_state: active weighted corrections and decay metadata

DAO expectations:
- atomic write of factor rankings + confidence metadata per recalculation cycle
- transactional update for personalization parameters and correction influence state
- read APIs optimized for dashboard and details surfaces

## Risks and Mitigations

- Overfitting from sparse signals
  - mitigate with 7-day minimum data gate and bounded updates
- Factor instability day-to-day
  - mitigate with smoothing and deterministic tie-breakers
- Correction abuse or one-off noise
  - mitigate with 20% cap and decay window
- Explainability drift between domain and UI wording
  - mitigate with shared presentation mappers and contract tests

## Test Strategy

Unit tests:
- factor extraction and ranking determinism
- confidence-tier mapping thresholds
- recency text formatting and source window correctness
- EMA bounded update math
- correction decay and 20% cap behavior

Integration tests:
- Room persistence read/write for factor/personalization/correction entities
- end-to-end use case from input aggregates to persisted outputs
- orchestrator consumes stored explainability metadata consistently

Instrumentation/UI tests:
- insight cards show exactly 3 factors
- confidence label and recency text appear for each insight
- correction action updates subsequent personalization outputs

## Implementation Sequence Recommendation

- 08-01: implement feature extraction, ranking, confidence/recency derivation, and factor persistence
- 08-02: implement adaptation state model, daily bounded updates, correction blend pipeline
- 08-03: implement UI mapping and correction flows using persisted contracts from 08-01 and 08-02

## Validation Architecture

- Keep verification incremental per plan to avoid late-stage debugging.
- Fast checks after each task: focused unit tests + module compile.
- Full checks after each plan: full unit suite plus targeted instrumentation paths.
- Block merge on deterministic ranking stability, threshold correctness, and bounded update invariants.

Suggested commands:
- quick: `./gradlew :app:testDevDebugUnitTest --tests "*Causal*" --tests "*Personalization*"`
- full: `./gradlew :app:testDevDebugUnitTest :app:compileDevDebugKotlin :app:connectedDevDebugAndroidTest`
