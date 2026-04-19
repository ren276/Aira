# Phase 9: Prediction, What-If, and Athlete Guidance - Context

**Gathered:** 2026-04-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver scenario simulation and on-device athlete guidance for daily and weekly decision support.

This phase covers only:
- next-day what-if prediction for recovery and energy impact,
- short-horizon burnout-risk projection with calibration tracking,
- on-device daily/weekly guidance generation from current local state.

Out of scope for this phase:
- cloud sync, remote inference, and off-device personalization logic,
- new sensor ingestion sources,
- subscription/business workflow changes.

</domain>

<decisions>
## Implementation Decisions

### Scenario Simulator Contract
- **D-01:** What-if input supports at least sleep and training-load adjustments, each represented as explicit deltas from baseline.
- **D-02:** Predictions are shown as bounded deltas plus confidence (not absolute deterministic promises).
- **D-03:** Simulator output always includes a short rationale referencing existing causal/personalization signals (no opaque numbers only).

### Burnout Projection and Calibration
- **D-04:** Burnout risk projection uses short-horizon trend windows (recent days) and surfaces risk tier plus trajectory direction.
- **D-05:** Calibration tracking stores predicted vs observed outcomes locally and computes simple rolling error metrics for transparency.
- **D-06:** If insufficient history exists, projection degrades gracefully to low-confidence guidance instead of hard failure.

### Guidance Generation Policy
- **D-07:** Daily and weekly guidance text must run on-device through the existing local runtime/fallback chain.
- **D-08:** Guidance content must be practical and action-oriented (training, recovery, nutrition) with explicit safety tone and uncertainty language when confidence is low.
- **D-09:** Guidance must cite current state signals (for example strain/stress/recovery trends) and avoid fabricated causality.

### UX Integration Rules
- **D-10:** What-if controls and guidance surfaces should fit existing dashboard patterns before introducing new navigation complexity.
- **D-11:** Weekly planning flow must summarize projected load vs recovery balance in concise, scannable cards.
- **D-12:** All user-facing prediction language must avoid medical diagnosis framing.

### Privacy and Safety Constraints
- **D-13:** Raw biometric events remain local-only; only computed projections/guidance artifacts are persisted.
- **D-14:** No provider static keys or remote LLM fallback paths are introduced in this phase.

### the agent's Discretion
- Exact simulator control widgets and visual treatment.
- Internal feature weighting strategy for projection models.
- Prompt-template wording details as long as D-07 to D-12 are preserved.
- Data schema naming for calibration/error tracking entities.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Scope and Requirements
- `.planning/ROADMAP.md` - Phase 9 goals, dependencies, and plan boundaries.
- `.planning/REQUIREMENTS.md` - PRED-01/02/03 and COCH-01/02/03 definitions.
- `.planning/PROJECT.md` - privacy-first and on-device constraints.

### Upstream Explainability and Personalization Baseline
- `.planning/phases/08-causal-insight-and-personalization-core/08-CONTEXT.md` - locked explainability/personalization decisions.
- `.planning/phases/08-causal-insight-and-personalization-core/08-01-SUMMARY.md` - causal ranking outputs now available as prediction inputs.
- `.planning/phases/08-causal-insight-and-personalization-core/08-02-SUMMARY.md` - personalization state and correction-influence behavior.
- `.planning/phases/08-causal-insight-and-personalization-core/08-03-SUMMARY.md` - UI-level explainability and correction interaction contracts.
- `.planning/phases/08-causal-insight-and-personalization-core/08-SECURITY.md` - threat mitigations and privacy boundaries.
- `.planning/phases/08-causal-insight-and-personalization-core/08-VALIDATION.md` - current validation status and environment-gated test caveat.

### Runtime and Inference Constraints
- `.planning/phases/07-on-device-ai-runtime-foundation/07-01-SUMMARY.md` - runtime session and local inference baseline.
- `.planning/phases/07-on-device-ai-runtime-foundation/07-02-SUMMARY.md` - fallback orchestration behavior and timeout handling.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/aira/health/domain/usecase/ComputeDailyScoresUseCase.kt`: already computes burnout-risk index and daily score composites.
- `app/src/main/java/com/aira/health/domain/usecase/BaselineRecalculatorUseCase.kt`: sequential daily recomputation seam suitable for calibration updates.
- `app/src/main/java/com/aira/health/domain/usecase/ComputeCausalInsightsUseCase.kt`: causal factor outputs can power prediction rationale text.
- `app/src/main/java/com/aira/health/domain/usecase/UpdatePersonalizationStateUseCase.kt`: personalization state available for athlete-specific adjustments.
- `app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachScreen.kt`: existing coach UI surface can host phase-9 guidance upgrades.

### Established Patterns
- Domain-first use-case orchestration with Room-backed local persistence.
- Hilt-provided dependency graph for engine/use-case composition.
- Explainability contracts already expose confidence tiers and recency metadata in UI.
- Runtime safety pattern favors bounded timeouts and local fallback responses.

### Integration Points
- Add prediction/calibration engines in domain layer and persist scenario runs + error metrics locally.
- Extend coach/dashboard presentation state with simulation inputs and guidance outputs.
- Reuse existing metric-detail/explainability metadata to ground generated guidance rationale.

</code_context>

<specifics>
## Specific Ideas

- Keep prediction output phrasing in coach-like language: concise, actionable, uncertainty-aware.
- Prefer side-by-side baseline vs what-if deltas for next-day recovery and energy.
- Weekly guidance should focus on balancing load and recovery, not generic motivation text.

</specifics>

<deferred>
## Deferred Ideas

- Long-range multi-week forecasting beyond weekly planning horizon.
- Social/team coach sharing workflows.
- Cloud-backed model retraining and cohort benchmarking.

</deferred>

---

*Phase: 09-prediction-what-if-and-athlete-guidance*
*Context gathered: 2026-04-18*
