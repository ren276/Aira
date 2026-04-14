# Phase 2 Research: Data Ingestion

## What do I need to know to PLAN this phase well?

### 1. Health Connect API Landscape
- **Library**: `androidx.health:health-connect-client:1.1.0-alpha07`.
- **Availability Check**: Before attempting to read, the app must call `HealthConnectClient.sdkStatus(context)`. If it returns `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED`, the user must be prompted to install/update the provider package.
- **Reading Data**: Queries are made via `readRecords(ReadRecordsRequest(...))`. The request takes a `TimeRangeFilter`.
- **Deduplication**: Health Connect automatically deduplicates data based on `clientRecordId` and `clientRecordVersion`.
- **Packages**: Data origins can be identified by the `metadata.dataOrigin.packageName`. This is crucial for the "Highest Confidence Source Wins" strategy.

### 2. Google Fit API (Fallback)
- **Library**: `com.google.android.gms:play-services-fitness:21.1.0`.
- **Components**: `HistoryClient` and `SensorsClient`. For historical baseline extraction (14 days), `HistoryClient.readData(DataReadRequest)` is used.
- **Deprecation Warning**: Google Fit REST API is deprecated, but the Android SDK continues to function for fallback scenarios (Android 10-13) where Health Connect is not supported or installed.

### 3. WorkManager Integration
- **Components**: `androidx.work:work-runtime-ktx:2.9.1`.
- **Implementation**: Needs a `CoroutineWorker` since Health Connect reads are suspend functions.
- **Constraints**: 
  - `NetworkType.CONNECTED` (if we need to sync to Supabase immediately after taking the local read, though Phase 2 goal is purely ingestion to local DB).
  - Ingestion does not strictly require the network if it's purely pulling from Health Connect to Room.
- **Scheduling**: 15m charging or 30m idle intervals are typically requested using `PeriodicWorkRequestBuilder`.

### 4. Wearable Source Detection & Ranking (Confidence Weights)
- Must maintain a static or configurable map of package names to confidence tiers.
- **Tier 1 (Highest)**: Dedicated medical or high-precision wearables (e.g., `com.whoop.android`, `com.ouraring.oura`).
- **Tier 2**: Standard fitness watches (e.g., `com.garmin.android.apps.connectmobile`, `com.fitbit.FitbitMobile`).
- **Tier 3**: Smartwatches (e.g., `com.samsung.android.app.shealth`, `com.google.android.apps.fitness` via WearOS).
- **Tier 4 (Lowest)**: Phone-only fallback (Health Connect native pedometer algorithms).

### 5. Architectural Considerations
- **Data Layers**: We need `HealthDataRepository` interfaces in the `domain` layer.
- **Implementations**:
  - `HealthConnectRepositoryImpl` (primary)
  - `GoogleFitRepositoryImpl` (fallback)
- **Use Cases**: `IngestHealthDataUseCase` to orchestrate reading from the appropriate source, resolving source conflicts based on the Confidence Map, and persisting to the Room DB (which was established in Phase 1).
- **Dependency**: The actual Room DAOs from Phase 1 are required to store the synced records.

### 6. Security & Privacy
- Under no circumstances is the raw biometric data fetched from Health Connect/Google Fit sent directly to remote networks (Supabase) in this phase. It is strictly saved to the local Room database using SQLCipher.

## RESEARCH COMPLETE
