# Phase 8: Causal Insight and Personalization Core - Context

**Gathered:** 2026-04-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Build explainability and adaptation engines that convert user telemetry history into trustworthy individualized reasoning.

This phase covers three scoped outcomes only:
- ranked causal factors tied to recent user data windows,
- confidence plus recency metadata on insight outputs,
- personalization updates and user-correction feedback loops that affect future explanations.

</domain>

<decisions>
## Implementation Decisions

### Causal Factor Model
- **D-01:** Insight cards show the top 3 weighted contributing factors.
- **D-02:** Each shown factor includes directional impact and contribution weight.

### Confidence and Recency Rules
- **D-03:** Insight confidence uses tiered labels with exact recency text.
- **D-04:** Confidence thresholds are fixed for this phase: High >= 0.75, Medium 0.40-0.74, Low < 0.40.
- **D-05:** Recency is displayed as explicit data-window text (for example, last 7d), not implicit freshness.

### Personalization Update Policy
- **D-06:** Personalization parameters update daily using bounded EMA updates.
- **D-07:** Adaptation guardrails: minimum 7 days of data before updates, with max +/-3% daily parameter change.

### User Correction Behavior
- **D-08:** User corrections influence future personalization using weighted decay over 14 days.
- **D-09:** Correction influence is capped at 20% to prevent single-event overfitting.

### the agent's Discretion
- Tie-break handling when factor weights are nearly equal.
- Final display phrasing for confidence/recency labels.
- Internal persistence schema details for adaptation deltas and correction decay state.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Scope and Requirements
- `.planning/ROADMAP.md` - Phase 8 goal, success criteria, and plan boundaries.
- `.planning/REQUIREMENTS.md` - CAUS-01/02/03 and PPM-01/02/03 requirement definitions.
- `.planning/PROJECT.md` - milestone constraints and privacy-first scope guardrails.

### Upstream Runtime Foundation
- `.planning/phases/07-on-device-ai-runtime-foundation/07-01-SUMMARY.md` - runtime contract and gateway baseline.
- `.planning/phases/07-on-device-ai-runtime-foundation/07-02-SUMMARY.md` - prompt/fallback orchestration and safety boundaries.
- `.planning/phases/07-on-device-ai-runtime-foundation/07-AI-SPEC.md` - phase AI contract and quality expectations.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/aira/health/domain/usecase/BaselineRecalculatorUseCase.kt`: existing sequential baseline update pipeline that can host adaptation updates.
- `app/src/main/java/com/aira/health/data/local/model/UserCorrection.kt`: existing correction entity with confidence delta fields.
- `app/src/main/java/com/aira/health/data/local/model/DailyMetrics.kt`: stores data confidence and score-level aggregates.
- `app/src/main/java/com/aira/health/ai/orchestration/InferenceOrchestrator.kt`: centralized inference/fallback execution seam for future explainability generation.

### Established Patterns
- Domain-first contracts with data implementations behind Hilt modules.
- Room + DAO persistence with explicit use-case orchestration for state transitions.
- Confidence is already modeled as a 0..1 signal in existing engines (sleep/strain/stress/recovery).

### Integration Points
- Add causal ranking pipeline inside domain use cases, then persist outputs to local entities for UI consumption.
- Extend personalization update path from baseline recalculation and correction records.
- Surface insight confidence/recency through presentation layer models that consume Room flows.

</code_context>

<specifics>
## Specific Ideas

- Keep causal cards compact and decision-oriented: top 3 only, with clear directional effect.
- Favor understandable confidence labels over raw numeric-only UX.
- Keep adaptation stable by default; avoid overreacting to short-lived spikes.

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope.

</deferred>

---

*Phase: 08-causal-insight-and-personalization-core*
*Context gathered: 2026-04-18*
