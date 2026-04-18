# Phase 10: Cloud Continuity Snapshot and Milestone Hardening - Research

Researched: 2026-04-18
Domain: Android Kotlin local-first continuity snapshot sync, restore, and reset hardening
Overall confidence: MEDIUM-HIGH

## Standard Stack

Use existing project stack and patterns; do not add a new backend SDK or scheduler layer for this phase.

| Area | Use | Why this is standard here | Confidence | Evidence |
|---|---|---|---|---|
| Remote continuity transport | Existing Supabase Kotlin client via DI | Supabase client is already provisioned in app DI and supports Postgrest/Auth setup used by existing account flows. | HIGH | [VERIFIED: app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt], [VERIFIED: app/src/main/java/com/aira/health/di/NetworkModule.kt] |
| Local continuity persistence | Room entities + DAO + explicit migration | Existing app data model uses Room with explicit entities/DAO and phase-based migrations for new tables. | HIGH | [VERIFIED: app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt], [VERIFIED: app/src/main/java/com/aira/health/data/local/db/migrations/Migration09PredictionTables.kt], [CITED: https://developer.android.com/training/data-storage/room/migrating-db-versions] |
| Background backstop sync | WorkManager periodic + immediate one-time work | Existing sync behavior already follows this pattern and survives process/app restarts. | HIGH | [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt], [CITED: https://developer.android.com/topic/libraries/architecture/workmanager] |
| User preference gating | DataStore-backed cloud backup preference in Settings | Cloud backup preference is already exposed and persisted; this phase should wire behavior to that gate. | HIGH | [VERIFIED: app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt], [VERIFIED: app/src/main/java/com/aira/health/presentation/settings/SettingsScreen.kt] |
| Reset and account actions | Existing Account screen + ViewModel action patterns | Sign-out and disconnect flows already use explicit user actions with ViewModel orchestration. | HIGH | [VERIFIED: app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt] |

Prescriptive decisions for Phase 10 implementation planning:

- Keep continuity payload derived-only and compact, with no raw biometric event rows. [VERIFIED: Phase 10 context D-01, D-02]
- Upload strategy should combine event-driven sync with periodic backstop and retry/backoff semantics. [VERIFIED: Phase 10 context D-04, D-05]
- Restore is user-mediated on reinstall/login and must be deterministic. [VERIFIED: Phase 10 context D-07, D-08]
- Reset flow must attempt final upload and block destructive wipe by default on failure. [VERIFIED: Phase 10 context D-09, D-10]

## Architecture Patterns

### Pattern A: Continuity snapshot contract and mapper boundary

1. Define a compact continuity DTO containing only derived/summary fields.
2. Build mapper functions from existing local aggregate entities (daily metrics, guidance summaries, weekly draft, settings flags).
3. Validate that payload excludes raw records by type boundary (domain contract accepts aggregates only).

Why: Enforces privacy-safe payload scope and prevents accidental raw-data inclusion during future extensions.

### Pattern B: Hybrid upload orchestration (event + periodic)

1. Trigger upload on key continuity state changes (for example: daily aggregate update, guidance refresh, weekly plan recompute).
2. Keep periodic WorkManager as reconciliation backstop.
3. Persist sync status/attempt metadata locally so UI can explain current continuity state and retries.

Why: Event-driven gives freshness; periodic covers missed triggers and transient failures.

### Pattern C: User-mediated restore with deterministic apply

1. On reinstall/login with cloud backup enabled, fetch latest snapshot metadata.
2. Show user explicit choice to restore or skip.
3. If accepted, apply continuity tables in one deterministic transaction path with version-aware mapping.
4. Resume normal local recomputation after restore.

Why: Matches locked decision for explicit trust and avoids silent data replacement.

### Pattern D: Reset safety gate with blocking semantics

1. User starts explicit reset from account/privacy surface.
2. Attempt final continuity upload.
3. If upload succeeds, proceed with confirmed wipe.
4. If upload fails, block destructive wipe by default and present retry path plus explicit irreversible local-only override.

Why: Protects continuity while still allowing informed irreversible reset.

### Pattern E: Hardening as verifiable gates, not narrative checks

1. Privacy boundary checks verify payload schema contains no raw columns.
2. Core unit and integration checks verify sync/restore/reset orchestration paths.
3. Gate close only when no unresolved high-severity security/privacy findings remain.

Why: Converts release-readiness from qualitative review to measurable criteria.

## Do Not Hand-Roll

| Problem | Avoid | Use instead | Why |
|---|---|---|---|
| Reliable background retries | Custom timers/threads for retry loops | WorkManager retry/backoff and unique work policies | Existing app already uses WorkManager lifecycle semantics and avoids duplicate scheduling races. [CITED: https://developer.android.com/topic/libraries/architecture/workmanager] |
| Data migration safety | Destructive fallback as default for new continuity tables | Explicit Room migration + migration tests | Required to preserve continuity history and settings across upgrades. [CITED: https://developer.android.com/training/data-storage/room/migrating-db-versions] |
| Restore conflict behavior | Implicit silent overwrite | User prompt + deterministic apply contract | Locked decision requires explicit user mediation for restore. [VERIFIED: Phase 10 context D-07] |
| Privacy enforcement | Ad hoc field filtering near network call | Continuity payload contract + mapper-level whitelist | Centralized contract prevents accidental raw field leakage in later refactors. |

## Common Pitfalls

1. Treating the existing cloud backup toggle as sufficient without wiring actual upload/restore paths.
- Prevention: Add explicit use-case orchestration and sync state persistence behind the toggle.
- Confidence: HIGH. [VERIFIED: settings currently says cloud sync not wired]

2. Allowing reset wipe to proceed after failed final upload by default.
- Prevention: Block wipe by default and require explicit irreversible override.
- Confidence: HIGH. [VERIFIED: Phase 10 context D-10]

3. Mixing raw and derived data in continuity payload over time.
- Prevention: Keep a strict snapshot DTO with derived-only fields and tests asserting excluded raw fields.
- Confidence: HIGH. [VERIFIED: prior privacy constraints in Phase 8/9 contexts]

4. Assuming periodic sync alone covers freshness.
- Prevention: Keep hybrid event-driven trigger plus periodic backstop.
- Confidence: MEDIUM-HIGH. [VERIFIED: Phase 10 context D-04]

5. Closing phase without measurable hardening gates.
- Prevention: Define strict pass/fail checks and security/open-threat criteria in plan verification.
- Confidence: HIGH. [VERIFIED: Phase 10 context D-12]

## Recommended Validation Checks

| Requirement | Validation target | Command/check | Expected |
|---|---|---|---|
| BACK-01 | Snapshot upload/read/restore lifecycle | .\gradlew.bat :app:testDevDebugUnitTest --tests "*Continuity*" --tests "*Snapshot*" | Continuity payload writes/reads are deterministic and privacy-safe |
| BACK-02 | Reset flow final-upload gate | .\gradlew.bat :app:testDevDebugUnitTest --tests "*Reset*" --tests "*Account*" | Failed final upload blocks destructive wipe by default |
| Release hardening | Compile and core integration seam | .\gradlew.bat :app:compileDevDebugKotlin | No DI/signature breakages after continuity integration |
| Migration safety | Continuity table migration path | Room migration test for Phase 10 tables | Existing local data remains intact with new continuity schema |

Environment caveat:
- Connected Android instrumentation remains environment-gated where adb is unavailable, so phase gate should keep mandatory unit/compile checks plus conditional connected tests.

## Validation Architecture

Phase 10 validation architecture should use a two-tier feedback loop:

- Task-level quick checks on every task completion:
  - targeted unit tests for continuity payload mapping, upload policy, restore selection, reset gate behavior
  - compile guard to catch DI/signature regressions early

- Wave-level full checks:
  - aggregate unit suite for continuity + account/settings integration
  - migration verification for continuity schema additions
  - privacy payload contract assertions (no raw event fields)

Sampling policy recommendation:
- After each task: run targeted tests under 60s when possible.
- After each wave: run broader suite and compile.
- Before verify-work: all mandatory checks green and high-severity issues at zero.

## Sources

Primary sources:

- .planning/PROJECT.md, .planning/ROADMAP.md, .planning/REQUIREMENTS.md, .planning/STATE.md
- .planning/phases/10-cloud-continuity-snapshot-and-milestone-hardening/10-CONTEXT.md
- .planning/phases/09-prediction-what-if-and-athlete-guidance/09-CONTEXT.md
- .planning/phases/08-causal-insight-and-personalization-core/08-CONTEXT.md
- app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt
- app/src/main/java/com/aira/health/di/NetworkModule.kt
- app/src/main/java/com/aira/health/presentation/settings/SettingsScreen.kt
- app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt
- app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt
- app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt
- app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt

External references:

- https://developer.android.com/topic/libraries/architecture/workmanager
- https://developer.android.com/training/data-storage/room/migrating-db-versions

## Plan Integration Notes

- 10-01 should define continuity schema/contracts, upload/read restore repositories/use-cases, and periodic backstop orchestration integration.
- 10-02 should implement reset safety path and strict hardening verification artifacts (privacy/security/test gates) with no unresolved high-severity findings.
