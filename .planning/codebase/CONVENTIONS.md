# Coding Conventions

**Analysis Date:** 2026-04-15

## Naming Patterns

**Files:**

- One public type per file is the dominant pattern in `app/src/main/java/com/aira/health/...` and `app/src/test/java/com/aira/health/...`.
- Package paths mirror responsibility boundaries such as `data/repository`, `domain/usecase`, `presentation/onboarding`, and `util/security`.

**Functions:**

- Use camelCase verbs for actions and queries.
- Boolean helpers use `is`, `has`, or `can` prefixes, such as `isAvailable()` and `isCoreGranted()`.
- ViewModel event handlers use `on...` names like `onGrantAccessTapped()` and `onUseLimitedModeTapped()`.

**Variables:**

- Prefer descriptive `val` names over short mutable state.
- Backing state uses a private mutable prefix, such as `_uiState`, with a read-only public exposure.

**Types:**

- PascalCase for classes, interfaces, sealed classes, data classes, and enums.
- Domain contracts stay in `domain/...` and avoid Android, Room, or Supabase imports.

## Code Style

**Language/Toolchain:**

- Kotlin 2.0.21 with JVM toolchain 17 in `app/build.gradle.kts`.
- Android compile options target Java 17.

**Style:**

- Prefer immutable `val` state, expression bodies for small functions, and `when` expressions for state branching.
- Use `runCatching` at external boundaries such as repository calls, WorkManager execution, and sign-in flows.
- Keep functions small and single-purpose; extract helper methods when mapping or conflict resolution becomes non-trivial.

**Layering:**

- `domain/...` stays pure Kotlin and defines contracts such as `HealthDataRepository` and `UserRepository`.
- `data/...` owns Room, Health Connect, Google Fit, Supabase, and WorkManager implementations.
- `presentation/...` owns Compose screens and ViewModels.
- `di/...` owns Hilt wiring only.

## Compose Patterns

**State Hoisting:**

- Hoist screen state into `ViewModel`s and expose it as `StateFlow`, then consume it in Compose with `collectAsState()`.
- Keep UI events as explicit ViewModel methods instead of mutating state inside the composable tree.

**Screen Shape:**

- Compose screens are narrow and feature-scoped, as shown by `PermissionBatchScreen` in `presentation/onboarding`.
- Use private helper composables for subpieces of a screen when they are not reused elsewhere, such as `HealthConnectInstallPrompt()`.

**Material Usage:**

- Prefer Material 3 components and `Modifier` chains.
- Use theme-provided colors and typography rather than hardcoded values when the design system is introduced.

**Previews:**

- No `@Preview` or `@PreviewParameter` usage is detected in the current codebase.
- Add previews only when the component is stable enough to be useful; do not treat previews as a required convention yet.

## DI Patterns

**Constructor Injection:**

- Prefer constructor injection for repositories, use cases, and ViewModels.
- `@Inject` constructors are the default for concrete implementations such as `UserRepositoryImpl` and `HealthPermissionManager`.

**Module Shape:**

- Use `@Binds` for simple interface-to-implementation mappings, as in `RepositoryModule`.
- Use `@Provides` when construction depends on runtime conditions or factory methods, as in `DatabaseModule`, `DataStoreModule`, `HealthDataModule`, and `NetworkModule`.

**Qualifiers and Scope:**

- Use `@ApplicationContext` for application-scoped Android dependencies.
- Prefer `@Singleton` for app-wide infrastructure such as the Room database, Supabase client, and DataStore.

**Source Selection:**

- Select platform-specific implementations in DI rather than scattering source selection across callers.
- Current example: `HealthDataModule` chooses `HealthConnectRepositoryImpl` when Health Connect is available and falls back to `GoogleFitRepositoryImpl` otherwise.

## Imports

**Order:**

1. Standard library and JDK imports.
2. Android and AndroidX imports.
3. Third-party imports.
4. Project imports.

**Grouping:**

- Group imports by origin and keep them stable within a file.
- Avoid interleaving project imports with third-party imports.

**Aliases:**

- No path alias or wildcard-import convention is enforced in the repository.

## Error Handling

**Repository Boundaries:**

- Use `runCatching` for network or SDK calls where a failure should become a `Result` or a safe fallback.
- Return empty collections for optional source reads when the source is unavailable or the request fails, as seen in `GoogleFitRepositoryImpl`.

**Domain Errors:**

- Use explicit state types such as `AuthState.Error` for user-facing failures.
- Keep error messages narrow and actionable; avoid leaking low-level SDK details unless needed for diagnosis.

**Fallbacks:**

- Treat Health Connect as the primary source and Google Fit as a compatibility fallback on older Android versions.
- Keep fallback logic in data-layer implementations or DI providers, not in the UI.

## Logging

**Framework:**

- No dedicated logging framework is detected in the current codebase.
- Crash reporting is handled through Firebase Crashlytics initialised in `AiraApplication`.

**Patterns:**

- Use logging sparingly until a project-wide logger is introduced.
- Prefer structured error handling and explicit result types over ad hoc log-and-continue flows.

## Comments

**When to Comment:**

- Comment only on decision points that are not obvious from the code, such as security flags, fallback source selection, or work scheduling policy.
- Use short comments for privacy, lifecycle, or migration constraints.

**Current Usage:**

- Comments in the codebase explain flavor flags, SQLCipher setup, Health Connect fallback behavior, and WorkManager policy.
- TODO comments should be scoped and time-bounded, like the Phase 4 migration note in `UserRepositoryImpl`.

## Function Design

**Size:**

- Keep functions focused on a single responsibility.
- When a function starts mapping, filtering, and persisting in one place, split the transformation from the side effect.

**Parameters:**

- Prefer explicit parameters over ambient state.
- Use injected collaborators instead of reaching into singletons from call sites.

**Return Values:**

- Return `Result<T>` where the caller needs an explicit success/failure contract.
- Return `Flow<T>` or `StateFlow<T>` for observable state and lists for read operations.

## Module Design

**Exports:**

- Keep modules narrowly scoped to one responsibility.
- `RepositoryModule` binds interfaces, `DatabaseModule` creates the encrypted Room database and DAOs, `DataStoreModule` provides preferences storage, and `NetworkModule` exposes the Supabase client.

**Barrel Files:**

- No barrel files are detected.
- Import from the leaf package directly instead of building aggregating re-export layers.

**Where to Add New Code:**

- New repository interface: `app/src/main/java/com/aira/health/domain/repository`.
- New repository implementation: `app/src/main/java/com/aira/health/data/repository`.
- New Hilt binding: `app/src/main/java/com/aira/health/di`.
- New Compose screen: `app/src/main/java/com/aira/health/presentation/<feature>`.
- New shared utility: `app/src/main/java/com/aira/health/util`.

---

_Convention analysis: 2026-04-15_
