# Roadmap: Aira

## Overview

Aira will be built in four coarse phases for v1, starting with the secure on-device persistence and cloud sync layer, moving upward into the data extraction pipeline from wearables, followed by defining the core pure-mathematical scoring engines, and concluding with a meticulously crafted Compose UI.

## Phases

- [x] **Phase 1: Environment & Persistence** - Set up Jetpack Compose, Supabase Auth, and SQLCipher encrypted Room DB.
- [x] **Phase 2: Data Ingestion** - Integrate Health Connect Client, Google Fit Fallback, and WorkManager background sync.
- [x] **Phase 3: Scoring Engines & Logic** - Implement mathematical equations for all 8 scores and EMA baselines.
- [x] **Phase 4: User Interface & Dashboards** - Build the visual application, charts, score explanations, and insights.

## Phase Details

### Phase 1: Environment & Persistence

**Goal**: Establish the foundational Android workspace, permissions, encrypted local database, and remote syncing hooks.
**Depends on**: Nothing (first phase)
**Requirements**: [ENV-01, ENV-02, ENV-03, ENV-04, DB-01, DB-02, AUTH-01, SYNC-01]
**Success Criteria** (what must be TRUE):

1. The app successfully requests permission batches for biometric data.
2. Users can create accounts via Supabase (or log in as anonymous guests).
3. The local database spins up using SQLCipher and Android Keystore cryptography.
   **Plans**: 3 plans

Plans:

- [ ] 01-01: [Setup Project, minSdk, Dependencies, CI config]
- [ ] 01-02: [Implement Room DB with SQLCipher & Keystore]
- [ ] 01-03: [Integrate Supabase Auth and User Session Management]

### Phase 2: Data Ingestion

**Goal**: Hook the application strictly into Health Connect and Google Fit to extract robust biometric and sleep metrics asynchronously.
**Depends on**: Phase 1
**Requirements**: [DATA-01, DATA-02, DATA-03, DATA-04]
**Success Criteria** (what must be TRUE):

1. Background WorkManager periodically reads Health Connect data reliably.
2. Google Fit gracefully wraps endpoints for Android 10-13 fallback contexts.
3. Wearable data origins are mapped to confidence weights.
   **Plans**: 3 plans

Plans:

- [x] 02-01: [Implement Health Connect Client Data Mapping]
- [x] 02-02: [Implement Google Fit Fallback integration]
- [x] 02-03: [Set up WorkManager HealthSyncWorker and Source Parsers]

### Phase 3: Scoring Engines & Logic

**Goal**: Build the mathematical domain models that derive Aira's insights (Recovery, Strain, Stress, Sleep, etc.) from raw data.
**Depends on**: Phase 2
**Requirements**: [SCORE-01, SCORE-02, SCORE-03, SCORE-04, SCORE-05]
**Success Criteria** (what must be TRUE):

1. Mathematical engines compute values within standard boundaries (e.g. 0-100).
2. EMA engines correctly adjust weights when digesting new days.
3. Cold starts successfully compute provisional averages.
   **Plans**: 3 plans

Plans:

- [x] 03-01-PLAN.md — Recovery and Sleep engine math with confidence-aware partial inputs
- [x] 03-02-PLAN.md — Strain, Stress, and Energy Bank non-linear engine models
- [x] 03-03-PLAN.md — EMA baseline recalculation, full DailyMetrics compute, and worker wiring

### Phase 4: User Interface & Dashboards

**Goal**: Surface the calculated insights via performant, beautiful Jetpack Compose screens matching the provided designs.
**Depends on**: Phase 3
**Requirements**: [UI-01, UI-02, UI-03, UI-04, UI-05, UI-06]
**Success Criteria** (what must be TRUE):

1. Concentric Ring architectures animate perfectly dynamically.
2. The Causal Anomaly card correctly interprets domain logic issues.
3. Navigation through Main Tabs functions efficiently.
   **Plans**: 9 plans

Plans:

