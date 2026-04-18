# Aira

## What This Is

A privacy-first, on-device health intelligence app for Android that turns wearable and phone signals into explainable daily guidance. v1.0 shipped the data, scoring, UI, and Strava foundations. v1.1 focuses on private on-device AI that explains, predicts, and adapts for athletes without sending raw biometrics to the cloud.

## Core Value

Empower Android users with true on-device, explainable health intelligence that learns their unique physiology without compromising privacy.

## Current Milestone: v1.1 AI Intelligence Expansion

**Goal:** Deliver a lightweight private AI layer that can explain metric changes, simulate outcomes, and generate practical athlete guidance inside the app.

**Target features:**
- Causal insight engine that explains why metrics changed.
- Prediction and what-if simulator for recovery, energy, and burnout risk.
- Personal physiology adaptation model with feedback loop.
- On-device generated athlete outputs (daily summary, coach guidance, meal/recovery/training planning).
- Compact Supabase cloud continuity snapshot for reinstall recovery without raw biome upload.

## Requirements

### Validated

- [x] Android local-first health data ingestion and persistence shipped in v1.0.
- [x] Core scoring and baseline pipeline shipped in v1.0.
- [x] Production app shell and major UI surfaces shipped in v1.0.
- [x] Strava onboarding plus ingestion pipeline shipped in v1.0.

### Active

- [ ] Integrate a small on-device text generation model (TFLite class) with predictable latency and memory bounds.
- [ ] Generate causal metric explanations from real user telemetry and journal context.
- [ ] Add what-if simulation for sleep/training scenarios and next-day impact.
- [ ] Add adaptive physiology personalization for sleep need, recovery speed, and stress sensitivity.
- [ ] Generate athlete-facing summaries and actionable coach plans fully on-device.
- [ ] Keep cloud persistence limited to compact computed summaries and settings only.

### Out of Scope

- Cloud-hosted LLM inference for core coaching features - violates privacy-first product promise.
- Exporting raw health biometrics to Supabase - explicitly disallowed.
- iOS parity milestone work - deferred until Android AI milestone stabilizes.

## Context

- v1.0 was archived on 2026-04-17 and tagged as v1.0.
- Next milestone is AI-product depth, not platform rewrite.
- User priority is free/private local AI first, with small models and no app-performance regression.
- Existing stack already supports local-first pipelines (Room, WorkManager, Health Connect, Compose).

## Constraints

- **Privacy/Security**: Raw health data must remain on-device; only compact computed summaries may sync.
- **Performance**: AI inference must not block UI thread or degrade baseline app responsiveness.
- **Model Footprint**: Prefer compact quantized models that fit realistic Android memory budgets.
- **Tech Stack**: Stay in Kotlin/Android native stack with current project architecture.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| On-device small TFLite model for text generation | Keeps AI private and free to use while fitting mobile constraints | - Pending |
| Causal/prediction/personalization over generic chatbot scope | Matches athlete value and retention goals | - Pending |
| Compact Supabase continuity snapshots only | Enables reinstall recovery without raw biometrics upload | - Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition**:
1. Requirements invalidated? -> Move to Out of Scope with reason
2. Requirements validated? -> Move to Validated with phase reference
3. New requirements emerged? -> Add to Active
4. Decisions to log? -> Add to Key Decisions
5. "What This Is" still accurate? -> Update if drifted

**After each milestone**:
1. Full review of all sections
2. Core Value check - still the right priority?
3. Audit Out of Scope - reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-17 after starting v1.1 milestone*
