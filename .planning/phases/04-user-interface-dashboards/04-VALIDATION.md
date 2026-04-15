---
phase: 04
slug: user-interface-dashboards
status: planned
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-15
updated: 2026-04-15
---

# Phase 04 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + MockK + kotlinx-coroutines-test + Compose androidTest |
| **Config file** | app/build.gradle.kts |
| **Quick run command** | `./gradlew.bat :app:testDevDebugUnitTest` |
| **Full suite command** | `./gradlew.bat :app:testDevDebugUnitTest :app:connectedDevDebugAndroidTest` |
| **Estimated runtime** | unit 60-180s; instrumentation depends on emulator/device |

---

## Sampling Rate

- **After every task commit:** run task-level `<automated>` command.
- **After each wave:** run all unit tests for changed plans in that wave.
- **Before `/gsd-verify-work`:** run `./gradlew.bat :app:testDevDebugUnitTest` and relevant instrumentation suites.
- **Max feedback latency target:** < 180 seconds for unit loops.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 04-01 | 1 | UI-01 | T-04-01/T-04-03 | Theme token constraints enforce light+OLED rules | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.theme.AiraThemeTokensTest"` | planned | pending |
| 04-01-02 | 04-01 | 1 | UI-01, UI-04 | T-04-02/T-04-03 | Score/vitals atoms clamp and render bounded values | unit+compile | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.theme.AiraThemeTokensTest" :app:compileDevDebugKotlin` | planned | pending |
| 04-01-03 | 04-01 | 1 | UI-01 | T-04-01 | Root nav/deep link routing is deterministic and safe-fallback | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.navigation.DeepLinkRouterTest"` | planned | pending |
| 04-02-01 | 04-02 | 2 | UI-02, UI-04 | T-04-04/T-04-05 | Cached-first state and refresh behavior are deterministic | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.home.HomeViewModelTest"` | planned | pending |
| 04-02-02 | 04-02 | 2 | UI-02 | T-04-04/T-04-06 | Fixed order + anomaly visibility contract remains intact | unit+compile | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.home.HomeViewModelTest" :app:compileDevDebugKotlin` | planned | pending |
| 04-02-03 | 04-02 | 2 | UI-02 | T-04-06 | Dashboard interaction contract enforced via Compose tests | instrumentation | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.dashboard.home.HomeDashboardTest"` | planned | pending |
| 04-04-01 | 04-04 | 2 | UI-05, UI-06 | T-04-10 | Workout CRUD DAO behavior is complete and bounded | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.local.dao.WorkoutSessionDaoTest"` | planned | pending |
| 04-04-02 | 04-04 | 2 | UI-06 | T-04-10 | Nutrition mutation DAO behavior is complete and bounded | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.local.dao.NutritionLogDaoTest"` | planned | pending |
| 04-04-03 | 04-04 | 2 | UI-05, UI-06 | T-04-11/T-04-12 | Repository wiring + scanner dependency baseline compile safely | unit+compile | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.local.dao.WorkoutSessionDaoTest" --tests "com.aira.health.data.local.dao.NutritionLogDaoTest" :app:compileDevDebugKotlin` | planned | pending |
| 04-03-01 | 04-03 | 3 | UI-03 | T-04-07/T-04-08 | Detail route and explanation contract are deterministic | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.details.MetricDetailViewModelTest"` | planned | pending |
| 04-03-02 | 04-03 | 3 | UI-03 | T-04-08 | Metric detail screens compile against full-depth state model | unit+compile | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.details.MetricDetailViewModelTest" :app:compileDevDebugKotlin` | planned | pending |
| 04-03-03 | 04-03 | 3 | UI-03 | T-04-09 | Bottom-sheet and route behavior hold under UI interaction | instrumentation | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.dashboard.details.MetricDetailSheetTest"` | planned | pending |
| 04-05-01 | 04-05 | 3 | UI-05 | T-04-13 | Train quick-add/deep-edit/history state transitions are stable | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.train.TrainViewModelTest"` | planned | pending |
| 04-05-02 | 04-05 | 3 | UI-05 | T-04-14/T-04-15 | Train composables compile with destructive confirmation behavior | unit+compile | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.train.TrainViewModelTest" :app:compileDevDebugKotlin` | planned | pending |
| 04-05-03 | 04-05 | 3 | UI-05 | T-04-14 | Train full flow validated by instrumentation | instrumentation | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.train.TrainFlowTest"` | planned | pending |
| 04-06-01 | 04-06 | 3 | UI-06 | T-04-17 | Nutrition quick-add/manual/scanner state behavior is stable | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.nutrition.NutritionViewModelTest"` | planned | pending |
| 04-06-02 | 04-06 | 3 | UI-06 | T-04-16/T-04-17 | Nutrition screens + scanner gateway compile with safe draft mapping | unit+compile | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.nutrition.NutritionViewModelTest" :app:compileDevDebugKotlin` | planned | pending |
| 04-06-03 | 04-06 | 3 | UI-06 | T-04-18 | Nutrition scanner/manual/edit/delete flow verified in UI tests | instrumentation | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.nutrition.NutritionFlowTest"` | planned | pending |

_Status: pending execution in this phase._

---

## Wave 0 Requirements

- [x] All plan tasks include explicit `<automated>` commands.
- [x] No task is blocked by missing validation artifact.
- [x] Unit and instrumentation checks are distributed across all behavior-heavy flows.

---

## Manual-Only Verifications

- Instrumentation runs require emulator/device + `adb` availability in execution environment.
- If device matrix is unavailable, run unit suite first and defer androidTest to next available environment checkpoint.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify commands
- [x] Sampling continuity maintained across waves
- [x] Validation artifact exists for Nyquist gate
- [x] No watch-mode commands
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ready for Nyquist gate (2026-04-15)
