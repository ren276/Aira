# Phase 2 Context: Data Ingestion

**Phase:** 2 — Data Ingestion
**Captured:** 2026-04-15
**Status:** Ready for planning

## Phase Goal

Hook the application strictly into Health Connect and Google Fit to extract robust biometric and sleep metrics asynchronously. Wearable data origins are mapped to confidence weights.

## Decisions

### 1. Foreground vs. Background Syncing
- **Strategy:** Hybrid. Execute background syncs using `WorkManager` (15-30m loops depending on OS allowances) to keep data relatively fresh.
- **Fast-Sync:** Immediately trigger a "fast-sync" sweep when the user opens the app to the foreground. This ensures the dashboards always reflect the absolute latest scores rather than stale cached data from the last background execution.

### 2. Historical Backfill Depth
- **Depth:** 14 Days.
- **Rationale:** On the very first launch, the ingestion pipeline will query Health Connect for 14 days of history. This prevents the "cold-start" problem where the user has to wait a week for baselines to adapt before they see any scores. By fetching 14 days immediately, the math engines can construct provisional averages dynamically.

### 3. Data Source Conflict Handling
- **Strategy:** "Highest Confidence Source Wins."
- **Multiple Wearables:** Instead of complex averaging algorithms, overlapping data for the exact same timeframe from different devices will be overridden by the higher-tier wearable.
- **Extensible Configuration:** The confidence router is not limited to just Garmin or Oura. It evaluates the source application package name from Health Connect and maps it to a broad tier list encompassing major wearables (eg: Fitbit, Samsung Health, Whoop, Withings, Coros, Apple).

### 4. Missing Data & Non-Wearable Handling
- **Gaps:** The dataset remains pure. If the user removes their wearable for a day, the app leaves those timeframes empty in the Room database rather than inserting "fake" missing entries. Phase 3 math engines explicitly process these gaps.
- **Phone-Only Fallbacks:** If the user owns no wearables at all, the application simply ingests and falls back to whatever native platform data resides in Health Connect as-is (e.g., standard Android phone pedometer or default sleep schedule sensors).

## Canonical Refs

- `.planning/PROJECT.md` — Core value, constraints, non-negotiables
- `.planning/REQUIREMENTS.md` — DATA-01, DATA-02, DATA-03, DATA-04
- `.planning/phases/01-environment-persistence/01-CONTEXT.md` — Hard reliance on Room as the SSOT and SQLCipher initialization paths for caching ingestion data safely.

## Success Criteria (from ROADMAP.md)

1. Background WorkManager periodically reads Health Connect data reliably.
2. Google Fit gracefully wraps endpoints for Android 10-13 fallback contexts.
3. Wearable data origins are mapped to confidence weights.
