# Architecture Overview

Aira follows **Clean Architecture** principles combined with **MVI (Model-View-Intent)** or **State-driven MVVM** in the presentation layer.

## Layers

### 1. Presentation Layer
- **Framework**: Jetpack Compose.
- **Pattern**: ViewModels maintain a single `StateFlow<UiState>` which is observed by Composable screens.
- **Navigation**: Using Jetpack Navigation Compose with a centralized `NavHost`.
- **DI**: Hilt (`@HiltViewModel`) for injecting Use Cases.

### 2. Domain Layer
- **Contents**: Entities, Repository Interfaces, and Use Cases.
- **Independence**: This layer should have zero dependencies on Android frameworks or the Data layer.
- **Use Cases**: Single-responsibility classes (e.g., `GetSleepScoreUseCase`) that orchestrate business logic.

### 3. Data Layer
- **Contents**: Repository Implementations, Data Sources (Local/Remote), and Mappers.
- **Local Source**: Room DAO + SQLCipher for encrypted on-device storage.
- **Remote Source**: Supabase client for cloud operations.
- **Mappers**: Convert Data Transfer Objects (DTOs) or Database Entities into Domain Models.

## Dependency Injection (Hilt)

- Standard for dependency management across all layers.
- Modules located in `com.aira.health.di`.
- Uses Scopes like `@Singleton` and `@ViewModelScoped` where appropriate.

## Concurrency & Data Flow

- **Coroutines**: Used for all background work and asynchronous API calls.
- **Flow**: Used for reactive data streams from Room or Health Connect into the Presentation layer.
- **WorkManager**: Used for deferred, persistent background tasks (e.g., periodic health data sync).
