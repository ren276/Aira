# Codebase Structure

**Analysis Date:** 2026-04-15

## Directory Layout

```text
Aira/
├── app/
│   ├── build.gradle.kts         # Android app module, flavors, dependencies
│   ├── google-services.json     # Firebase config for the active app setup
│   ├── google-services.json.example
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/aira/health/
│       │   │   ├── AiraApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   ├── di/
│       │   │   ├── domain/
│       │   │   ├── presentation/
│       │   │   └── util/
│       │   └── res/
│       │       ├── values/
│       │       ├── values-night/
│       │       └── xml/
│       └── test/
│           └── java/com/aira/health/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── designs/
│   └── ...
├── .planning/
│   └── codebase/
└── build/
```

## Directory Purposes

**`app/`:**

- Purpose: The only Gradle module and the entire Android application surface.
- Contains: Kotlin source, manifest, resources, app-level Gradle configuration, and Firebase config.
- Key files: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/aira/health/AiraApplication.kt`, `app/src/main/java/com/aira/health/MainActivity.kt`.

**`app/src/main/java/com/aira/health/data/`:**

- Purpose: Data sources, persistence, and sync execution.
- Contains: Room database, entities, DAOs, repository implementations, Supabase client wrapper, WorkManager worker, confidence routing.
- Key files: `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`, `app/src/main/java/com/aira/health/data/repository/HealthConnectRepositoryImpl.kt`, `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`, `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`.

**`app/src/main/java/com/aira/health/domain/`:**

- Purpose: Pure Kotlin contracts and orchestration logic.
- Contains: Domain models, repository interfaces, and the ingestion use case.
- Key files: `app/src/main/java/com/aira/health/domain/model/UserSession.kt`, `app/src/main/java/com/aira/health/domain/repository/HealthDataRepository.kt`, `app/src/main/java/com/aira/health/domain/repository/UserRepository.kt`, `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt`.

**`app/src/main/java/com/aira/health/presentation/`:**

- Purpose: Compose UI and ViewModels.
- Contains: Only the onboarding permission flow in the current tree.
- Key files: `app/src/main/java/com/aira/health/presentation/onboarding/PermissionBatchScreen.kt`, `app/src/main/java/com/aira/health/presentation/onboarding/PermissionViewModel.kt`.
- Current state: No `presentation/navigation/` or reusable `presentation/theme/` package exists in source.

**`app/src/main/java/com/aira/health/di/`:**

- Purpose: Hilt modules that wire app-wide dependencies.
- Contains: Database, datastore, network, repository, and health-source modules.
- Key files: `app/src/main/java/com/aira/health/di/DatabaseModule.kt`, `app/src/main/java/com/aira/health/di/HealthDataModule.kt`, `app/src/main/java/com/aira/health/di/NetworkModule.kt`, `app/src/main/java/com/aira/health/di/RepositoryModule.kt`, `app/src/main/java/com/aira/health/di/DataStoreModule.kt`.

**`app/src/main/java/com/aira/health/util/`:**

- Purpose: Cross-cutting helpers and platform wrappers.
- Contains: Permission management, security helpers, and broadcast receiver support.
- Key files: `app/src/main/java/com/aira/health/util/permission/HealthPermissionManager.kt`, `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt`, `app/src/main/java/com/aira/health/util/security/BiometricManager.kt`, `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt`.

**`app/src/main/res/`:**

- Purpose: Android resources and manifest-adjacent XML config.
- Contains: app strings, theme definitions, Health Connect permissions metadata, backup rules, and data extraction rules.
- Key files: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values/health_permissions.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`.

**`app/src/test/java/com/aira/health/`:**

- Purpose: JVM unit tests for the app package.
- Contains: tests for repository mapping, worker scheduling, the ingestion use case, the boot receiver, and confidence routing.
- Key files: `app/src/test/java/com/aira/health/data/repository/UserRepositoryImplTest.kt`, `app/src/test/java/com/aira/health/domain/usecase/IngestHealthDataUseCaseTest.kt`, `app/src/test/java/com/aira/health/data/worker/HealthSyncWorkerScheduleTest.kt`, `app/src/test/java/com/aira/health/util/receiver/BootReceiverTest.kt`.

**`designs/`:**

- Purpose: Product and UI design references.
- Contains: concept-specific design folders and rendered mockups.
- Key files: `designs/aira_intelligence/DESIGN.md` and the `code.html` artifacts under the other design folders.

**`.planning/`:**

- Purpose: GSD planning and mapping artifacts.
- Contains: codebase documents and roadmap state.
- Key files: `.planning/codebase/ARCHITECTURE.md`, `.planning/codebase/STRUCTURE.md`.

