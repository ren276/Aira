---
status: diagnosed
trigger: "UAT gap test #4: user reported they cannot find where to disconnect Strava; expected disconnect should reset connection state and show reconnect-required state"
created: 2026-04-17T18:40:39.1861234+05:30
updated: 2026-04-17T18:42:30.3614953+05:30
---

## Current Focus

hypothesis: Confirmed - disconnect capability exists only in repository layer and is unreachable from user-facing navigation/settings/account flows.
test: Completed call-site and UI affordance searches plus onboarding flow inspection.
expecting: N/A (hypothesis confirmed by observed zero call sites and connect-only presentation paths).
next_action: Return structured root-cause diagnosis for UAT gap and reference this debug session.

## Symptoms

expected: Disconnecting Strava resets connection state and app shows reconnect-required state.
actual: User cannot find where to disconnect Strava.
errors: No crash/error string reported; usability failure (missing discoverable disconnect entry point).
reproduction: Open app after connecting Strava, go to expected settings/account areas, try to find disconnect path.
started: Observed during UAT Phase 06 test 4.

## Eliminated

## Evidence

- timestamp: 2026-04-17T18:41:20.8030849+05:30
  checked: .planning/phases/06-strava-api-onboarding-integration-and-daily-activity-ingesti/06-UAT.md
  found: Test 4 is marked major issue with report "where to disconnect it" while expected behavior explicitly requires disconnect and reconnect-required state.
  implication: Gap is likely discoverability/wiring, not sync correctness.

- timestamp: 2026-04-17T18:41:20.8030849+05:30
  checked: app/src/main/java/com/aira/health/domain/repository/StravaRepository.kt and app/src/main/java/com/aira/health/data/repository/StravaRepositoryImpl.kt
  found: StravaRepository defines disconnect(), and StravaRepositoryImpl.disconnect() clears token store and marks connection disconnected.
  implication: Backend/domain capability exists to clear local Strava session.

- timestamp: 2026-04-17T18:41:20.8030849+05:30
  checked: app/src/main/java/com/aira/health/presentation/settings/SettingsScreen.kt and app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt
  found: No Strava section, no disconnect button/action, and AccountScreen only exposes profile info plus sign out.
  implication: Settings and Account surfaces do not provide a visible Strava disconnect entry point.

- timestamp: 2026-04-17T18:41:20.8030849+05:30
  checked: app/src/main/java/com/aira/health/presentation/navigation/AiraNavHost.kt and app/src/main/java/com/aira/health/presentation/navigation/AppEntryViewModel.kt
  found: Navigation includes settings/account routes but no dedicated Strava management route; AppEntryViewModel has startStravaConnection/handleStravaAuthCallback but no disconnect action.
  implication: Current presentation flow supports connect/auth callback but not user-initiated disconnect.

- timestamp: 2026-04-17T18:42:30.3614953+05:30
  checked: Codebase search for "disconnect(" and Strava references across app/src/main/java
  found: Only two disconnect declarations exist (StravaRepository interface and StravaRepositoryImpl override) with zero usage call sites.
  implication: Disconnect cannot be triggered anywhere in app behavior.

- timestamp: 2026-04-17T18:42:30.3614953+05:30
  checked: app/src/main/java/com/aira/health/presentation/navigation/AppEntryRoute.kt and app/src/main/java/com/aira/health/presentation/onboarding/StravaConnectScreen.kt
  found: Onboarding route wires only onConnectStrava, and Strava screen UI presents only "Connect Strava"/"Reconnect Strava" actions.
  implication: UX exposes connect/reconnect only; missing disconnect entry point explains user report verbatim.

- timestamp: 2026-04-17T18:42:30.3614953+05:30
  checked: .planning/debug/knowledge-base.md existence
  found: No knowledge base file present.
  implication: No prior reusable debug pattern entry available for this issue.

## Resolution

root_cause: Strava disconnect is implemented only at repository level but never surfaced or invoked by any presentation route/viewmodel/settings/account UI, so users have no discoverable path to trigger disconnect.
fix: Expose Strava disconnect action in reachable settings/account management flow and wire to StravaRepository.disconnect(), then surface post-disconnect reconnect-required UX state.
verification: Root-cause-only mode; no code fix applied.
files_changed: - .planning/debug/p06-t4-disconnect-strava.md
