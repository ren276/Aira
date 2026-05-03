# Requirements: Aira

**Defined:** 2026-04-17
**Core Value:** Empower Android users with true on-device, explainable health intelligence that learns their unique physiology without compromising privacy.

## v1 Requirements

### AI Runtime and Privacy
- [x] **AIM-01**: App can load and run a compact on-device text model (TFLite class) for inference on supported Android devices.
- [x] **AIM-02**: All AI inference runs fully on-device for core coaching features.
- [x] **AIM-03**: Raw biometric records never leave local storage during AI processing and syncing.
- [x] **AIM-04**: App falls back to deterministic non-AI summaries when model is unavailable.

### Causal Insight Engine
- [x] **CAUS-01**: User can see ranked contributing factors for major metric changes (for example sleep, strain, caffeine/stress notes).
- [x] **CAUS-02**: Each causal insight references real recent user data windows and not static template values.
- [x] **CAUS-03**: Insight cards display confidence level and recency metadata.

### Prediction and What-If
- [x] **PRED-01**: User can simulate sleep/training changes and view predicted next-day recovery and energy impact.
- [x] **PRED-02**: App provides short-horizon burnout risk projection from recent workload patterns.
- [x] **PRED-03**: App tracks prediction error against observed outcomes for ongoing calibration.

### Personal Physiology Adaptation
- [x] **PPM-01**: App adapts baseline sleep need per user over time using observed outcomes.
- [x] **PPM-02**: App adapts recovery-speed and stress-sensitivity weights from historical response patterns.
- [x] **PPM-03**: User corrections can influence future personalization behavior.

### Athlete Guidance Generation
- [x] **COCH-01**: App generates daily metric summary text on-device.
- [x] **COCH-02**: App generates practical training/recovery/nutrition coaching guidance on-device.
- [x] **COCH-03**: App generates a weekly athlete planning draft using current state and upcoming load.

### Performance and Reliability
- [x] **PERF-01**: AI inference executes off the UI thread and is cancellable.
- [x] **PERF-02**: AI feature latency remains within acceptable interactive budget on target devices.
- [x] **PERF-03**: AI memory usage remains within defined mobile safety budget without app instability.

### Cloud Continuity Snapshot
- [x] **BACK-01**: App maintains compact computed-summary snapshots in Supabase so users can restore insights after reinstall.
- [x] **BACK-02**: App can upload a final compact summary snapshot before explicit local-account reset flows.

## v1.2 Requirements (Active)

### Aira Assistant
- [ ] **ASSIST-01**: Implement a fluid, real-time chat interface for direct Q&A with the Aira intelligence.
- [ ] **ASSIST-02**: Assistant uses on-device Gemini Nano (AICore) or local LLM fallback for privacy.
- [ ] **CONTEXT-01**: Assistant automatically anchors conversations in the user's recent telemetry context.

### ML-Driven Metric Personalization
- [ ] **MLM-01**: Define a local feature extraction pipeline that reads biometric trends (HRV, RHR, sleep stages, zone minutes) from Room DB and formats them as tensors for model input.
- [ ] **MLM-02**: Implement TFLite inference classes for Recovery, Strain, Stress, and Sleep Score metrics.
- [ ] **MLM-03**: Build a `HybridEngine` for each metric that blends ML prediction with the existing heuristic fallback, gated on data availability (≥14 days history required to activate ML mode).
- [ ] **MLM-04**: Store learned/personalized per-user model weight deltas on-device (internal storage, AES-encrypted) to allow the models to adapt without data leaving the device.
- [ ] **MLM-05**: Create a Python `scripts/ml` toolkit with model definitions (scikit-learn/TensorFlow), feature extraction scripts, and a TFLite conversion pipeline for all four metric models.

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
| AIM-01 | Phase 7 | COMPLETED |
| AIM-02 | Phase 7 | COMPLETED |
| AIM-03 | Phase 7 | COMPLETED |
| AIM-04 | Phase 7 | COMPLETED |
| PERF-01 | Phase 7 | COMPLETED |
| PERF-02 | Phase 7 | COMPLETED |
| PERF-03 | Phase 7 | COMPLETED |
| CAUS-01 | Phase 8 | COMPLETED |
| CAUS-02 | Phase 8 | COMPLETED |
| CAUS-03 | Phase 8 | COMPLETED |
| PPM-01 | Phase 8 | COMPLETED |
| PPM-02 | Phase 8 | COMPLETED |
| PPM-03 | Phase 8 | COMPLETED |
| PRED-01 | Phase 9 | COMPLETED |
| PRED-02 | Phase 9 | COMPLETED |
| PRED-03 | Phase 9 | COMPLETED |
| COCH-01 | Phase 9 | COMPLETED |
| COCH-02 | Phase 9 | COMPLETED |
| COCH-03 | Phase 9 | COMPLETED |
| BACK-01 | Phase 10 | COMPLETED |
| BACK-02 | Phase 10 | COMPLETED |
| ASSIST-01 | Phase 11 | ACTIVE |
| ASSIST-02 | Phase 11 | ACTIVE |
| CONTEXT-01 | Phase 11 | ACTIVE |
| MLM-01 | Phase 12 | NOT STARTED |
| MLM-02 | Phase 12 | NOT STARTED |
| MLM-03 | Phase 12 | NOT STARTED |
| MLM-04 | Phase 12 | NOT STARTED |
| MLM-05 | Phase 12 | NOT STARTED |

**Coverage:**
- v1 requirements: 21 total (21 COMPLETED)
- v1.2 requirements: 8 total (3 ACTIVE, 5 NOT STARTED)
- Mapped to phases: 29
- Unmapped: 0

---
*Requirements defined: 2026-04-17*
*Last updated: 2026-04-19 after v1.2 transition*
