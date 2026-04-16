---
phase: 06-strava-api-onboarding-integration-and-daily-activity-ingesti
plan: 01
subsystem: strava-onboarding-sync
tags:
  - execute
requires:
  - 05-runtime-data-wiring-ui-alignment
provides:
  - DATA-01
  - DATA-04
  - SYNC-01
  - UI-02
  - UI-03
affects:
  - app/src/main/java/com/aira/health/presentation/navigation
  - app/src/main/java/com/aira/health/presentation/onboarding
  - app/src/main/java/com/aira/health/presentation/dashboard/home
  - app/src/main/java/com/aira/health/data/repository
  - app/src/main/java/com/aira/health/data/remote/strava
  - app/src/main/java/com/aira/health/data/strava
  - app/src/main/java/com/aira/health/data/local/dao
  - app/src/main/java/com/aira/health/domain/repository
  - app/src/main/java/com/aira/health/domain/model
  - app/src/main/java/com/aira/health/domain/usecase
  - app/src/test/java/com/aira/health/data/repository
key-files.created:
  - app/src/main/java/com/aira/health/presentation/onboarding/StravaConnectScreen.kt
  - app/src/main/java/com/aira/health/data/repository/StravaRepositoryImpl.kt
  - app/src/main/java/com/aira/health/data/remote/strava/StravaApiClient.kt
  - app/src/main/java/com/aira/health/data/remote/strava/StravaDtos.kt
  - app/src/main/java/com/aira/health/data/strava/StravaTokenStore.kt
  - app/src/main/java/com/aira/health/data/strava/StravaConnectionStore.kt
  - app/src/main/java/com/aira/health/domain/repository/StravaRepository.kt
  - app/src/main/java/com/aira/health/domain/usecase/SyncStravaActivitiesUseCase.kt
  - app/src/test/java/com/aira/health/data/repository/StravaRepositoryImplTest.kt
key-decisions:
  - Made Strava onboarding mandatory between auth and permission batches.
  - Implemented local-first token/session storage with reconnect-required fallback.
  - Added full-history backfill cursor and incremental sync cursor scaffolding in repository + DataStore.
  - Added rate-header-aware pre-throttling and deferred sync window tracking for 429 and 5xx resilience.
  - Added confidence-based duplicate suppression for overlapping Strava and non-Strava workouts.
requirements:
  - DATA-01
  - DATA-04
  - SYNC-01
  - UI-02
  - UI-03
duration: 2 sessions
completed: 2026-04-16T16:34:00Z
---

# Phase 06 Plan 01: Strava Onboarding and Full Sync Summary

Implemented all six waves for Phase 06 plan 01, including onboarding, OAuth lifecycle, incremental ingestion, mapping hardening, API resilience handling, and verification coverage.

## Tasks Completed

- [x] Mandatory Strava onboarding step added to app entry flow before permission batching.
- [x] OAuth authorization URL generation and callback handling integrated.
- [x] Secure Strava token storage added via EncryptedSharedPreferences.
- [x] Strava repository + DTO/API client + connection store implemented.
- [x] Disconnect hardening added with best-effort deauthorize and local state reset.
- [x] Scope validation added to block incomplete Strava grants.
- [x] Backfill/incremental cursor model and idempotent insert-or-ignore ingestion pipeline completed.
- [x] Worker orchestration now triggers Strava sync alongside Health Connect ingestion.
- [x] Activity type mapping normalized (run/ride/walk/hike with fallback mapping) and duplicate suppression added using confidence-first selection.
- [x] Strava rate header parsing added with pre-throttling behavior and deferred-sync windows for 429 and transient 5xx failures.
- [x] Strava connection state extended with deferred sync and user-safe error metadata.
- [x] Added dedicated Strava repository unit tests for token refresh lifecycle, throttling, cursor handling, and dedupe behavior.
- [x] Stabilized baseline unit tests impacted by recent debug logging changes and updated DI expectation to merged Health Connect repository behavior.

## Commits

- `07b1fea` feat: execute phase 6 strava onboarding and sync scaffold
- `dd819d6` feat: harden strava oauth scope and disconnect revoke
- `4d5b226` feat: complete phase 6 waves 3-6 strava sync resilience and tests

## Verification

- PASS: `./gradlew.bat :app:compileDevDebugKotlin`
- PASS: `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.repository.StravaRepositoryImplTest"`
- PASS: `./gradlew.bat :app:testDevDebugUnitTest`
- PASS: `./gradlew.bat :app:compileDevDebugAndroidTestKotlin`
- PASS: `./gradlew.bat :app:compileDevDebugKotlin :app:testDevDebugUnitTest :app:compileDevDebugAndroidTestKotlin --no-daemon`

## Remaining Work

- None for this plan. Follow-up work, if any, should be scoped as a new phase/plan.

## Self-Check: PASSED
