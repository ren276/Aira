# Requirements: Aira

**Defined:** 2026-04-17
**Core Value:** Empower Android users with true on-device, explainable health intelligence that learns their unique physiology without compromising privacy.

## v1 Requirements

### AI Runtime and Privacy
- [ ] **AIM-01**: App can load and run a compact on-device text model (TFLite class) for inference on supported Android devices.
- [ ] **AIM-02**: All AI inference runs fully on-device for core coaching features.
- [ ] **AIM-03**: Raw biometric records never leave local storage during AI processing and syncing.
- [ ] **AIM-04**: App falls back to deterministic non-AI summaries when model is unavailable.

### Causal Insight Engine
- [ ] **CAUS-01**: User can see ranked contributing factors for major metric changes (for example sleep, strain, caffeine/stress notes).
- [ ] **CAUS-02**: Each causal insight references real recent user data windows and not static template values.
- [ ] **CAUS-03**: Insight cards display confidence level and recency metadata.

### Prediction and What-If
- [ ] **PRED-01**: User can simulate sleep/training changes and view predicted next-day recovery and energy impact.
- [ ] **PRED-02**: App provides short-horizon burnout risk projection from recent workload patterns.
- [ ] **PRED-03**: App tracks prediction error against observed outcomes for ongoing calibration.

### Personal Physiology Adaptation
- [ ] **PPM-01**: App adapts baseline sleep need per user over time using observed outcomes.
- [ ] **PPM-02**: App adapts recovery-speed and stress-sensitivity weights from historical response patterns.
- [ ] **PPM-03**: User corrections can influence future personalization behavior.

### Athlete Guidance Generation
- [ ] **COCH-01**: App generates daily metric summary text on-device.
- [ ] **COCH-02**: App generates practical training/recovery/nutrition coaching guidance on-device.
- [ ] **COCH-03**: App generates a weekly athlete planning draft using current state and upcoming load.

### Performance and Reliability
- [ ] **PERF-01**: AI inference executes off the UI thread and is cancellable.
- [ ] **PERF-02**: AI feature latency remains within acceptable interactive budget on target devices.
- [ ] **PERF-03**: AI memory usage remains within defined mobile safety budget without app instability.

### Cloud Continuity Snapshot
- [ ] **BACK-01**: App maintains compact computed-summary snapshots in Supabase so users can restore insights after reinstall.
- [ ] **BACK-02**: App can upload a final compact summary snapshot before explicit local-account reset flows.

## v2 Requirements

### Advanced Intelligence
- **AIM-05**: Multi-turn long-context conversation memory grounded in long-term athlete history.
- **COCH-04**: Fully personalized meal plan generation with pantry/preferences/allergy constraints.
- **PRED-04**: Multi-day training block optimization with objective trade-off controls.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Cloud LLM for primary coaching | Conflicts with on-device privacy-first mandate |
| Uploading raw biometric timeseries to Supabase | Violates product trust boundary |
| Full uninstall event interception guarantees | Android platform does not provide a reliable uninstall callback |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AIM-01 | Phase 7 | Pending |
| AIM-02 | Phase 7 | Pending |
| AIM-03 | Phase 7 | Pending |
| AIM-04 | Phase 7 | Pending |
| PERF-01 | Phase 7 | Pending |
| PERF-02 | Phase 7 | Pending |
| PERF-03 | Phase 7 | Pending |
| CAUS-01 | Phase 8 | Pending |
| CAUS-02 | Phase 8 | Pending |
| CAUS-03 | Phase 8 | Pending |
| PPM-01 | Phase 8 | Pending |
| PPM-02 | Phase 8 | Pending |
| PPM-03 | Phase 8 | Pending |
| PRED-01 | Phase 9 | Pending |
| PRED-02 | Phase 9 | Pending |
| PRED-03 | Phase 9 | Pending |
| COCH-01 | Phase 9 | Pending |
| COCH-02 | Phase 9 | Pending |
| COCH-03 | Phase 9 | Pending |
| BACK-01 | Phase 10 | Pending |
| BACK-02 | Phase 10 | Pending |

**Coverage:**
- v1 requirements: 21 total
- Mapped to phases: 21
- Unmapped: 0

---
*Requirements defined: 2026-04-17*
*Last updated: 2026-04-17 after v1.1 scope definition*
