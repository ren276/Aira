---
phase: "02"
plan: "03"
subsystem: background-sync
tags: [workmanager, hilt-worker, use-case, boot-receiver, datastore]
requires: [HealthDataRepository (02-01/02-02), Room DAOs (Phase 1), DataStore (Phase 1)]
provides: [IngestHealthDataUseCase, HealthSyncWorker, BootReceiver (wired), AiraApplication Hilt factory]
affects: [domain layer, data worker layer, application entry point]
tech-stack:
  added: [HiltWorker + AssistedInject pattern, PeriodicWorkRequestBuilder, ExistingPeriodicWorkPolicy.KEEP]
  patterns: [Use case orchestration, DataStore for last-sync timestamp, Confidence-based conflict resolution]
key-files:
  created:
    - app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt
    - app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt
  modified:
    - app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt
    - app/src/main/java/com/aira/health/AiraApplication.kt
key-decisions:
  - First launch backfill depth is 14 days (CONTEXT.md decision) to avoid cold-start baseline emptiness
  - Conflict resolution groups HR/HRV by `timestamp`, Sleep by `date`, picks max(confidence)
  - RawSample tables are purged after 90 days rolling window to control storage growth
  - Worker uses NetworkType.NOT_REQUIRED — Health Connect reads are fully local
  - AiraApplication wires HiltWorkerFactory into workManagerConfiguration so @HiltWorker injection chain resolves correctly
requirements-completed: [DATA-04]
duration: "10 min"
completed: "2026-04-15"
---

# Phase 2 Plan 03: WorkManager HealthSyncWorker and Source Parsers Summary

Established the full background ingestion loop: a 30-minute periodic CoroutineWorker backed by Hilt injection, orchestrated by a clean domain use case, with automatic rescheduling on device reboot.

**Duration:** 10 min | **Tasks:** 2 + 1 deviation fix | **Files:** 2 created, 2 modified

## What Was Built

- **`IngestHealthDataUseCase`** — Orchestrates the full data pipeline: reads `lastSyncEpoch` from DataStore, queries all 3 data types (HR, HRV, Sleep) from the repository, resolves conflicts by `maxByOrNull { it.confidence }`, persists to Room DAOs, purges samples >90 days, and saves the new sync timestamp to DataStore.
- **`HealthSyncWorker`** — `@HiltWorker CoroutineWorker` that calls `IngestHealthDataUseCase.invoke()` in `doWork()`. Exposes `schedule()` (30m periodic, 15m flex, exponential backoff) and `scheduleImmediate()` (expedited one-shot for foreground fast-sync).
- **`BootReceiver`** — Stub wired with `HealthSyncWorker.schedule(context)` for idempotent re-enqueue on cold boot.
- **`AiraApplication`** — `HiltWorkerFactory` injected and wired into `workManagerConfiguration` so `@AssistedInject` in `HealthSyncWorker` resolves at runtime.

## Self-Check: PASSED

- `PeriodicWorkRequestBuilder<HealthSyncWorker>` in `HealthSyncWorker.kt` ✅
- `BootReceiver` calls `HealthSyncWorker.schedule(context)` ✅
- `AndroidManifest.xml` contains `RECEIVE_BOOT_COMPLETED` permission and `<receiver>` for `BootReceiver` ✅
- `IngestHealthDataUseCase` calls `repository.readHeartRate`, `repository.readHeartRateVariability`, `repository.readSleepSessions` ✅

## Deviations from Plan

- **AiraApplication modification (Rule 1 — missing critical)**: The `HiltWorkerFactory` wiring was not in the original plan but is required for `@HiltWorker` + `@AssistedInject` to resolve at runtime. Auto-fixed by injecting `HiltWorkerFactory` and passing it to `Configuration.Builder.setWorkerFactory()`.

## Phase Complete

Phase 2 — Data Ingestion: all 3 plans executed. All DATA-01 through DATA-04 requirements marked complete.
Ready for Phase 3: Scoring Engines & Logic.
