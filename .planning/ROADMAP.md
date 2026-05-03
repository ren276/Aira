# Roadmap: Aira

## Milestones

- [x] **v1.0 milestone** - Foundational Persistence & Dashboard (Phases 1-6). Shipped 2026-04-17.
- [x] **v1.1 AI Intelligence Expansion** - On-device inference, reasoning, and continuity (Phases 7-10). Shipped 2026-04-19.
- [ ] **v1.2 Athlete Context & Wearable Deep-Dive** - Enhancing intelligence with environmental, nutrition, and gear depth (Phases 11-15).

## v1.2 Phases

### Phase 11: Aira Assistant & Contextual Intelligence [ACTIVE]

**Goal**: Implement a privacy-safe, on-device conversational assistant anchored in athlete context (Gemini Nano / Local LLM).
**Depends on**: Phase 10
**Requirements**: [ASSIST-01, ASSIST-02, CONTEXT-01]
**Success Criteria**:
1. Assistant screen provides a fluid, real-time chat experience.
2. Inference is executed entirely on-device (via AICore/MediaPipe).
3. Responses are contextually aware of current scores and athlete history.
**Plans**: 3 plans

Plans:
- [ ] 11-01-PLAN.md - Research & Prototyping: Gemini Nano vs Local LLM fallback.
- [ ] 11-02-PLAN.md - UI: Aira Assistant chat interface and navigation entry points.
- [ ] 11-03-PLAN.md - Integration: Contextual anchoring and LLM streaming service.

### Phase 12: Applied Physiological ML & Metric Personalization

**Goal**: Transition core scoring engines (Recovery, Strain, Stress, Sleep) from deterministic heuristic formulas to on-device TFLite models that learn and adapt to the user's unique physiology over time.
**Depends on**: Phase 11
**Requirements**: [MLM-01, MLM-02, MLM-03, MLM-04, MLM-05]
**Success Criteria**:
1. TFLite model files for Recovery, Strain, Stress, and Sleep are loadable at runtime.
2. A `HybridEngine` correctly blends ML prediction with heuristic fallback during cold-start.
3. `PersonalizedWeightsStore` persists learned adaptations across app restarts on-device.
4. Python `scripts/ml` directory provides full model training-to-TFLite conversion pipeline.
**Plans**: 3 plans

Plans:
- [ ] 12-01-PLAN.md — Python ML Architecture: Model definitions, feature extractors, TFLite conversion.
- [ ] 12-02-PLAN.md — Android Runtime: TFLite loader, inference classes, PersonalizedWeightsStore.
- [ ] 12-03-PLAN.md — Engine Integration: HybridEngine wiring, heuristic fallback, Hilt module.

### Phase 13: Nutrition Intelligence & Data Layer

**Goal**: Seamlessly integrate nutrition anchoring into scoring loops via advanced logging and macro-tracking.
**Depends on**: Phase 12
**Requirements**: [NUTR-01, NUTR-02, NUTR-03]
**Success Criteria**:
1. Barcode scanning and manual entry pipelines are operational.
2. Nutrient-timing guidance is generated based on training load history.
**Plans**: 3 plans

### Phase 14: Gear & Injury Correlation

**Goal**: Add environmental context (gear, weather) and physical state (injury) to the athlete physiology model.
**Depends on**: Phase 13
**Requirements**: [GEAR-01, INJ-01]
**Plans**: 2 plans

### Phase 15: Enhanced Wearable Telemetry & Hardening

**Goal**: Deepen Health Connect support for high-resolution proprietary fields and harden v1.2 features.
**Depends on**: Phase 14
**Requirements**: [WEAR-01, WEAR-02]
**Plans**: 2 plans

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 11. Aira Assistant & Contextual Intelligence | 0/3 | Active | - |
| 12. Applied Physiological ML & Metric Personalization | 0/3 | Not started | - |
| 13. Nutrition Intelligence | 0/3 | Not started | - |
| 14. Gear & Injury Correlation | 0/2 | Not started | - |
| 15. Enhanced Wearable Telemetry | 0/2 | Not started | - |
