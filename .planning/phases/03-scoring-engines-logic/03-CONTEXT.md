# Phase 3: Scoring Engines & Logic - Context

**Gathered:** 2026-04-15
**Status:** Ready for planning

<domain>
## Phase Boundary

Build the core mathematical engine layer that turns local health inputs into daily and intraday scores for recovery, sleep, strain, stress, energy bank, readiness-to-learn, nutrition, burnout risk, and composite readiness. This phase also establishes per-metric EMA baselines and confidence-aware scoring behavior. It does not change the ingestion pipeline or source selection rules from Phase 2.
</domain>

<decisions>
## Implementation Decisions

### Algorithm Scaling
- **D-01:** Use exponential/non-linear scaling for Strain and Stress scores to push extreme high days closer to 100, heavily weighting them to reflect physiological limits.

### Historical Recalculation
- **D-02:** When backfilling past data, recalculate all subsequent days' EMA baselines instead of just updating the backfilled day. This ensures all baselines and scores accurately reflect true history.

### Score visibility and missing inputs
- **D-03:** Show scores even when some inputs are missing, but mark them with lower confidence instead of suppressing them.
- **D-04:** Missing inputs should not force the engine to hide the score; the engine should prefer partial output plus confidence signaling.

### Energy bank and engine coupling
- **D-05:** Energy Bank should be a hybrid output: visible to users as a public score, but also maintained as a separate internal depletion/recharge state.
- **D-06:** Strain, Stress, and Recovery should remain distinct engine outputs, with Energy Bank derived from their interaction rather than replacing them.

### EMA baselines and cold start
- **D-07:** Phase 3 should maintain EMA baselines for all scores and supporting inputs, not just the core input metrics.
- **D-08:** Baselines should still start from the existing 7-day cold-start rule, but the resulting baseline model needs to cover the full score set.

### Extra DailyMetrics outputs
- **D-09:** Compute all DailyMetrics fields now, including nutritionScore, readinessToLearnScore, burnoutRiskIndex, and compositeReadiness.
- **D-10:** These fields are first-class Phase 3 outputs, not placeholders.

### Confidence handling
- **D-11:** Low-confidence days should always show a score instead of being excluded or suppressed.
- **D-12:** Confidence is a parallel signal used for explanation and trust, not a gate that suppresses the score.

### Agent's Discretion
None - user selected specific directions for all discussed areas.
</decisions>

<canonical_refs>

## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project and requirements

- [.planning/PROJECT.md](../../PROJECT.md) - Core value, privacy constraints, and v1/v2 scope
- [.planning/REQUIREMENTS.md](../../REQUIREMENTS.md) - SCORE-01 through SCORE-05 plus supporting v1 requirements

### Prior phase decisions

- [.planning/phases/01-environment-persistence/01-CONTEXT.md](../01-environment-persistence/01-CONTEXT.md) - Local-first Room SSOT, optional biometric app lock, flavor strategy
- [.planning/phases/02-data-ingestion/02-CONTEXT.md](../02-data-ingestion/02-CONTEXT.md) - 14-day backfill, highest-confidence source wins, pure gaps handling
- [.planning/phases/02-data-ingestion/02-RESEARCH.md](../02-data-ingestion/02-RESEARCH.md) - Repository/source selection, confidence map, and WorkManager ingestion assumptions

### Codebase maps

- [.planning/codebase/ARCHITECTURE.md](../../codebase/ARCHITECTURE.md) - Layer boundaries, current data flow, and where scoring code will integrate
- [.planning/codebase/STRUCTURE.md](../../codebase/STRUCTURE.md) - Directory layout and where to place new domain/data code
- [.planning/codebase/CONVENTIONS.md](../../codebase/CONVENTIONS.md) - Naming, layering, and error-handling patterns
- [.planning/codebase/STACK.md](../../codebase/STACK.md) - Dependency versions and framework constraints
- [.planning/codebase/CONCERNS.md](../../codebase/CONCERNS.md) - Known brittle areas and testing gaps that affect score work
- [.planning/codebase/INTEGRATIONS.md](../../codebase/INTEGRATIONS.md) - Integration touchpoints for local persistence, sync, and support services
- [.planning/codebase/TESTING.md](../../codebase/TESTING.md) - Current testing patterns and gaps relevant to score engines
</canonical_refs>

<code_context>

## Existing Code Insights

### Reusable Assets

- DailyMetrics entity: already contains score columns for recovery, sleep, strain, stress, energy bank, readiness-to-learn, nutrition, burnout risk, composite readiness, and data confidence.
- Baseline entity: already stores a metric name, EMA alpha, sample count, and cold-start state.
- ConfidenceRouter: already maps package names to confidence weights and provides a preferred-source helper.
- IngestHealthDataUseCase: already persists confidence-aware daily metrics inputs and sync timestamps.

### Established Patterns

- Domain logic stays in pure Kotlin under domain/ and should avoid Android imports.
- Data and persistence concerns belong in data/ and local Room entities/DAOs.
- Small focused use cases are preferred for orchestration and persistence coordination.
- Existing code uses runCatching at SDK boundaries and keeps overlap resolution deterministic.

### Integration Points

- New scoring engines should likely live under domain/usecase or a dedicated domain/model package, with Room persistence in data/local.
- Baseline updates will need to read the ingested samples and write back to Baseline and DailyMetrics.
- Any confidence-aware score output will need to line up with the existing DailyMetrics.dataConfidence column and the current data source routing behavior.
</code_context>

<specifics>
## Specific Ideas

- Strain and Stress should scale non-linearly to emphasize high-stress physiology.
- Backfilled historic data must trigger a recalculation downstream for EMA validity.
- The user wants low-confidence days to remain visible rather than disappear.
- Energy Bank should not be purely hidden internals; it should have a visible user-facing meaning too.
- Phase 3 should populate the full DailyMetrics shape that already exists in the schema.
</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope.
</deferred>
