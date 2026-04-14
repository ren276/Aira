# Directory Structure

This document outlines the physical layout of the Aira project.

## Root Directory

| Path | Description |
| :--- | :--- |
| `app/` | Main application module. |
| `.planning/` | GSD workflow artifacts and codebase mapping documentation. |
| `gradle/` | Gradle wrapper and version catalog (`libs.versions.toml`). |
| `designs/` | UI/UX high-fidelity mockups and design assets. |

## App Module (`app/src/main/java/com/aira/health`)

| Package | Description |
| :--- | :--- |
| `data/` | Data layer implementations (repositories, DAOs, services). |
| `data/local/` | Room entities, DAOs, and database configuration. |
| `data/local/dao/` | Room DAOs: `HrSampleDao`, `HrvSampleDao`, `SleepSessionDao`, etc. |
| `data/local/model/` | Room entity data classes: `HrSample`, `HrvSample`, `SleepSession`, etc. |
| `data/model/` | Pure data model helpers: `ConfidenceRouter` (source-to-tier map). |
| `data/remote/` | Supabase API clients and DTOs. |
| `data/repository/` | Repository implementations: `HealthConnectRepositoryImpl`, `GoogleFitRepositoryImpl`. |
| `data/worker/` | WorkManager workers: `HealthSyncWorker` (CoroutineWorker, @HiltWorker). |
| `domain/` | Business logic (entities, interactor/use cases, repository interfaces). |
| `domain/model/` | Pure Kotlin domain models: `UserSession`, etc. |
| `domain/repository/` | Repository interfaces: `UserRepository`, `HealthDataRepository`. |
| `domain/usecase/` | Use cases: `IngestHealthDataUseCase`. |
| `presentation/` | UI layer (Screens, ViewModels, Themes, Navigation). |
| `presentation/navigation/` | Navigation graphs and destinations. |
| `presentation/theme/` | Jetpack Compose design system implementation. |
| `di/` | Hilt modules: `DatabaseModule`, `HealthDataModule`, `NetworkModule`, `RepositoryModule`. |
| `util/` | Reusable extensions, helpers, and constant definitions. |
| `util/receiver/` | Broadcast receivers: `BootReceiver` (reschedules WorkManager on boot). |

## Resources (`app/src/main/res`)

| Path | Description |
| :--- | :--- |
| `drawable/` | Static vector graphics and bitmapped images. |
| `values/` | String resources, colors, and global styles. |
| `xml/` | Configuration files (e.g., backup rules, search config). |

## Core Application Files

- `AiraApplication.kt`: Hilt `@HiltAndroidApp` entry point.
- `MainActivity.kt`: Single entry activity with `@AndroidEntryPoint`.
- `build.gradle.kts`: Module-level build configuration and dependencies.
