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
  - app/src/main/java/com/aira/health/data/repository
  - app/src/main/java/com/aira/health/data/remote/strava
  - app/src/main/java/com/aira/health/data/strava
  - app/src/main/java/com/aira/health/domain/repository
  - app/src/main/java/com/aira/health/domain/usecase
key-files.created:
  - app/src/main/java/com/aira/health/presentation/onboarding/StravaConnectScreen.kt
  - app/src/main/java/com/aira/health/data/repository/StravaRepositoryImpl.kt
  - app/src/main/java/com/aira/health/data/remote/strava/StravaApiClient.kt
  - app/src/main/java/com/aira/health/data/remote/strava/StravaDtos.kt
  - app/src/main/java/com/aira/health/data/strava/StravaTokenStore.kt
  - app/src/main/java/com/aira/health/data/strava/StravaConnectionStore.kt
  - app/src/main/java/com/aira/health/domain/repository/StravaRepository.kt
  - app/src/main/java/com/aira/health/domain/usecase/SyncStravaActivitiesUseCase.kt
key-decisions:
  - Made Strava onboarding mandatory between auth and permission batches.
  - Implemented local-first token/session storage with reconnect-required fallback.
  - Added full-history backfill cursor and incremental sync cursor scaffolding in repository + DataStore.
requirements:
  - DATA-01
  - DATA-04
  - SYNC-01
  - UI-02
  - UI-03
duration: 1 session
completed: 2026-04-16T16:20:00Z
---

# Phase 06 Plan 01: Strava Onboarding and Sync Scaffold Summary

Implemented Waves 1 and 2 and major Wave 3 foundations for Strava integration, then validated with compile and targeted tests.

## Tasks Completed

- [x] Mandatory Strava onboarding step added to app entry flow before permission batching.
- [x] OAuth authorization URL generation and callback handling integrated.
- [x] Secure Strava token storage added via EncryptedSharedPreferences.
- [x] Strava repository + DTO/API client + connection store implemented.
- [x] Disconnect hardening added with best-effort deauthorize and local state reset.
- [x] Scope validation added to block incomplete Strava grants.
- [x] Backfill/incremental cursor model and idempotent insert-or-ignore ingestion scaffold added.
- [x] Worker orchestration now triggers Strava sync alongside Health Connect ingestion.

## Commits

- `07b1fea` feat: execute phase 6 strava onboarding and sync scaffold
- `dd819d6` feat: harden strava oauth scope and disconnect revoke

## Verification

- PASS: `./gradlew.bat :app:compileDevDebugKotlin`
- PASS: `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.navigation.AppEntryRouteDestinationTest" --tests "com.aira.health.presentation.navigation.AppEntryViewModelTest"`
- FAIL: `./gradlew.bat :app:compileDevDebugKotlin :app:testDevDebugUnitTest :app:compileDevDebugAndroidTestKotlin`
  - 11 unit test failures outside newly added Strava files (ComputeDailyScoresUseCaseTest, HomeViewModelTest, HealthDataModuleTest, IngestHealthDataUseCaseTest).

## Remaining Work

- Complete Waves 4-6 from plan (mapping depth refinements, resilience tuning, extended test/UAT closure).
- Resolve baseline red tests so full primary gate is green.

## Self-Check: FAILED

Reason: Full-unit primary verification gate is red due existing unrelated failing tests, so plan cannot be marked fully complete yet.
