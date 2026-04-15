# Technology Stack

**Analysis Date:** 2026-04-15

## Languages

**Primary:**

- Kotlin 2.0.21 - all app logic under `app/src/main/java/com/aira/health/**`.

**Secondary:**

- Java 17 - Android toolchain and CI runtime target.
- XML - manifest and resource definitions under `app/src/main`.

## Runtime

**Environment:**

- Android app targeting API 35 with `compileSdk = 36`, `compileSdkExtension = 19`, and `minSdk = 29`.
- Gradle 8.13 with Android Gradle Plugin 8.13.2.

**Package Manager:**

- Gradle wrapper 8.13.
- Lockfile: not applicable.

## Frameworks

**Core:**

- Jetpack Compose BOM 2024.10.01 - primary UI toolkit in `app/build.gradle.kts`.
- Hilt 2.52 - dependency injection for repositories, workers, and security helpers.
- Room 2.6.1 + SQLCipher 4.5.4 - encrypted local persistence in `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`.
- WorkManager 2.9.1 - periodic and immediate sync orchestration in `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`.
- DataStore 1.1.1 - preferences and sync cursor storage in `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt`.
- Supabase Kotlin SDK 3.0.0 - auth, Postgrest, Realtime, and Storage wiring in `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt`.
- Health Connect 1.1.0 - primary health data source through `app/src/main/java/com/aira/health/data/repository/HealthConnectRepositoryImpl.kt`.
- Google Play Services Fitness 21.1.0 - legacy fallback source through `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`.

**Testing:**

- JUnit 5.11.3 - unit tests in `app/src/test`.
- MockK 1.13.13 - mocking in JVM tests.
- Turbine 1.2.0 - Flow assertions.
- Compose UI Test - UI test support declared in `app/build.gradle.kts`.

**Build/Dev:**

- KSP 2.0.21-1.0.28 - Room and Hilt code generation.
- Google Services 4.4.2 - Firebase configuration processing.
- Firebase Crashlytics plugin 3.0.2 and Firebase Performance plugin 1.4.2 - Gradle integrations declared in the app module.

## Key Dependencies

**Critical:**

- `androidx.health.connect:connect-client` - device health data access and permission handling.
- `io.github.jan-tennert.supabase:bom`, `auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt` - backend client surface for authentication and sync.
- `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler` - local persistence.
- `net.zetetic:android-database-sqlcipher` and `androidx.security:security-crypto` - encrypted at-rest storage.
- `androidx.work:work-runtime-ktx` - background ingestion scheduling.
- `com.google.dagger:hilt-android` and `androidx.hilt:hilt-*` - DI and Hilt workers.
- `com.google.firebase:firebase-bom` plus `firebase-crashlytics-ktx`, `firebase-analytics-ktx`, and `firebase-perf-ktx` - Firebase integration surface.
- `com.google.mediapipe:tasks-genai`, `org.tensorflow:tensorflow-lite`, and `tensorflow-lite-support` - on-device AI/ML libraries declared in the app module.
- `com.revenuecat.purchases:purchases` - monetization SDK declared in the app module.

**Infrastructure:**

- `io.coil-kt:coil-compose` - image loading support for Compose UI.
- `org.jetbrains.kotlinx:kotlinx-serialization-json` - JSON serialization support.
- `org.jetbrains.kotlinx:kotlinx-coroutines-*` - async and Play Services bridges.
- `androidx.biometric:biometric` - optional app-lock flow in `app/src/main/java/com/aira/health/util/security/BiometricManager.kt`.
- `androidx.datastore:datastore-preferences` - key-value preferences.
- `com.google.android.gms:play-services-auth` - Google OAuth and legacy account access.
- `com.google.android.material:material` - Material components used alongside Compose.

## Configuration

**Environment:**

- Flavor-specific `BuildConfig` fields are derived from `local.properties`, Gradle properties, or environment variables in `app/build.gradle.kts`.
- Required values include `SUPABASE_STAGING_URL`, `SUPABASE_STAGING_ANON_KEY`, `SUPABASE_PROD_URL`, and `SUPABASE_PROD_ANON_KEY`.
- Feature flags include `ENABLE_FLAG_SECURE` and `ENABLE_CRASH_REPORTING`.
- Firebase config is supplied by `app/google-services.json`; the repository also includes `app/google-services.json.example`.
- GitHub Actions restores `app/google-services.json` from the `GOOGLE_SERVICES_JSON_B64` secret in `.github/workflows/ci.yml`.

**Build:**

- Root and module build files live in `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts`.
- Versioning is centralized in `gradle/libs.versions.toml`.
- The Gradle wrapper is pinned in `gradle/wrapper/gradle-wrapper.properties`.

## Platform Requirements

**Development:**

- JDK 17.
- Android SDK platform 36 and build-tools 36.0.0 in CI.
- AndroidX is enabled, Jetifier is enabled, and non-transitive R classes are enabled in `gradle.properties`.

**Production:**

- Android 10+ devices are supported through `minSdk = 29`.
- Health Connect is the primary health-data runtime on supported devices; Google Fit is the fallback on older devices.

---

_Stack analysis: 2026-04-15_
