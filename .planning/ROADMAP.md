# Roadmap: Aira

## Overview

Aira will be built in four coarse phases for v1, starting with the secure on-device persistence and cloud sync layer, moving upward into the data extraction pipeline from wearables, followed by defining the core pure-mathematical scoring engines, and concluding with a meticulously crafted Compose UI.

## Phases

- [ ] **Phase 1: Environment & Persistence** - Set up Jetpack Compose, Supabase Auth, and SQLCipher encrypted Room DB.
- [ ] **Phase 2: Data Ingestion** - Integrate Health Connect Client, Google Fit Fallback, and WorkManager background sync.
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
- [ ] 02-01: [Implement Health Connect Client Data Mapping]
- [ ] 02-02: [Implement Google Fit Fallback integration]
- [ ] 02-03: [Set up WorkManager HealthSyncWorker and Source Parsers]

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
- [ ] 03-01: [Build Recovery & Sleep Mathematics Models]
- [ ] 03-02: [Build Strain, Stress & Energy Bank Models]
- [ ] 03-03: [Build EMA Baseline algorithms and Confidence Aggregation]

### Phase 4: User Interface & Dashboards
**Goal**: Surface the calculated insights via performant, beautiful Jetpack Compose screens matching the provided designs.
**Depends on**: Phase 3
**Requirements**: [UI-01, UI-02, UI-03, UI-04, UI-05, UI-06]
**Success Criteria** (what must be TRUE):
  1. Concentric Ring architectures animate perfectly dynamically.
  2. The Causal Anomaly card correctly interprets domain logic issues.
  3. Navigation through Main Tabs functions efficiently.
**Plans**: 4 plans

Plans:
- [ ] 04-01: [Implement Global Theming, Canvas Score Rings & Atoms]
- [ ] 04-02: [Build Home Dashboard with 2x2 grid and Insights card]
- [ ] 04-03: [Build Metric Detail & Bottom Sheet screens]
- [ ] 04-04: [Build basic Strength Builder & Nutrition Loggers]

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Environment & Persistence | 0/3 | Not started | - |
| 2. Data Ingestion | 0/3 | Not started | - |
| 3. Scoring Engines & Logic | 0/3 | Not started | - |
| 4. User Interface & Dashboards | 0/4 | Not started | - |
