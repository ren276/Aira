# Requirements: Aira

**Defined:** 2026-04-15
**Core Value:** Empower Android users with true on-device, explainable health intelligence that learns their unique physiology safely without cloud biometric processing.

## v1 Requirements

### Environment & Permissions
- [ ] **ENV-01**: Setup project for minSdk 29, targetSdk 35 with Kotlin 2.0+
- [ ] **ENV-02**: Implement multi-batch permission request flow (Core, Body, Advanced)
- [ ] **ENV-03**: Prompt Android 10-13 users without Health Connect to install from Play Store
- [ ] **ENV-04**: Secure app using Biometric Prompt App Lock and map FLAG_SECURE to sensitive screens

### Persistence & Auth
- [ ] **DB-01**: Configure Room Database schemas annotated for 10 entities
- [ ] **DB-02**: Apply SQLCipher encryption to Room Database initialized from Android Keystore keys
- [ ] **AUTH-01**: Implement Supabase Auth (Google SignIn, Email/Pass, Anonymous Guest)
- [ ] **SYNC-01**: Configure secure HTTPS + Certificate Pinning for Supabase telemetry endpoints

### Data Extraction
- [x] **DATA-01**: Implement HealthConnect Repository extracting Sleep, HR, HRV, SpO2, Calories
- [x] **DATA-02**: Implement GoogleFit Fallback Repository matching HealthConnect fields
- [x] **DATA-03**: Implement wearable source detection mapping `packageName` to confidence weights
- [x] **DATA-04**: Configure WorkManager periodic sync (15m charging, 30m idle limit)

### Math & Scoring Engine
- [ ] **SCORE-01**: Recovery Engine (40% HRV, 25% RHR, 25% Sleep, 10% Prior Strain)
- [ ] **SCORE-02**: Sleep Engine (30% Duration, 30% Stage, 20% Continuity, 20% Consistency)
- [ ] **SCORE-03**: Hourly Stress Engine (real-time rolling calculation) & Energy Bank 
- [ ] **SCORE-04**: Strain Engine aggregating HR time-in-zones (1-5 scaled weights)
- [ ] **SCORE-05**: EMA Baseline Engine (handles 7-day cold start flat averages)

### UI / Presentation
- [x] **UI-01**: Light/Dark/OLED Themes with Custom Canvas Score Arcs
- [ ] **UI-02**: Home Dashboard with 2x2 Score Grid + Causal Insight Card
- [ ] **UI-03**: Detail Screens for Recovery, Strain, Sleep, Stress with explanation bottom sheets
- [x] **UI-04**: Health Monitor real-time vitals strip component
- [x] **UI-05**: Basic Strength Builder (manual sets/reps logging)
- [x] **UI-06**: Basic Nutrition Logger (barcode scanner integration + manual fields)

## v2 Requirements

### Intelligence & Adapting
- **AI-01**: Load Gemma 4 2B (e2b) model locally via MediaPipe GenAI Tasks
- **AI-02**: Aira AI Conversational Coach UI with context generation prompts
- **AI-03**: Generate weekly Longitudinal Body Narrative
- **ML-01**: TFLite PersonalisedScoreAdjuster processing User Corrections

### Features & Billing
- **FEAT-01**: Biological / Trends Charting (3/7/14/30/90 days)
- **FEAT-02**: Cardio Load (ATL/CTL/TSB tracking)
- **FEAT-03**: Sleep Debt ledger and target payoff generator
- **NOTF-01**: Adaptive Smart Notifications tracking open/dismiss times
- **BILL-01**: Integrate RevenueCat via Google Play Billing for Aira Pro paywall isolation

## Out of Scope

| Feature | Reason |
|---------|--------|
| Medical Diagnosis | Explicitly a wellness tool, uncertified for medical interpretation |
| Cloud Inference | Raw biometric data cannot exit the device to ensure "zero-trust" physiological privacy |
| iOS Platform | Completely divergent API requirements (HealthKit) against Health Connect core focus |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ENV-01 | Phase 1 | Pending |
| ENV-02 | Phase 1 | Pending |
| ENV-03 | Phase 1 | Pending |
| ENV-04 | Phase 1 | Pending |
| DB-01 | Phase 1 | Pending |
| DB-02 | Phase 1 | Pending |
| AUTH-01 | Phase 1 | Pending |
| SYNC-01 | Phase 1 | Pending |
| DATA-01 | Phase 1 | Complete |
| DATA-02 | Phase 1 | Complete |
| DATA-03 | Phase 1 | Complete |
| DATA-04 | Phase 1 | Complete |
| SCORE-01 | Phase 1 | Pending |
| SCORE-02 | Phase 1 | Pending |
| SCORE-03 | Phase 1 | Pending |
| SCORE-04 | Phase 1 | Pending |
| SCORE-05 | Phase 1 | Pending |
| UI-01 | Phase 1 | Complete |
| UI-02 | Phase 1 | Pending |
| UI-03 | Phase 1 | Pending |
| UI-04 | Phase 1 | Complete |
| UI-05 | Phase 1 | Complete |
| UI-06 | Phase 1 | Complete |
| AI-01 | Phase 2 | Pending |
| AI-02 | Phase 2 | Pending |
| AI-03 | Phase 2 | Pending |
| ML-01 | Phase 2 | Pending |
| FEAT-01 | Phase 2 | Pending |
| FEAT-02 | Phase 2 | Pending |
| FEAT-03 | Phase 2 | Pending |
| NOTF-01 | Phase 2 | Pending |
| BILL-01 | Phase 2 | Pending |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 32 total
- Unmapped: 0

---
*Requirements defined: 2026-04-15*
*Last updated: 2026-04-15 after initial definition*
