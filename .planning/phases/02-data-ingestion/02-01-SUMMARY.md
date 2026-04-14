---
phase: "02"
plan: "01"
subsystem: data-ingestion
tags: [health-connect, repository, confidence-routing, hilt, domain]
requires: [Phase 1 — Room DB + SQLCipher entities (HrSample, HrvSample, SleepSession)]
provides: [HealthDataRepository interface, ConfidenceRouter, HealthConnectRepositoryImpl, HealthDataModule]
affects: [domain layer, data layer, DI graph]
tech-stack:
  added: [Health Connect Client 1.1.0-alpha07]
  patterns: [Repository pattern, Hilt @Module/@Provides, ConfidenceRouter singleton]
key-files:
  created:
    - app/src/main/java/com/aira/health/domain/repository/HealthDataRepository.kt
    - app/src/main/java/com/aira/health/data/model/ConfidenceRouter.kt
    - app/src/main/java/com/aira/health/data/repository/HealthConnectRepositoryImpl.kt
    - app/src/main/java/com/aira/health/di/HealthDataModule.kt
  modified: []
key-decisions:
  - Nullable HealthConnectClient provided by Hilt so downstream (HealthDataModule) can safely route to GoogleFit fallback without requiring a non-null guarantee
  - ConfidenceRouter maps Oura/Whoop at 100, Garmin/Fitbit at 85, Samsung/Google at 65, unknown at 40
  - readSleepSessions maps SleepSessionRecord.stages to remMin/deepMin/lightMin/awakeMin directly
requirements-completed: [DATA-01, DATA-03]
duration: "8 min"
completed: "2026-04-15"
---

# Phase 2 Plan 01: Health Connect Data Mapping Summary

Implemented the primary biometric ingestion boundary using Health Connect, backed by a confidence-weighted source router.

**Duration:** 8 min | **Tasks:** 4 | **Files:** 4 created

## What Was Built

- **`HealthDataRepository`** — Pure Kotlin domain interface defining suspend functions for HR, HRV, Sleep, SpO2, Calories, and Steps. Zero Android SDK dependencies.
- **`ConfidenceRouter`** — Static singleton mapping 15+ source package names to confidence tiers (100/85/65/40). Exposes `getConfidenceWeight`, `getConfidenceFloat`, and `preferredSource` helpers.
- **`HealthConnectRepositoryImpl`** — Primary implementation reading all 6 data types via `HealthConnectClient.readRecords`. Maps `HeartRateRecord.samples`, `SleepSessionRecord.stages`, etc. to Room entity types with ConfidenceRouter-assigned weights.
- **`HealthDataModule`** — Hilt `@Module` providing a nullable `HealthConnectClient` (null if SDK unavailable) and routing to the correct `HealthDataRepository` implementation.

## Self-Check: PASSED

- `interface HealthDataRepository` present ✅
- `readHeartRate`, `readSleepSessions` both defined ✅
- `getConfidenceWeight(packageName: String): Int` defined ✅
- `"com.garmin.android.apps.connectmobile"` present in tier2 ✅
- `HealthConnectRepositoryImpl` implements `HealthDataRepository` ✅
- `HealthConnectClient.getSdkStatus(context)` checked before instantiation ✅

## Deviations from Plan

None — plan executed exactly as written.

## Next

Ready for 02-02: Google Fit Fallback Integration
