# Architecture

**Analysis Date:** 2026-04-15

## Pattern Overview

**Overall:** Single-module Android app with a clean-architecture-inspired layout, Hilt wiring, an encrypted local-first data layer, and a source-selecting health ingestion pipeline.

**Key Characteristics:**

- `app/` is the only Gradle module; the app, domain, data, and support layers all live under `app/src/main/java/com/aira/health`.
- The data layer chooses between Health Connect and Google Fit at runtime through `HealthDataModule.kt`.
- Persistent device state is split across Room (`AiraDatabase.kt`), DataStore (`DataStoreModule.kt`), and the Android Keystore (`KeystoreManager.kt`).
- Background sync is owned by WorkManager (`HealthSyncWorker.kt`) and re-scheduled after boot by `BootReceiver.kt`.
- The current Compose surface is narrow: only the onboarding permission flow is implemented in `presentation/onboarding/`.

## Layers

**Presentation layer:**

- Purpose: Compose UI and permission onboarding state.
- Location: `app/src/main/java/com/aira/health/presentation/onboarding/`
- Contains: `PermissionBatchScreen.kt`, `PermissionViewModel.kt`
- Depends on: `HealthPermissionManager.kt`, `HealthConnectStatus`, Hilt ViewModel injection.
- Used by: `MainActivity.kt` when a root `setContent` host is wired in.

**Domain layer:**

- Purpose: Pure Kotlin contracts and orchestration logic.
- Location: `app/src/main/java/com/aira/health/domain/`
- Contains: `domain/model/UserSession.kt`, `domain/repository/HealthDataRepository.kt`, `domain/repository/UserRepository.kt`, `domain/usecase/IngestHealthDataUseCase.kt`
- Depends on: data-layer interfaces only, plus Kotlin coroutines/DataStore types where required by the use case.
- Used by: repository implementations, WorkManager worker, and auth/session mapping.

**Data layer:**

- Purpose: Concrete persistence, remote, and device-source implementations.
- Location: `app/src/main/java/com/aira/health/data/`
- Contains: `data/local/`, `data/repository/`, `data/remote/supabase/`, `data/model/`, `data/worker/`
- Depends on: Room, SQLCipher, Health Connect, Google Fit, Supabase, WorkManager.
- Used by: domain use cases and Hilt modules.

**Platform and support layer:**

- Purpose: Application bootstrapping, DI, permissions, security, and boot receivers.
- Location: `app/src/main/java/com/aira/health/di/` and `app/src/main/java/com/aira/health/util/`
- Contains: `DatabaseModule.kt`, `DataStoreModule.kt`, `HealthDataModule.kt`, `NetworkModule.kt`, `RepositoryModule.kt`, `HealthPermissionManager.kt`, `KeystoreManager.kt`, `BiometricManager.kt`, `BootReceiver.kt`
- Depends on: Android framework APIs, Hilt, WorkManager, Health Connect, Android Keystore, Biometrics.

## Data Flow

**App startup and bootstrap:**

1. `AndroidManifest.xml` declares `AiraApplication.kt` as the application class and `MainActivity.kt` as the launcher activity.
2. `AiraApplication.kt` initializes Firebase and provides a Hilt-backed WorkManager configuration.
3. `DatabaseModule.kt` obtains the SQLCipher passphrase from `KeystoreManager.kt` and creates `AiraDatabase.kt`.
4. `NetworkModule.kt` exposes the Supabase singleton from `SupabaseClientProvider.kt`.

**Onboarding and permissions:**

1. `MainActivity.kt` is the entry activity and currently hosts an empty Compose root placeholder.
2. `PermissionBatchScreen.kt` and `PermissionViewModel.kt` manage the Health Connect onboarding flow.
3. `HealthPermissionManager.kt` groups permissions into Core, Body, and Advanced batches and checks Health Connect availability.
4. The UI is currently the only presentation slice in source; there is no separate navigation package in the current tree.

**Health ingestion and sync:**

1. `HealthSyncWorker.kt` runs periodic or immediate sync jobs.
2. The worker invokes `IngestHealthDataUseCase.kt`.
3. `HealthDataModule.kt` injects `HealthConnectRepositoryImpl.kt` when Health Connect is available, otherwise `GoogleFitRepositoryImpl.kt`.
4. `IngestHealthDataUseCase.kt` reads health samples, resolves overlaps with `ConfidenceRouter.kt`, writes to Room DAOs, purges old raw samples, and persists the last sync timestamp in DataStore.

**Auth and session state:**

1. `UserRepositoryImpl.kt` wraps Supabase auth and maps session events into `AuthState` and `UserSession`.
2. Guest mode is represented locally and bypasses Supabase initialization in `UserRepositoryImpl.kt`.
3. `SupabaseClientProvider.kt` reads flavor-configured BuildConfig values from `app/build.gradle.kts`.

**State Management:**

