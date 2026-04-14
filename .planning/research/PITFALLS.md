# Pitfalls Research

**Domain:** Android Health & Fitness App (On-device AI + Wearables)
**Researched:** 2026-04-15
**Confidence:** HIGH

## Common Domain Mistakes

### 1. Health Connect Rate Limiting & Permission Revocation
- **Warning Sign**: Silent failures when pulling data. No updates in local DB.
- **Prevention**: Android explicitly rate-limits Health Connect to preserve battery. We must use `WorkManager` strictly respecting system constraints (e.g., sync primarily when `requiresCharging = true` or use large interval spans). Also, wrap all `HealthConnectClient` calls in try/catch for `SecurityException` since users can silently revoke a single permission (like SpO2) from OS settings without notifying the app.
- **Phase Addressed**: Phase 1 (Ingestion & Permissions).

### 2. Large AI Model Compute Out-Of-Memory (OOM)
- **Warning Sign**: The app consistently drops frames, ANRs (Application Not Responding), or gets killed aggressively by the OS low memory killer when opening the Coach UI.
- **Prevention**: Gemma 4 2B is huge. Only instantiate the LLM Engine when absolutely needed. Unload it immediately when exiting the Coach feature. Use `BaselineProfiles` to warm up execution paths, and implement a graceful fallback string/toast if RAM is too constrained to instantiate the model safely.
- **Phase Addressed**: Phase 3 (Intelligence).

### 3. Missing Hardware (e.g., No HRV Source)
- **Warning Sign**: App shows "0" or breaks math models (divide by zero) when calculating Recovery because the user's wearable does not supply beat-to-beat HRV metrics.
- **Prevention**: The Domain layer scoring algo must gracefully exclude/skip missing components, reweighting the remaining parameters dynamically (e.g., scale Sleep metrics higher if HRV is missing). Clearly reflect "Low Confidence" in the Confidence Engine rather than failing.
- **Phase Addressed**: Phase 1 (Core Engine Calculation).

### 4. Background Sync Over-Reporting (Phantom Drain)
- **Warning Sign**: Users leaving angry Play Store reviews explicitly citing Aira as draining battery 15%+ per day in battery usage stats.
- **Prevention**: We must coalesce database writes and sync events. Use Jetpack `DataStore` or lightweight in-memory cache to debounce updates. Batch all Health Connect reads and DB writes into a single transaction block. 
- **Phase Addressed**: Phase 1 and 2 (Architecture).

---
*Pitfalls research for: Android Health & Fitness App (On-device AI + Wearables)*
*Researched: 2026-04-15*
