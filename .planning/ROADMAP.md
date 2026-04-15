# Roadmap: Aira

## Overview

Aira will be built in four coarse phases for v1, starting with the secure on-device persistence and cloud sync layer, moving upward into the data extraction pipeline from wearables, followed by defining the core pure-mathematical scoring engines, and concluding with a meticulously crafted Compose UI.

## Phases

- [x] **Phase 1: Environment & Persistence** - Set up Jetpack Compose, Supabase Auth, and SQLCipher encrypted Room DB.
- [x] **Phase 2: Data Ingestion** - Integrate Health Connect Client, Google Fit Fallback, and WorkManager background sync.
- [ ] **Phase 3: Scoring Engines & Logic** - Implement mathematical equations for all 8 scores and EMA baselines.
- [ ] **Phase 4: User Interface & Dashboards** - Build the visual application, charts, score explanations, and insights.

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

- [ ] 03-01-PLAN.md — Recovery and Sleep engine math with confidence-aware partial inputs
- [ ] 03-02-PLAN.md — Strain, Stress, and Energy Bank non-linear engine models
- [ ] 03-03-PLAN.md — EMA baseline recalculation, full DailyMetrics compute, and worker wiring

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
- [ ] 04-05-PLAN.md - Strength Builder UI flow: quick-add first, optional deep-edit, and historical edit/delete
- [ ] 04-06-PLAN.md - Nutrition UI flow: quick-add first, barcode or manual entry, optional deep-edit, and historical edit/delete
- [x] 04-07-PLAN.md - App shell navigation and smart deep-link routing
- [x] 04-08-PLAN.md - Train or Nutrition repository wiring and scanner dependency baseline
- [ ] 04-09-PLAN.md - Recovery/Strain/Sleep/Stress full-detail screens and interaction tests

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4

| Phase                          | Plans Complete | Status      | Completed  |
| ------------------------------ | -------------- | ----------- | ---------- |
| 1. Environment & Persistence   | 3/3            | Complete    | 2026-04-15 |
| 2. Data Ingestion              | 3/3            | Complete    | 2026-04-15 |
| 3. Scoring Engines & Logic     | 0/3            | Not started | -          |
| 4. User Interface & Dashboards | 0/9            | Not started | -          |
