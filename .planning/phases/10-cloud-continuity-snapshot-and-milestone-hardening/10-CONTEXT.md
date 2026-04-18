# Phase 10: Cloud Continuity Snapshot and Milestone Hardening - Context

**Gathered:** 2026-04-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Enable privacy-safe cloud continuity snapshots and end-to-end hardening for v1.1 release readiness.

This phase delivers only:
- compact Supabase snapshot write/read restore for computed continuity data,
- explicit local reset flow that performs final snapshot upload before destructive wipe,
- release hardening verification for privacy boundaries and stability.

Out of scope for this phase:
- raw biometric event upload,
- cloud-hosted core coaching inference,
- new sensor ingestion capability.

</domain>

<decisions>
## Implementation Decisions

### Snapshot Content Contract
- **D-01:** Continuity snapshots are compact and derived-only: daily computed scores, confidence, prediction deltas, guidance summaries, weekly planning highlights, and app continuity settings.
- **D-02:** Raw biometric events and detailed low-level internals are excluded from snapshot payloads.
- **D-03:** Snapshot schema is continuity-focused and designed for safe restore after reinstall.

### Sync and Retry Behavior
- **D-04:** Upload policy is hybrid: event-driven uploads on key continuity changes plus periodic WorkManager backstop sync.
- **D-05:** Upload failures use retry with backoff and offline-safe re-attempt behavior.
- **D-06:** Existing local-first behavior remains default; cloud sync augments continuity only.

### Restore Behavior
- **D-07:** Restore is user-mediated, not automatic: user is asked each time whether to apply cloud continuity snapshot after reinstall/login.
- **D-08:** If user accepts restore, continuity data is applied in a deterministic restore path and local pipelines can continue normal recomputation after restore.

### Reset Flow Safety
- **D-09:** Explicit local reset must attempt final snapshot upload before wipe.
- **D-10:** If final snapshot upload fails, destructive reset is blocked by default and user is prompted to retry (with explicit irreversible local-only override path if needed).
- **D-11:** Reset UX must clearly communicate privacy-safe boundaries and irreversible effects.

### Hardening and Release Gates
- **D-12:** Phase hardening uses strict gate criteria: privacy-boundary checklist pass, core unit suite pass, key integration path pass, and no unresolved high-severity findings at close.
- **D-13:** Any non-critical gaps must be explicitly documented and triaged before milestone close.

### Locked Constraints Carried Forward
- **D-14:** Local-first privacy constraints from prior phases remain binding: no raw biometric data leaves device.
- **D-15:** Existing coaching/runtime safety posture remains in force for phase interactions.

### the agent's Discretion
- Exact snapshot table/column naming and DTO naming conventions.
- Internal job orchestration details for event triggers versus periodic backstop.
- Final restore prompt copy and UX microcopy, as long as D-07 through D-11 remain intact.

</decisions>

<specifics>
## Specific Ideas

- Preserve the current Settings positioning where cloud backup preference already exists, but wire it to real continuity behavior.
- Keep reset flow conservative: prevent accidental destructive wipes when continuity backup did not complete.
- Restore should feel explicit and trustworthy rather than silent.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and Requirements
- .planning/ROADMAP.md - Phase 10 goal, success criteria, and plan boundaries.
- .planning/REQUIREMENTS.md - BACK-01 and BACK-02 requirement definitions.
- .planning/PROJECT.md - privacy-first product constraints and milestone intent.

### Prior Locked Context
- .planning/phases/08-causal-insight-and-personalization-core/08-CONTEXT.md - confidence, personalization, and privacy presentation constraints.
- .planning/phases/09-prediction-what-if-and-athlete-guidance/09-CONTEXT.md - local-only guidance/runtime boundaries and coach UX continuity.

### Existing Implementation Seams
- app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt - existing Supabase client and modules available for continuity integration.
- app/src/main/java/com/aira/health/di/NetworkModule.kt - dependency injection seam for Supabase client access.
- app/src/main/java/com/aira/health/presentation/settings/SettingsScreen.kt - current Cloud Backup Preference UI entry point.
- app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt - DataStore-backed cloud backup preference state.
- app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt - periodic and immediate worker scheduling patterns for sync backstop integration.
- app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt - existing account/sign-out surface relevant to reset flow placement.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Supabase client singleton is already wired and injectable via DI.
- Settings already has cloud backup preference storage and UI toggle in place.
- WorkManager patterns already exist for periodic and immediate sync scheduling.
- Account surface already supports identity actions and can host reset entry points.

### Established Patterns
- Domain-usecase orchestration with local Room/DataStore persistence first.
- Privacy-safe aggregation contract is already enforced in AI and prompt paths.
- Feature state is generally surfaced through ViewModel + StateFlow + Compose cards/actions.

### Integration Points
- Add continuity snapshot domain/data contracts and persistence mapping near existing score/guidance aggregates.
- Connect upload/restore orchestration to Supabase via repository/use-case seams, not direct UI networking.
- Extend settings/account flows with explicit restore/reset decision points and safety confirmations.
- Reuse HealthSyncWorker-style scheduling semantics for periodic continuity sync backstop.

</code_context>

<deferred>
## Deferred Ideas

- Multi-device conflict-free merge engine beyond current single-user continuity semantics.
- Full raw-data cloud backup capability (explicitly out of scope).
- Advanced historical replay/rollback UI for multiple snapshot versions.

</deferred>

---

*Phase: 10-cloud-continuity-snapshot-and-milestone-hardening*
*Context gathered: 2026-04-18*
