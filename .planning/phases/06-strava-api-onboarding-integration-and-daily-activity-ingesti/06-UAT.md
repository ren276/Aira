---
status: complete
phase: 06-strava-api-onboarding-integration-and-daily-activity-ingesti
source:
  - 06-01-SUMMARY.md
started: 2026-04-17T12:40:05.2269748Z
updated: 2026-04-17T14:05:00.0000000Z
---

## Current Test

[testing complete]

## Tests

### 1. Mandatory Strava onboarding gate

expected: After sign-in, Strava connection appears as a required step before permissions/main shell.
result: pass

### 2. Successful Strava connect callback

expected: Tapping Connect opens Strava auth, returning to the app marks Strava as connected without crashing.
result: pass

### 3. Missing scope is blocked safely

expected: If Strava grant is incomplete, the app blocks completion and shows a clear reconnect/retry path.
result: pass

### 4. Disconnect clears local Strava session

expected: Disconnecting Strava resets connection state and the app shows reconnect-required state.
result: pass

### 5. Initial backfill sync populates activity data

expected: After connect, historical activities sync in and relevant UI data (workouts/steps/distance/calories) reflects imported activity.
result: pass

### 6. Incremental sync adds only new activities

expected: A subsequent sync imports new activities only and does not create obvious duplicates.
result: pass

### 7. Duplicate suppression across sources

expected: Overlapping workouts from Strava and non-Strava sources do not appear as duplicate entries in user-facing history.
result: pass

## Summary

total: 7
passed: 7
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "Disconnecting Strava resets connection state and app shows reconnect-required state."
  status: resolved
  reason: "Disconnect control added to Account, wired to repository disconnect, and reconnect-required routing precedence enforced."
  severity: none
  test: 4
  root_cause: "Previously missing UI invocation path is now closed via AccountViewModel and AccountScreen action wiring."
  artifacts:
  - path: "app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt"
    issue: "Resolved: Account UI now shows Strava status and Disconnect Strava action with error/progress state."
  - path: "app/src/main/java/com/aira/health/data/strava/StravaConnectionStore.kt"
    issue: "Resolved: disconnect now marks reconnectRequired=true while clearing connection metadata."
  - path: "app/src/main/java/com/aira/health/presentation/navigation/AppEntryRoute.kt"
    issue: "Resolved: reconnect-required route is prioritized before onboardingCompleted shortcut."
  - path: "app/src/test/java/com/aira/health/presentation/supplementary/AccountViewModelTest.kt"
    issue: "Added: verifies disconnect success/failure UI state and repository invocation."
  - path: "app/src/test/java/com/aira/health/presentation/navigation/AppEntryRouteTest.kt"
    issue: "Added: verifies reconnect-required destination precedence."
  - path: "app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt"
    issue: "Covered by instrumentation test for disconnect control discoverability and click action."
    missing: []
    verification:
  - "./gradlew.bat :app:testDevDebugUnitTest --tests \"com.aira.health.presentation.supplementary.AccountViewModelTest\""
  - "./gradlew.bat :app:testDevDebugUnitTest --tests \"com.aira.health.presentation.navigation.AppEntryRouteTest\""
  - "./gradlew.bat :app:connectedDevDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.supplementary.AccountScreenTest'"
    debug_session: ".planning/debug/p06-t4-disconnect-strava.md"