- [x] 04-01-PLAN.md - Global theming and reusable score or vitals atoms
- [x] 04-02-PLAN.md - Home dashboard with fixed 2x2 card grid, local-first refresh, and always-on Causal Anomaly card
- [x] 04-03-PLAN.md - Metric detail routing and explanation sheet contracts
- [x] 04-04-PLAN.md - Train and Nutrition DAO plus Room persistence prerequisites
- [x] 04-05-PLAN.md - Strength Builder UI flow: quick-add first, optional deep-edit, and historical edit/delete
- [x] 04-06-PLAN.md - Nutrition UI flow: quick-add first, barcode or manual entry, optional deep-edit, and historical edit/delete
- [x] 04-07-PLAN.md - App shell navigation and smart deep-link routing
- [x] 04-08-PLAN.md - Train or Nutrition repository wiring and scanner dependency baseline
- [x] 04-09-PLAN.md - Recovery/Strain/Sleep/Stress full-detail screens and interaction tests

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6

| Phase                          | Plans Complete | Status   | Completed  |
| ------------------------------ | -------------- | -------- | ---------- |
| 1. Environment & Persistence   | 3/3            | Complete | 2026-04-15 |
| 2. Data Ingestion              | 3/3            | Complete | 2026-04-15 |
| 3. Scoring Engines & Logic     | 3/3            | Complete | 2026-04-15 |
| 4. User Interface & Dashboards | 9/9            | Complete | 2026-04-16 |

### Phase 04.1: Design-Faithful UI Implementation — all 18 screens pixel-perfect from designs folder (INSERTED)

**Goal:** Complete a fidelity-and-integrity rework of all intended Phase 04 surfaces so UI behavior and styling match root design artifacts while production runtime data remains repository-backed and verifiable.
**Requirements**: [UI-01, UI-02, UI-03, UI-04, UI-05, UI-06]
**Depends on:** Phase 4
**Plans:** 10 plans

Plans:

- [x] 04.1-01-PLAN.md — Wave 1: unit harness repair and no-runtime-mock-data contract test scaffold
- [x] 04.1-02-PLAN.md — Wave 1: androidTest harness repair for Home/Detail/Nav contracts
- [x] 04.1-03-PLAN.md — Wave 2: navigation IA alignment and deep-link route hardening
- [x] 04.1-04-PLAN.md — Wave 2: runtime Light/OLED theme selection and app-entry gate safety
- [x] 04.1-05-PLAN.md — Wave 3: Home fidelity rework with fixed clinical 2x2 and anomaly contract
- [x] 04.1-06-PLAN.md — Wave 3: detail route semantic correction and explanation-sheet trigger flow
- [x] 04.1-07-PLAN.md — Wave 3: Train/Nutrition/Coach data-integrity cleanup and test alignment
- [x] 04.1-08-PLAN.md — Wave 4: supplementary predictions/corrections/what-if data-binding hardening
- [x] 04.1-09-PLAN.md — Wave 4: settings/privacy runtime-state binding and data-confidence surface
- [x] 04.1-10-PLAN.md — Wave 5: final UAT replay, validation evidence, and global hardening gates

### Phase 5: Runtime Data Wiring & UI Alignment

**Goal**: Rewire the current app so every existing UI surface reflects real local data, explicit empty states, and semantically correct navigation behavior.
**Depends on**: Phase 4
**Requirements**: [UI-01, UI-02, UI-03, UI-04, UI-05, UI-06]
**Success Criteria** (what must be TRUE):

1. The visible IA, route graph, and deep-link behavior all resolve to real destinations.
2. Home, detail, Train, Nutrition, supplementary, and settings surfaces consume truthful runtime state.
3. Any missing-data or deferred capability state is explicit rather than silently faked.
   **Plans**: 1 plan

Plans:

- [ ] 05-01-PLAN.md — Runtime data wiring and UI alignment blueprint

### Phase 6: Strava API onboarding integration and daily activity ingestion into Aira metrics with Room and Health Connect persistence

**Goal:** Add mandatory Strava onboarding connection, full-history plus incremental activity ingestion, and local-first persistence that feeds Aira daily metrics.
**Requirements**: [DATA-01, DATA-04, SYNC-01, UI-02, UI-03]
**Depends on:** Phase 5
**Plans:** 1/1 plans complete

**Success Criteria** (what must be TRUE):

1. Users must connect Strava in onboarding before entering permission batching.
2. First sync backfills all available Strava activities, then transitions to idempotent incremental sync.
3. Activities persist to Room and influence existing daily metrics pipelines, with optional Health Connect mirror under user control.
4. Rate-limit and token-expiry handling is resilient (401, 403, 429, 5xx) with safe retries and cursor integrity.

Plans:

- [x] 06-01-PLAN.md — Strava onboarding, OAuth lifecycle, full-history backfill, incremental sync, and metrics persistence
