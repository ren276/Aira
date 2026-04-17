---
phase: 06-strava-api-onboarding-integration-and-daily-activity-ingesti
plan: 02
subsystem: ui
tags: [strava, onboarding, navigation, compose, uat-gap-closure]
requires:
  - phase: 06-01
    provides: Strava connect/onboarding and sync lifecycle foundation.
provides:
  - Discoverable Account-level Strava disconnect action with loading/error feedback.
  - Reconnect-required state persistence on disconnect.
  - App-entry route precedence for reconnect-required onboarding.
  - Automated unit and instrumentation coverage for the gap closure.
affects: [phase-06, onboarding-routing, account-settings, strava-lifecycle]
tech-stack:
  added: []
  patterns:
    - Presentation actions call repository contracts via ViewModel with explicit UI action state.
    - App-entry resolver enforces reconnect-required precedence before onboarding-complete shortcut.
key-files:
  created:
    - app/src/test/java/com/aira/health/presentation/supplementary/AccountViewModelTest.kt
    - app/src/test/java/com/aira/health/presentation/navigation/AppEntryRouteTest.kt
    - app/src/androidTest/java/com/aira/health/presentation/supplementary/AccountScreenTest.kt
  modified:
    - app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt
    - app/src/main/java/com/aira/health/data/strava/StravaConnectionStore.kt
    - app/src/main/java/com/aira/health/presentation/navigation/AppEntryRoute.kt
key-decisions:
  - "Exposed disconnect in AccountScreen instead of adding a new settings sub-route to close discoverability quickly and safely."
  - "Set reconnectRequired=true on disconnect to force explicit reconnect onboarding instead of silently returning to main shell."
  - "Added a targeted Compose instrumentation test for disconnect visibility/clickability to prevent regressions of this exact UAT gap."
patterns-established:
  - "UI action state pattern: keep in-progress/error state in a dedicated MutableStateFlow and combine with repository streams."
  - "Routing safety pattern: reconnect-required condition must be evaluated before onboarding-completed when resolving entry destination."
requirements-completed: [UI-02, UI-03]
duration: 58min
completed: 2026-04-17
---

# Phase 06-02 Summary

**Strava disconnect is now discoverable from Account, invokes repository disconnect with UI feedback, and reliably routes users back to reconnect onboarding.**

## Performance

- **Duration:** 58 min
- **Started:** 2026-04-17T13:02:00Z
- **Completed:** 2026-04-17T14:00:00Z
- **Tasks:** 3
- **Files modified:** 6 (plus 3 new tests)

## Accomplishments

- Added Account-level Strava management UI with status, Disconnect Strava action, progress state, and failure message surface.
- Wired AccountViewModel disconnect flow to StravaRepository and verified success/failure behavior with targeted unit tests.
- Enforced reconnect-required route precedence at app entry and validated with route resolver tests.
- Added and executed a targeted instrumentation test proving disconnect control discoverability and click actionability.

## Task Commits

Each task was committed atomically:

1. **Task 1: Expose disconnect in Account and presentation state** - `4bd3b92` (fix/test)
2. **Task 2: Enforce reconnect-required routing precedence** - `4bd3b92` (fix/test)
3. **Task 3: Run focused compile and regression gates** - verified in-session (no additional code commit)

**Plan metadata:** `aa1c12c` (docs: add 06-02 summary)
**UAT closure metadata:** `a466601` (docs: mark test 4 gap resolved)

## Files Created/Modified

- `app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt` - Added Strava section, disconnect action, and AccountViewModel disconnect logic/state.
- `app/src/main/java/com/aira/health/data/strava/StravaConnectionStore.kt` - Updated disconnect persistence contract to mark reconnect required.
- `app/src/main/java/com/aira/health/presentation/navigation/AppEntryRoute.kt` - Prioritized reconnect-required destination resolution.
- `app/src/test/java/com/aira/health/presentation/supplementary/AccountViewModelTest.kt` - Added disconnect success/failure tests with active uiState collection.
- `app/src/test/java/com/aira/health/presentation/navigation/AppEntryRouteTest.kt` - Added route precedence coverage for reconnect-required and healthy-connected cases.
- `app/src/androidTest/java/com/aira/health/presentation/supplementary/AccountScreenTest.kt` - Added compose instrumentation discoverability/actionability test.

## Decisions Made

- Closed the UAT issue by surfacing disconnect where users already expect account-level connection controls.
- Kept error messaging user-safe and generic (no token/session internals).
- Chose explicit reconnect-required enforcement at entry-point resolver to avoid bypassing onboarding after disconnect.

## Deviations from Plan

### Auto-fixed Issues

**1. [Blocking compile] Unresolved theme color token**

- **Found during:** Task 3 verification
- **Issue:** `Theme.colors.error` token was not defined in theme contract.
- **Fix:** Switched to explicit error tint `Color(0xFFFFB4AB)` for disconnect error text.
- **Files modified:** `app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt`
- **Verification:** `:app:compileDevDebugKotlin` passed.
- **Committed in:** `4bd3b92`

**2. [Test determinism] AccountViewModel test reading inactive StateFlow**

- **Found during:** Task 3 verification
- **Issue:** `stateIn(WhileSubscribed)` was not active in test, causing stale assertions.
- **Fix:** Added background `uiState.collect { }` in tests before invoking disconnect.
- **Files modified:** `app/src/test/java/com/aira/health/presentation/supplementary/AccountViewModelTest.kt`
- **Verification:** targeted unit tests passed.
- **Committed in:** `4bd3b92`

**3. [Instrumentation setup] Compose assertion/rule stability**

- **Found during:** Task 1 verification
- **Issue:** Initial instrumentation run failed due assertion API mismatch and transient no-hierarchy test setup behavior.
- **Fix:** Switched to `assertIsDisplayed`, used Android compose rule with `ComponentActivity`, and reran targeted connected test successfully.
- **Files modified:** `app/src/androidTest/java/com/aira/health/presentation/supplementary/AccountScreenTest.kt`
- **Verification:** `:app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.supplementary.AccountScreenTest` passed.
- **Committed in:** `4bd3b92`

---

**Total deviations:** 3 auto-fixed (1 compile, 1 deterministic unit test, 1 instrumentation stability)
**Impact on plan:** All fixes were required to satisfy planned verification and did not expand scope beyond the diagnosed gap closure.

## Issues Encountered

- Gradle property argument for instrumentation test needed quoting in PowerShell for reliable parsing.
- One instrumentation run was flaky (no compose hierarchy) but passed on rerun after test adjustments and stacktrace run.

## Verification

- `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.supplementary.AccountViewModelTest"` -> PASS
- `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.navigation.AppEntryRouteTest"` -> PASS
- `./gradlew.bat :app:compileDevDebugKotlin :app:testDevDebugUnitTest --tests "com.aira.health.presentation.supplementary.AccountViewModelTest" --tests "com.aira.health.presentation.navigation.AppEntryRouteTest"` -> PASS
- `./gradlew.bat :app:connectedDevDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.supplementary.AccountScreenTest'` -> PASS

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The diagnosed UAT gap for disconnect discoverability is now covered in UI, routing, and tests.
- Phase 06 is ready for final verification sweep/closure review with updated UAT evidence.

---

_Phase: 06-strava-api-onboarding-integration-and-daily-activity-ingesti_
_Completed: 2026-04-17_
