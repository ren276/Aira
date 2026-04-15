# External Integrations

**Analysis Date:** 2026-04-15

## APIs & External Services

**Health Connect:**

- Health Connect is the primary on-device health data source.
- SDK/client: `androidx.health.connect:connect-client`.
- Wiring: `app/src/main/java/com/aira/health/di/HealthDataModule.kt`, `app/src/main/java/com/aira/health/data/repository/HealthConnectRepositoryImpl.kt`, and `app/src/main/java/com/aira/health/util/permission/HealthPermissionManager.kt`.
- Manifest surface: `app/src/main/AndroidManifest.xml` declares the runtime and health permissions plus the `VIEW_PERMISSION_USAGE` activity alias.

**Google Fit:**

- Google Fit is the legacy fallback on devices where Health Connect is unavailable.
- SDK/client: `com.google.android.gms:play-services-fitness` and `com.google.android.gms:play-services-auth`.
- Wiring: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt` reads historical heart rate, sleep, calories, and steps, while `HealthDataModule.kt` selects it when Health Connect is unavailable.
- Scope: read-only fallback for older devices; new write paths are handled through Health Connect permissions.

**Supabase:**

- Supabase is the backend integration for authentication and remote sync surfaces.
- SDK/client: `io.github.jan-tennert.supabase:bom` with `auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt`, and Ktor Android.
- Wiring: `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt` creates the singleton client from `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`, and `app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt` uses Supabase auth for Google and email/password flows.
- State: guest mode bypasses Supabase entirely and returns a local pseudo-session.

**Firebase:**

- Firebase App initialization and Crashlytics toggling are wired in `app/src/main/java/com/aira/health/AiraApplication.kt`.
- SDKs/plugins: Firebase BOM plus Crashlytics, Analytics, and Performance dependencies are declared in `app/build.gradle.kts`.
- State: Crashlytics collection is gated by `BuildConfig.ENABLE_CRASH_REPORTING`; Analytics and Performance have no app-side call sites in the current source tree.

**WorkManager:**

- WorkManager drives periodic and immediate sync jobs through `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`.
- Boot restart: `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt` reschedules work after `BOOT_COMPLETED`.
- App startup: `AiraApplication.kt` provides the Hilt worker factory via `Configuration.Provider`.

**Biometric App Lock:**

- Optional biometric unlock is implemented in `app/src/main/java/com/aira/health/util/security/BiometricManager.kt`.
- SDK/client: `androidx.biometric:biometric`.
- Scope: UI lock only; it is explicitly separate from database encryption.

**Monetization / AI / Storage SDKs declared but not yet wired:**

- RevenueCat is declared in `app/build.gradle.kts`, but no app code references were found in `app/src/main/java/**`.
- MediaPipe Tasks GenAI is declared in `app/build.gradle.kts`, but no runtime integration code exists yet.
- TensorFlow Lite and TensorFlow Lite Support are declared in `app/build.gradle.kts`, but no model execution code exists yet.
- Supabase Storage and Realtime are installed in `SupabaseClientProvider.kt`, but no repository or feature code currently consumes them.

## Data Storage

**Databases:**

- Room with SQLCipher is the local persistence layer.
- Wiring: `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt` plus `app/src/main/java/com/aira/health/di/DatabaseModule.kt`.
- Encryption key: generated and stored in Android Keystore by `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt`.
- Migration policy: `AiraDatabase.kt` currently uses `fallbackToDestructiveMigration()`.

**File Storage:**

- Local filesystem plus app-private Android storage for config and database files.
- Supabase Storage is declared, but there is no call site yet that uploads or downloads objects.

**Caching / Preferences:**

- `androidx.datastore:datastore-preferences` stores sync timestamps and lightweight app state.
- Wiring: `app/src/main/java/com/aira/health/di/DataStoreModule.kt` and `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt`.

## Authentication & Identity

**Auth Provider:**

- Supabase auth is the primary identity provider.
- Implementations: `app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt` and `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt`.
- Supported flows: Google OAuth, email/password sign-in, email/password sign-up, guest mode, sign-out, and guest upgrade.
- Token persistence: the Supabase SDK stores tokens in encrypted shared preferences by default.

**Google identity use:**

- `com.google.android.gms:play-services-auth` is also used for Google Sign-In state checks in the legacy Google Fit repository.

## Monitoring & Observability

**Error Tracking:**

- Firebase Crashlytics is the only actively wired error-tracking integration.
- Build-time toggle: `ENABLE_CRASH_REPORTING` in `app/build.gradle.kts` flavor fields.

**Logs:**

- WorkManager logging level is set in `app/src/main/java/com/aira/health/AiraApplication.kt`.
- No separate logging backend is wired in the current source tree.

## CI/CD & Deployment

**Hosting:**

- GitHub Actions builds on `ubuntu-latest`.

**CI Pipeline:**

- `.github/workflows/ci.yml` sets up JDK 17, installs Android SDK API 36/build-tools 36.0.0, materializes `app/google-services.json` from `GOOGLE_SERVICES_JSON_B64`, runs `./gradlew detekt`, runs unit tests, and builds `assembleProdDebug`.
- Artifacts uploaded: test reports and the prod debug APK.
- No separate deployment workflow is present.

## Environment Configuration

**Required env vars:**

- `SUPABASE_STAGING_URL`
- `SUPABASE_STAGING_ANON_KEY`
- `SUPABASE_PROD_URL`
- `SUPABASE_PROD_ANON_KEY`
- `GOOGLE_SERVICES_JSON_B64` in GitHub Actions

**Secrets location:**

- GitHub Actions secrets are consumed in `.github/workflows/ci.yml`.
- Local flavor configuration is sourced from `local.properties`, Gradle properties, or environment variables in `app/build.gradle.kts`.

## Webhooks & Callbacks

**Incoming:**

- None detected.

**Outgoing:**

- Supabase auth uses OAuth and email/password sign-in flows through `UserRepositoryImpl.kt`.
- No explicit webhook publisher or callback endpoint is defined in the current codebase.

---

_Integration audit: 2026-04-15_
