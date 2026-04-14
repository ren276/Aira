# Architecture Research

**Domain:** Android Health & Fitness App (On-device AI + Wearables)
**Researched:** 2026-04-15
**Confidence:** HIGH

## Component Boundaries & System Structure

### 1. Presentation Layer (Jetpack Compose & ViewModels)
- **Role**: Render state, observe `StateFlow` from ViewModels, dispatch intents.
- **Rules**: Absolutely zero Health Connect API calls or database logic in UI elements. Strict adherence to UDF (Unidirectional Data Flow). Compose views observe state and pass lambdas for UI events.

### 2. Domain Layer (Use Cases & Pure Logic)
- **Role**: Mathematical core executing scoring formulas (Recovery, Sleep, Strain, etc). EMA Baseline adjustments, and Anomaly formatting.
- **Rules**: Must contain exactly ZERO Android Framework dependencies (No Context, No Log, No HealthConnectClient). This makes the core scoring algorithms 100% unit-testable.

### 3. Data Layer (Repositories & Raw Storage)
- **Role**: Mediate between `HealthConnectClient`, Google Fit fallback APIs, local `Room` DB, and `Supabase` network endpoints.
- **Rules**: Implements Domain layer repository interfaces. Manages caching, SQLCipher encryption keys, and raw biometric normalization. 

### 4. Machine Learning & AI Layer (MediaPipe & TFLite)
- **Role**: Provide on-device inference via decoupled modules.
- **Rules**: Model instantiation must be isolated to avoid UI thread blockages. Background thread dispatchers exclusively (Dispatchers.Default / Dispatchers.IO). LLM context building happens here.

## Data Flow

1. **Ingestion Loop (WorkManager - Every 15~30 mins)**:
   - `HealthSyncWorker` wakes up -> Asks Data Layer to fetch from Health Connect -> Standardizes data -> Emits to Domain layer for baseline check -> Data Layer saves to `Room`.
2. **Scoring Engine**:
   - `CalculateDailyScoresUseCase` triggers post-sync. Computes values pure mathematically -> Dispatches persistence to Repositories.
3. **UI Sync**:
   - `ScoreViewModel` constantly observes Room DAOs via `Flow<DailyMetrics>`. UI updates instantaneously when underlying Room DB updates.

## Build Order / Dependencies

1. **Phase 1: Persistence & Extraction** - SQLCipher Room definitions, Health Connect + Fit integration models.
2. **Phase 2: Mathematical Core** - Implement scoring logic using mock inputs before touching UI.
3. **Phase 3: State & UI** - Build Jetpack Compose view model bindings, Canvas graphics, and screen navigation.
4. **Phase 4: ML & Intelligence** - Map actual user data streams into MediaPipe/TFLite models for insights and chat context.

---
*Architecture research for: Android Health & Fitness App (On-device AI + Wearables)*
*Researched: 2026-04-15*
