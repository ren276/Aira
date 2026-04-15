# Codebase Concerns

**Analysis Date:** 2026-04-15

## Tech Debt

**Encrypted database bootstrap is brittle:**

- Issue: `KeystoreManager.getDatabasePassphrase()` reads `SecretKey.encoded`, but Android Keystore secret keys are typically non-exportable. `DatabaseModule.provideAiraDatabase()` calls this during singleton creation, so database startup can fail before the app renders.
- Files: `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt`, `app/src/main/java/com/aira/health/di/DatabaseModule.kt`, `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Impact: A cold-start crash blocks access to all encrypted local health data.
- Fix approach: Use a stable exportable seed protected by Keystore, or derive the SQLCipher passphrase without relying on `SecretKey.encoded`.

**Room migration policy will destroy local history:**

- Issue: `AiraDatabase.create()` uses `.fallbackToDestructiveMigration()` while `exportSchema = true` is enabled.
- Files: `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Impact: Any future schema version bump will wipe stored biometrics, corrections, conversations, and baselines.
- Fix approach: Add explicit Room migrations before raising the schema version and keep the exported schema artifacts in sync.

**Build config handling is permissive instead of fail-fast:**

- Issue: `getLocalProperty()` returns an empty string when a required value is missing, and both the `dev` and `staging` flavors read `SUPABASE_STAGING_*` values.
- Files: `app/build.gradle.kts`, `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt`
- Impact: Builds can succeed with blank Supabase configuration, and dev builds can hit the staging backend instead of an isolated environment.
- Fix approach: Fail the build on missing required properties and give the dev flavor its own backend values.

**Guest account upgrade does not migrate local data yet:**

- Issue: `UserRepositoryImpl.upgradeGuestAccount()` signs up the user and returns the remote session, but the Room-to-Supabase migration is still a TODO.
- Files: `app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt`
- Impact: Guest users who upgrade can strand or lose locally accumulated history and corrections.
- Fix approach: Add a resumable migration job and treat account upgrade as incomplete until local data has been reconciled.

## Known Bugs

**Google Fit fallback can fail silently:**

- Symptoms: The fallback repository returns empty lists on any read failure, which makes permissions errors, deprecated API behavior, and account issues look like valid "no data" states.
- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`, `app/src/main/java/com/aira/health/di/HealthDataModule.kt`
- Trigger: Missing Google account, incomplete Fit permissions, or a read failure in any of the `runCatching { ... }.getOrElse { emptyList() }` blocks.
- Workaround: None in code; the failure is swallowed.

**Fallback permissions check is incomplete:**

- Symptoms: `GoogleFitRepositoryImpl.isAvailable()` checks only heart-rate access, but the repository also reads sleep, calories, and steps.
- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`
- Trigger: Devices where only a subset of Google Fit scopes is granted.
- Workaround: The repository quietly degrades to empty data for the missing reads.

## Security Considerations

**Telemetry gating is only partially controlled:**

- Risk: `FirebaseApp.initializeApp(this)` runs for every build and only Crashlytics collection is explicitly toggled. Analytics and Performance dependencies are still present without a visible runtime consent gate.
- Files: `app/src/main/java/com/aira/health/AiraApplication.kt`, `app/build.gradle.kts`
- Current mitigation: Crashlytics collection is disabled in builds where `BuildConfig.ENABLE_CRASH_REPORTING` is false.
- Recommendations: Gate all telemetry behind explicit consent and verify the default-disabled state in release builds.

**Boot receiver is exported:**

- Risk: `BootReceiver` is exported without an app-specific permission, so other apps can send it broadcasts and force sync scheduling.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt`
- Current mitigation: The receiver only reacts to `Intent.ACTION_BOOT_COMPLETED`.
- Recommendations: Keep the receiver minimal, verify the action defensively, and reassess whether public export is still necessary.

## Fragile Areas

**Sync worker hides root causes:**

- Files: `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`
- Why fragile: `doWork()` catches all exceptions, retries or fails generically, and does not preserve the throwable context in logs.
- Safe modification: Surface structured diagnostics when sync fails so production issues can be triaged.
- Test coverage: `app/src/test/java/com/aira/health/data/worker/HealthSyncWorkerScheduleTest.kt` only verifies scheduling, not failure behavior.

**Google Fit integration is best-effort only:**

- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`, `app/src/main/java/com/aira/health/di/HealthDataModule.kt`
- Why fragile: The repository is injected whenever Health Connect is unavailable, but runtime success still depends on a Google account, Fit permissions, and deprecated API behavior.
- Safe modification: Validate all required scopes before using the fallback path and report partial ingestion explicitly.
- Test coverage: There are no direct unit tests for the Google Fit repository.

**Local data loss on version changes:**

- Files: `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Why fragile: Any schema evolution that reaches production without explicit migrations will delete user data.
- Safe modification: Add migration coverage before changing `version`.
- Test coverage: No database migration tests are present.

## Scaling Limits

**Anonymous and authenticated data paths are not fully separated yet:**

- Current capacity: Guest mode exists, but the upgrade path does not migrate local state.
- Limit: Scaling account conversion without a migration job will continue to create data islands.
- Scaling path: Treat local-to-cloud migration as a first-class workflow with retries and idempotency.

## Test Coverage Gaps

**Crypto/bootstrap path:**

- What's not tested: `KeystoreManager` and Room/SQLCipher startup.
- Files: `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt`, `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Risk: Startup-only encryption failures can ship unnoticed.
- Priority: High.

**Supabase and telemetry init:**

- What's not tested: `AiraApplication`, `SupabaseClientProvider`, and build-config driven environment wiring.
- Files: `app/src/main/java/com/aira/health/AiraApplication.kt`, `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt`, `app/build.gradle.kts`
- Risk: Privacy or initialization regressions can slip past unit tests.
- Priority: High.

**Manifest and boot-time behavior:**

- What's not tested: exported receiver behavior, boot rescheduling, and permission alias wiring.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt`
- Risk: Security or boot-time regressions only surface in on-device testing.
- Priority: Medium.

**Fallback ingestion behavior:**

- What's not tested: `GoogleFitRepositoryImpl` partial permissions, read failures, and empty-data scenarios.
- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`
- Risk: The fallback can look healthy while ingesting nothing.
- Priority: High.

---

_Concerns audit: 2026-04-15_
