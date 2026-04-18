# Phase 10: Pattern Map

Generated: 2026-04-18
Phase: 10-cloud-continuity-snapshot-and-milestone-hardening

## Target Files and Closest Analogs

| Planned Role | Target File (expected) | Closest Existing Analog | Why this analog |
|---|---|---|---|
| Supabase-backed repository | app/src/main/java/com/aira/health/data/repository/ContinuitySnapshotRepositoryImpl.kt | app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt | Uses injected Supabase client and maps domain contract to backend operations with Result-based error boundaries. |
| Continuity domain repository contract | app/src/main/java/com/aira/health/domain/repository/ContinuitySnapshotRepository.kt | app/src/main/java/com/aira/health/domain/repository/UserRepository.kt | Pure Kotlin domain seam pattern with no Android/Room/Supabase imports in contract. |
| Upload orchestration use-case | app/src/main/java/com/aira/health/domain/usecase/UploadContinuitySnapshotUseCase.kt | app/src/main/java/com/aira/health/domain/usecase/SyncStravaActivitiesUseCase.kt | Lightweight use-case orchestration with repository delegation and explicit result propagation. |
| Restore orchestration use-case | app/src/main/java/com/aira/health/domain/usecase/RestoreContinuitySnapshotUseCase.kt | app/src/main/java/com/aira/health/domain/usecase/ComputeDailyScoresUseCase.kt | Domain-usecase pipeline pattern coordinating multiple local dependencies and safe failure handling. |
| Sync backstop scheduling integration | app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt | app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt | Existing periodic/immediate scheduling and backoff policy should be extended, not duplicated. |
| Settings backup toggle behavior wiring | app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt | app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt | Existing DataStore-backed toggle and uiState exposure pattern already in place. |
| Reset flow UI/VM behavior | app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt | app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt | Existing account actions (sign out, disconnect) follow ViewModel intent dispatch + loading/error flags. |
| Room migration addition | app/src/main/java/com/aira/health/data/local/db/migrations/Migration10ContinuityTables.kt | app/src/main/java/com/aira/health/data/local/db/migrations/Migration09PredictionTables.kt | Phase-based explicit migration object convention is established and testable. |

## Reusable Code Excerpts

### Supabase DI seam

From app/src/main/java/com/aira/health/di/NetworkModule.kt:

- provideSupabaseClient() returns shared SupabaseClient for data-layer usage.

### Worker scheduling seam

From app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt:

- schedule() uses ExistingPeriodicWorkPolicy.KEEP and backoff criteria.
- scheduleImmediate() uses unique one-time work with replace policy.

### Settings preference seam

From app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt:

- CLOUD_BACKUP_ENABLED preference key exists.
- setCloudBackupEnabled(enabled) writes DataStore flag.

### Account action seam

From app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt:

- AccountViewModel owns user action entrypoints and in-progress/error state updates.

## Constraints to Preserve

- Local-first privacy boundary: continuity payload must remain derived-only.
- Do not bypass DI to instantiate Supabase client directly in UI or use-case layers.
- Keep reset flow explicit with blocking safety gate semantics from Phase 10 decisions.
