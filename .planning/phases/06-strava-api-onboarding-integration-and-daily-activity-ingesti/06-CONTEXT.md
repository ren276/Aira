# Phase 06: Strava API Onboarding Integration and Daily Activity Ingestion - Context

**Gathered:** 2026-04-16
**Status:** Ready for planning
**Source:** User discussion decisions + codebase survey + Strava official docs

<domain>
## Phase Boundary

This phase introduces first-class Strava integration for users who want external activity sync beyond passive Health Connect ingestion.

The integration starts at onboarding with an explicit Strava connection option, then adds OAuth token lifecycle management, incremental activity sync, and mapping into Aira's existing local scoring pipeline.

Primary persistence is Room so Aira remains fully local-first and resilient when APIs are unavailable.
An optional Health Connect write-back path may be offered as a user-controlled mirror for interoperability.

This phase is an integration and ingestion phase, not a redesign of scoring formulas.

</domain>

<decisions>
## Implementation Decisions

### Onboarding-first integration entry

- Add a new onboarding step before Health Connect permission batching where users must connect Strava to proceed.
- Keep reconnect and retry UX explicit so users are not trapped by transient auth failures.
- Extend onboarding step count and route flow accordingly.

### OAuth and token lifecycle

- Use Strava OAuth authorization code flow with secure redirect handling.
- Persist access token, refresh token, expiry, athlete id, granted scopes, and last sync cursor in encrypted local storage.
- Refresh tokens before expiry and handle invalid_grant / revoked tokens by gracefully requiring reconnect.

### Activity ingestion strategy

- Source endpoint: GET /athlete/activities with after/before + pagination.
- Perform incremental sync with cursor windowing and idempotency keys by Strava activity id.
- Batch process pages and stop early when older than local cursor.
- First connection performs a full historical backfill of all available activities before switching to incremental sync.

### Mapping into Aira domain

- Map each activity into workout-style local records that feed existing daily strain and activity rollups.
- Include all Strava activity types in ingestion; guarantee full mapping quality for run/ride/walk/hike and apply safe fallback mapping for remaining sport types.
- Use sport_type/type and available fields (distance, moving_time, elapsed_time, average_heartrate, max_heartrate, total_elevation_gain, kilojoules, calories where present).
- Preserve raw source identifiers and confidence metadata for auditability.

### Source conflict resolution

- If both Health Connect and Strava represent the same workout, keep a single merged record chosen by highest-confidence-source-wins.
- Persist source provenance to support explainability and future audit/debug of merge outcomes.

### Persistence contract

- Mandatory persistence target: Room (workout_sessions plus any new Strava sync metadata tables).
- Optional persistence target: Health Connect write-back behind explicit user toggle and capability checks.
- Room remains source of truth for Aira scoring and UI.

### Rate limiting and resilience

- Respect Strava rate limits and include backoff/defer behavior in workers.
- Treat HTTP 401, 403, 429, and 5xx distinctly with retry policy and user-facing diagnostics.
- Keep ingestion partial-success safe: page-level commits, cursor updates only after successful page persistence.
- Read and enforce rate headers (`X-RateLimit-Limit`, `X-RateLimit-Usage`, `X-ReadRateLimit-Limit`, `X-ReadRateLimit-Usage`) and throttle before hitting hard caps.

### Privacy and security

- Never upload raw biometric time series to cloud.
- Store OAuth secrets and tokens only in local secure storage.
- Log only redacted sync telemetry (counts, durations, status), never token bodies.

### the agent's Discretion

- Exact UX copy and visual composition of the Strava onboarding screen.
- Whether Health Connect write-back ships in the same plan or behind a staged feature flag, as long as Room persistence ships in this phase.
- Exact retry/backoff constants as long as they remain rate-limit-safe and battery-aware.

</decisions>

<canonical_refs>

## Canonical References

### Roadmap and phase state

- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `.planning/PROJECT.md`

### Existing app integration points

- `app/src/main/java/com/aira/health/presentation/navigation/AppEntryRoute.kt`
- `app/src/main/java/com/aira/health/presentation/navigation/AppEntryViewModel.kt`
- `app/src/main/java/com/aira/health/presentation/onboarding/OnboardingFlow.kt`
- `app/src/main/java/com/aira/health/presentation/onboarding/PermissionBatchScreen.kt`
- `app/src/main/java/com/aira/health/presentation/onboarding/PermissionViewModel.kt`
- `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`
- `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt`
- `app/src/main/java/com/aira/health/domain/usecase/ComputeDailyScoresUseCase.kt`
- `app/src/main/java/com/aira/health/data/local/model/WorkoutSession.kt`
- `app/src/main/java/com/aira/health/data/local/dao/WorkoutSessionDao.kt`
- `app/src/main/java/com/aira/health/data/local/model/DailyMetrics.kt`
- `app/src/main/java/com/aira/health/di/DataStoreModule.kt`
- `app/src/main/java/com/aira/health/di/NetworkModule.kt`

### Strava official docs

- Getting Started: https://developers.strava.com/docs/getting-started/
- Authentication: https://developers.strava.com/docs/authentication/
- API Reference root: https://developers.strava.com/docs/reference/
- List Athlete Activities (getLoggedInAthleteActivities): https://developers.strava.com/docs/reference/#api-Activities-getLoggedInAthleteActivities
- Rate Limits: https://developers.strava.com/docs/rate-limits/

### Research findings applied to this phase

- Mobile OAuth authorize endpoint for Android: `https://www.strava.com/oauth/mobile/authorize`.
- Token exchange and refresh endpoint: `POST https://www.strava.com/api/v3/oauth/token`.
- Access tokens are short-lived (about 6 hours), and refresh tokens can rotate on refresh and must be persisted from latest response.
- List athlete activities requires `activity:read`; `activity:read_all` is required to include "Only Me" activities.
- Default app limits: 200 requests per 15 minutes and 2000 per day overall; non-upload default limits: 100 per 15 minutes and 1000 per day.

</canonical_refs>

<code_context>

## Existing Code Insights

### Reusable assets

- Onboarding gating is already centralized in `AppEntryRoute` + `AppEntryViewModel` and can be extended with one additional pre-permission step.
- Daily score computation already ingests workout-derived active minutes via `WorkoutSessionDao` in `HealthSyncWorker`.
- Room models for workouts and daily metrics already exist and are sufficient for a first integration path.

### Established patterns

- ViewModel + StateFlow screen state.
- Local-first scoring pipeline with WorkManager orchestration.
- Hilt modules for DI boundaries.

### Integration gaps to fill

- No existing Strava API client, token model, or sync worker.
- No external-source onboarding UI before permission batches.
- No idempotent external activity ingestion contract yet.

</code_context>

<specifics>
## Specific Ideas

- Add `StravaConnectionScreen` between auth onboarding and permission onboarding.
- Add `StravaAuthRepository` and `StravaActivityRepository` contracts for testable boundaries.
- Add a sync cursor model keyed by athlete id to support incremental pull.
- Persist Strava activity id in local session metadata for dedupe and re-sync safety.
- Use `sourcePackage = "com.strava"` and confidence routing consistent with current source strategy.
- Trigger sync on app-open foreground refresh plus daily background worker cadence.

</specifics>

<deferred>
## Deferred Ideas

- Strava webhooks for near-real-time sync.
- Full stream ingestion (lat/lng/watts/time-series) beyond summary activities.
- Multi-provider external integrations beyond Strava in this phase.

</deferred>

---

_Phase: 06-strava-api-onboarding-integration-and-daily-activity-ingesti_
_Context gathered: 2026-04-16_