- `PermissionViewModel.kt` holds onboarding state in `MutableStateFlow`.
- `IngestHealthDataUseCase.kt` persists sync progress in DataStore and reads from it on each run.
- Room is the persistent store for local health entities in `data/local/model/`.
- WorkManager is the only scheduled background runtime in source.
- Supabase auth session state is exposed as a Kotlin `Flow` in `UserRepositoryImpl.kt`.

## Key Abstractions

**`HealthDataRepository`:**

- Purpose: Source-agnostic contract for health reads.
- Examples: `app/src/main/java/com/aira/health/domain/repository/HealthDataRepository.kt`, `app/src/main/java/com/aira/health/data/repository/HealthConnectRepositoryImpl.kt`, `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`
- Pattern: Interface in domain, runtime-selected implementations in data.

**`UserRepository`:**

- Purpose: Auth/session contract for Supabase-backed identity and guest mode.
- Examples: `app/src/main/java/com/aira/health/domain/repository/UserRepository.kt`, `app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt`
- Pattern: Interface-driven auth service with Flow-based state observation.

**`IngestHealthDataUseCase`:**

- Purpose: Orchestrates the health sync pipeline.
- Examples: `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt`
- Pattern: Single-purpose application service that coordinates repository reads, conflict resolution, DAO writes, and sync bookkeeping.

**`AiraDatabase`:**

- Purpose: Encrypted Room database for local health and user data.
- Examples: `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Pattern: Room database with SQLCipher support and destructive migration placeholder for early schema development.

**`HealthSyncWorker`:**

- Purpose: Periodic and immediate sync entry point.
- Examples: `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`
- Pattern: Hilt-injected `CoroutineWorker` with unique periodic and one-time work scheduling helpers.

**`HealthPermissionManager`:**

- Purpose: Central permission batch and availability logic for Health Connect.
- Examples: `app/src/main/java/com/aira/health/util/permission/HealthPermissionManager.kt`
- Pattern: Singleton helper that encapsulates runtime permission grouping and Health Connect status checks.

**`KeystoreManager` and `BiometricManager`:**

- Purpose: Device security primitives.
- Examples: `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt`, `app/src/main/java/com/aira/health/util/security/BiometricManager.kt`
- Pattern: Separate encryption-key management from optional UI lock authentication.

## Entry Points

**`AndroidManifest.xml`:**

- Location: `app/src/main/AndroidManifest.xml`
- Triggers: Android package install, launch, boot completion, and Health Connect permission usage intent alias.
- Responsibilities: Declares permissions, application class, launcher activity, boot receiver, and Health Connect metadata.

**`AiraApplication.kt`:**

- Location: `app/src/main/java/com/aira/health/AiraApplication.kt`
- Triggers: Process start.
- Responsibilities: Firebase initialization and WorkManager configuration.

**`MainActivity.kt`:**

- Location: `app/src/main/java/com/aira/health/MainActivity.kt`
- Triggers: Launcher intent.
- Responsibilities: Window setup, secure-flag toggle, and Compose host placeholder.

**`BootReceiver.kt`:**

- Location: `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt`
- Triggers: `BOOT_COMPLETED` broadcast.
- Responsibilities: Reschedules health sync work.

**`HealthSyncWorker.kt`:**

- Location: `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`
- Triggers: WorkManager periodic or one-time work.
- Responsibilities: Executes the ingestion use case and manages retry behavior.

## Error Handling

**Strategy:** Fail closed on permissions and security, prefer retryable background sync, and keep source-selection logic isolated behind DI.

**Patterns:**

- `runCatching` is used in `UserRepositoryImpl.kt`, `GoogleFitRepositoryImpl.kt`, and `HealthSyncWorker.kt` to convert runtime failures into `Result` or fallback behavior.
- `HealthDataModule.kt` selects the primary data source only when Health Connect is available; otherwise it falls back to Google Fit.
- `HealthSyncWorker.kt` retries transient failures up to three attempts.
- `IngestHealthDataUseCase.kt` keeps overlap resolution deterministic by selecting the highest-confidence record per timestamp or date.
- `AiraDatabase.kt` currently uses `fallbackToDestructiveMigration()` as an early-schema placeholder, which keeps startup simple but assumes no production migration history yet.

## Cross-Cutting Concerns

**Logging:** WorkManager is configured with INFO logging in `AiraApplication.kt`; no dedicated app logging wrapper is present in source.

**Validation:** Runtime capability checks happen in `HealthPermissionManager.kt` and `HealthDataModule.kt` before source-specific work is executed.

**Authentication:** Supabase auth is centralized in `UserRepositoryImpl.kt` and created in `SupabaseClientProvider.kt` from flavor-specific BuildConfig values.

**Security:** SQLCipher encryption is seeded from the Android Keystore in `KeystoreManager.kt`; optional biometric gating is separate in `BiometricManager.kt`; `MainActivity.kt` can enable `FLAG_SECURE` based on flavor config.

**Configuration:** Build-time environment selection comes from `app/build.gradle.kts` product flavors, while runtime resources are supplied from `app/src/main/res/values/` and `app/src/main/res/xml/`.

---

_Architecture analysis: 2026-04-15_
