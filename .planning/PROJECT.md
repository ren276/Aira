# Aira

## What This Is

A privacy-first, on-device health intelligence app for Android that turns wearable and phone signals into explainable daily guidance. v1.1 delivered the on-device AI Intelligence Expansion (inference, reasoning, and continuity). v1.2 focuses on Athlete Context & Wearable Deep-Dive, enriching the data landscape with nutrition, gear, and advanced telemetry.

## Core Value

Empower Android users with true on-device, explainable health intelligence that learns their unique physiology without compromising privacy.

## Current Milestone: v1.2 Athlete Context & Wearable Deep-Dive

**Goal:** Expand the athlete intelligence horizon by integrating richer environmental and behavioral context while deepening second-party wearable support.

**Target features:**
- **Nutrition Intelligence**: Advanced meal logging and personalized nutrient-timing guidance mapped to training load.
- **Gear & Injury Tracking**: Correlating specialized gear (shoes, bikes) and injury states with physiological response patterns.
- **Enhanced Wearables**: Deepening Health Connect mappings for Oura, Whoop, and Garmin high-resolution telemetry.
- **Contextual Anchoring**: Allowing the AI to anchor reasoning in non-biometric signals like weather, travel, and altitude.

## Requirements

### Validated

- [x] Android local-first health data ingestion and persistence (v1.0)
- [x] Production app shell and Strava integration (v1.0)
- [x] On-device AI Intelligence Expansion: Private inference, causal reasoning, and scenarios (v1.1)
- [x] Privacy-first cloud continuity snapshots via Firebase (v1.1)

### Active

- [ ] Build a flexible athlete-context schema for gear, injuries, and environmental signals.
- [ ] Implement nutrition-first data layer with barcode scanning and macro-tracking logic.
- [ ] Expand Health Connect reading to prioritize high-resolution proprietary fields from top-tier wearables.
- [ ] Add altitude and weather context ingestion to baseline scoring loops.
- [ ] Integrate gear usage triggers and injury recovery phase logic into the athlete coach guidance.

### Out of Scope

- Cloud-hosted LLM inference for core coaching features - violates privacy-first product promise.
- Exporting raw health biometrics off-device - explicitly disallowed.
- Apple Health integration - deferred until Android contextual depth is finalized.

## Context

- v1.1 was archived on 2026-04-19 and tagged as v1.1.
- Project has successfully migrated from Supabase to Firebase for all cloud continuity requirements.
- Core on-device AI runtime is stable and provides the foundation for the upcoming contextual expansion.

## Constraints

- **Privacy/Security**: Raw health data MUST remain on-device; only compact computed summaries may sync.
- **Performance**: High-resolution ingestion must not impact background sync or battery shelf life.
- **Data Fidelity**: Prioritize Health Connect as the primary source of truth while handling provider-specific quirks gracefully.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Total Firebase migration | Replaces Supabase for unified NoSQL and RLS continuity support | Decided (v1.1) |
| On-device small TFLite model | Keeps AI private and free while fitting mobile constraints | Validated (v1.1) |
| Nutrition as athlete context | Critical for accurate energy/recovery modeling beyond just biometrics | Decided (v1.2) |

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
