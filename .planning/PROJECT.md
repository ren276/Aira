# Aira

## What This Is

A privacy-first, on-device health intelligence OS for Android that aggregates wearable and phone sensor data, learns the user's unique physiology over time, and converts noisy raw data into accurate, explainable daily scores with causal reasoning. It is designed to be Android-first, built natively with Jetpack Compose, integrating deeply with Health Connect and Google Fit.

## Core Value

Empower Android users with true on-device, explainable health intelligence that learns their unique physiology without compromising their privacy or requiring their raw biome data to leave the device.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Support Android 10+ (minSdk 29, targetSdk 35) with `com.aira.health` package
- [ ] Implement UI entirely in Kotlin Jetpack Compose according to `designs/` folder
- [ ] MVVM + Clean Architecture + Hilt DI + Flow/Coroutines
- [ ] Health Connect primary data source; Google Fit fallback (prompt Android 10-13 to download HC)
- [ ] Core Engine: Calculate Recovery, Sleep, Strain, Stress, Energy Bank, Readiness-to-Learn, Nutrition, Burnout Risk
- [ ] Baseline Calculation: Implement EMA baselines and confidence engines with wearable source detection
- [ ] On-device local persistence: Room Database encrypted with SQLCipher via Android Keystore
- [ ] Supabase Auth & Remote Sync (Google Sign-In, Email, Guest), PostgreSQL with RLS
- [ ] Local AI Coach: Integrate Gemma 4 2B (e2b) via MediaPipe for personalized health insights without medical advice
- [ ] Hardening: Biometric App Lock, FLAG_SECURE on sensitive screens, HTTPS with Certificate Pinning
- [ ] Subscriptions: RevenueCat for Google Play Billing (Aira Pro monthly/annual)
- [ ] Telemetry & Quality: Firebase Crashlytics & Performance monitoring (clean/de-identified)

### Out of Scope

- [ ] iOS Support — Deferred (Android ecosystem focus initially)
- [ ] Cloud AI Processing for core health scores — Explicitly excluded to enforce privacy-first, on-device processing guarantees
- [ ] Medical diagnostics — Explicitly excluded, strictly positioned as an intelligent coaching tool

## Context

- The Android health tech ecosystem currently lacks a pure on-device, explainable AI platform with deep Health Connect integration.
- Users want insight without handing over massive amounts of raw biometric/health data to cloud providers.
- Leveraging powerful device capabilities like Gemma 4 2B locally ensures privacy while offering top-tier personalization.
- GSD Workflow initialized with detailed architectural decisions.

## Constraints

- **Tech Stack**: 100% Kotlin, Jetpack Compose, Room, WorkManager, Hilt, Supabase, Coil, kotlinx.serialization — Native Android approach for long-term maintainability.
- **Privacy/Security**: Must ensure complete raw data isolation locally on the device (SQLCipher). Only computed metrics, scores, and AI narratives sync to Supabase (if opted-in).
- **Target OS**: minSdk 29 to support Android 10 users, but pushing them towards Health Connect via Play Store.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin + Jetpack Compose | Native Android best practice and optimum performance | — Pending |
| Local Gemma 4 2B (e2b) model | Deliver privacy-first AI intelligence without cloud dependency | — Pending |
| Supabase for Backend Sync | Superior RLS, native Android Kotlin SDK, predictable pricing vs alternatives | — Pending |
| Health Connect Primary / Fit Fallback | Single source of truth for unified Android health data with failover robustness | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition**:
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone**:
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-15 after initialization*
