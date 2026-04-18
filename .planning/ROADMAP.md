# Roadmap: Aira

## Milestones

- [x] **v1.0 milestone** - Phases 1 to 6 (including inserted 04.1) shipped on 2026-04-17. See `.planning/milestones/v1.0-ROADMAP.md`.
- [ ] **v1.1 AI Intelligence Expansion** - Phases 7 to 10 planned.

## v1.1 Phases

### Phase 7: On-Device AI Runtime Foundation

**Goal**: Integrate a compact private local inference runtime that can safely generate text without impacting app responsiveness.
**Depends on**: Phase 6
**Requirements**: [AIM-01, AIM-02, AIM-03, AIM-04, PERF-01, PERF-02, PERF-03]
**Success Criteria**:
1. Local model loads and executes on supported devices without blocking the UI thread.
2. Fallback output path works when model is unavailable.
3. Runtime and memory budgets are measured and within agreed limits.
**Plans**: 3 plans

Plans:
- [ ] 07-01-PLAN.md - Runtime integration, model loading, and dependency wiring.
- [ ] 07-02-PLAN.md - Inference service API, fallback pathway, and safety wrappers.
- [ ] 07-03-PLAN.md - Performance benchmarking, tuning, and regression gates.

### Phase 8: Causal Insight and Personalization Core

**Goal**: Build explainability and adaptation engines that convert user telemetry history into trustworthy individualized reasoning.
**Depends on**: Phase 7
**Requirements**: [CAUS-01, CAUS-02, CAUS-03, PPM-01, PPM-02, PPM-03]
**Success Criteria**:
1. Causal explanations cite real user factors with confidence and recency metadata.
2. Personalization parameters update from observed outcomes over time.
3. User correction loop affects future explanations.
**Plans**: 3 plans

Plans:
- [ ] 08-01-PLAN.md - Causal feature extraction and factor ranking pipeline.
- [ ] 08-02-PLAN.md - Personal physiology adaptation model and persistence.
- [ ] 08-03-PLAN.md - UI surfacing for explainability confidence and user corrections.

### Phase 9: Prediction, What-If, and Athlete Guidance

**Goal**: Deliver scenario simulation and on-device athlete guidance generation for daily and weekly decision support.
**Depends on**: Phase 8
**Requirements**: [PRED-01, PRED-02, PRED-03, COCH-01, COCH-02, COCH-03]
**Success Criteria**:
1. What-if simulations provide coherent next-day impact projections.
2. Burnout risk projection and calibration tracking are operational.
3. Daily/weekly athlete guidance text is generated on-device from current state.
**Plans**: 3 plans

Plans:
- [ ] 09-01-PLAN.md - Prediction model pipeline and scenario simulator.
- [ ] 09-02-PLAN.md - Athlete guidance generation contracts and prompt strategy.
- [ ] 09-03-PLAN.md - UX integration for coach, summaries, and weekly planning flows.

### Phase 10: Cloud Continuity Snapshot and Milestone Hardening

**Goal**: Enable privacy-safe cloud continuity snapshots and complete end-to-end hardening for v1.1 release readiness.
**Depends on**: Phase 9
**Requirements**: [BACK-01, BACK-02]
**Success Criteria**:
1. Compact computed-summary snapshots sync to Supabase and restore cleanly after reinstall.
2. Explicit local reset flow performs final snapshot upload before wipe.
3. End-to-end validation confirms privacy boundaries and stability of all AI features.
**Plans**: 2 plans

Plans:
- [ ] 10-01-PLAN.md - Supabase snapshot schema, write/read restore, and lifecycle integration.
- [ ] 10-02-PLAN.md - Security, privacy, and release hardening verification for v1.1.

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 7. On-Device AI Runtime Foundation | 0/3 | Not started | - |
| 8. Causal Insight and Personalization Core | 0/3 | Not started | - |
| 9. Prediction, What-If, and Athlete Guidance | 0/3 | Not started | - |
| 10. Cloud Continuity Snapshot and Milestone Hardening | 0/2 | Not started | - |