**`gradle/`:**

- Purpose: Shared Gradle infrastructure.
- Contains: version catalog and wrapper metadata.
- Key files: `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`.

**`build/` and `app/build/`:**

- Purpose: Generated build outputs and reports.
- Contains: compiled artifacts, intermediates, reports, generated source stubs, and task outputs.
- Generated: Yes.
- Committed: No.

## Key File Locations

**Entry Points:**

- `app/src/main/AndroidManifest.xml`: Declares the launcher activity, application class, boot receiver, and Health Connect metadata.
- `app/src/main/java/com/aira/health/AiraApplication.kt`: Application bootstrap and WorkManager configuration.
- `app/src/main/java/com/aira/health/MainActivity.kt`: Launcher activity and Compose host placeholder.
- `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt`: Boot-time rescheduling hook.
- `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`: Sync execution entry point.

**Configuration:**

- `build.gradle.kts`: Root plugin declarations.
- `app/build.gradle.kts`: Product flavors, BuildConfig flags, dependencies, and build types.
- `settings.gradle.kts`: Single-module project definition.
- `gradle/libs.versions.toml`: Centralized dependency versions and plugin aliases.
- `local.properties`: Local environment values consumed by `app/build.gradle.kts`.
- `app/google-services.json`: Firebase runtime configuration.
- `app/google-services.json.example`: Template for the Firebase config file.
- `app/proguard-rules.pro`: Release shrinker rules.

**Core Logic:**

- `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt`: Health ingestion orchestration.
- `app/src/main/java/com/aira/health/data/repository/HealthConnectRepositoryImpl.kt`: Primary Health Connect source adapter.
- `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`: Legacy fallback source adapter.
- `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`: Encrypted Room database.
- `app/src/main/java/com/aira/health/data/model/ConfidenceRouter.kt`: Source confidence routing.

**Testing:**

- `app/src/test/java/com/aira/health/`: JVM test root for the package tree.
- `app/src/androidTest/java/com/aira/health/`: Not detected in the current source tree.

## Naming Conventions

**Files:**

- Kotlin files mirror the top-level type they define, such as `PermissionViewModel.kt`, `HealthSyncWorker.kt`, and `AiraDatabase.kt`.
- Repository implementations end in `RepositoryImpl`, while contracts stay in `domain/repository/`.
- Room types are grouped by role: `dao/` for DAO interfaces, `model/` for entities, `db/` for the database class.

**Directories:**

- Layer-first package layout under `com/aira/health/` is the norm: `data/`, `domain/`, `presentation/`, `di/`, and `util/`.
- Feature-specific UI should live under a feature subpackage such as `presentation/onboarding/` rather than a global screens folder.
- Device or platform helpers belong in a narrow utility subpackage such as `util/security/`, `util/permission/`, or `util/receiver/`.

## Where to Add New Code

**New Feature:**

- Primary code: `app/src/main/java/com/aira/health/presentation/<feature>/` for Compose UI and ViewModels, `app/src/main/java/com/aira/health/domain/usecase/` for orchestration, and `app/src/main/java/com/aira/health/data/repository/` or `data/local/` for data access.
- Tests: `app/src/test/java/com/aira/health/<matching-package>/`.

**New Component/Module:**

- Implementation: `app/src/main/java/com/aira/health/di/` for new Hilt bindings, or a dedicated package under `data/`, `domain/`, or `presentation/` if the component is feature-specific.

**Utilities:**

- Shared helpers: `app/src/main/java/com/aira/health/util/`.
- Device-specific helpers: `util/security/`, `util/permission/`, or `util/receiver/`.

**Persistence:**

- New Room entities and DAOs belong under `data/local/model/` and `data/local/dao/`.
- Database wiring belongs in `data/local/db/AiraDatabase.kt` and `di/DatabaseModule.kt`.

## Special Directories

**`app/src/main/res/values-night/`:**

- Purpose: Night-mode resource overrides.
- Generated: No.
- Committed: Yes.

**`app/src/main/res/xml/`:**

- Purpose: Manifest-adjacent XML config for backup and data extraction rules.
- Generated: No.
- Committed: Yes.

**`designs/`:**

- Purpose: Product design references and HTML mockups.
- Generated: No.
- Committed: Yes.

**`.planning/codebase/`:**

- Purpose: Generated mapping documents used by GSD workflows.
- Generated: Yes.
- Committed: Yes.

**`build/` and `app/build/`:**

- Purpose: Generated Gradle outputs, intermediates, and reports.
- Generated: Yes.
- Committed: No.

---

_Structure analysis: 2026-04-15_
