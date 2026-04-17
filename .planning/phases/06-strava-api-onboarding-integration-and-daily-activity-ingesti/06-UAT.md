---
status: diagnosed
phase: 06-strava-api-onboarding-integration-and-daily-activity-ingesti
source:
  - 06-01-SUMMARY.md
started: 2026-04-17T12:40:05.2269748Z
updated: 2026-04-17T13:08:04.6133340Z
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
result: issue
reported: "where to disconnect it"
severity: major

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
passed: 6
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "Disconnecting Strava resets connection state and app shows reconnect-required state."
  status: failed
  reason: "User reported: where to disconnect it"
  severity: major
  test: 4
  root_cause: "Strava disconnect is implemented in data/domain layers but no UI surface invokes it, so users cannot find or trigger disconnect."
  artifacts:
    - path: "app/src/main/java/com/aira/health/domain/repository/StravaRepository.kt"
      issue: "Disconnect contract exists but is not consumed by presentation actions."
    - path: "app/src/main/java/com/aira/health/data/repository/StravaRepositoryImpl.kt"
      issue: "Disconnect behavior exists but has no call path from UI."
    - path: "app/src/main/java/com/aira/health/presentation/settings/SettingsScreen.kt"
      issue: "No Strava management/disconnect control is exposed."
    - path: "app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt"
      issue: "Account screen has sign out only, no Strava disconnect action."
  missing:
    - "Add a discoverable Strava disconnect control in settings/account surface."
    - "Wire presentation action to StravaRepository.disconnect()."
    - "After disconnect, surface reconnect-required state in onboarding flow."
  debug_session: ".planning/debug/p06-t4-disconnect-strava.md"
