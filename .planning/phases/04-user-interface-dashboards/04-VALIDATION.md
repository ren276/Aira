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

| Property               | Value                                                                       |
| ---------------------- | --------------------------------------------------------------------------- |
| **Framework**          | JUnit 5 + MockK + kotlinx-coroutines-test + Compose androidTest             |
| **Config file**        | app/build.gradle.kts                                                        |
| **Quick run command**  | `./gradlew.bat :app:testDevDebugUnitTest`                                   |
| **Full suite command** | `./gradlew.bat :app:testDevDebugUnitTest :app:connectedDevDebugAndroidTest` |
| **Estimated runtime**  | unit 60-180s; instrumentation depends on emulator/device                    |

---

## Sampling Rate

- **After every task commit:** run task-level `<automated>` command with fast unit or compile focus.
- **Per-task rule:** do not block each task on emulator instrumentation; reserve androidTest execution for wave gates.
- **After each wave gate:** run the wave's instrumentation suite once, plus changed unit suites.
- **Before `/gsd-verify-work`:** run `./gradlew.bat :app:testDevDebugUnitTest` and all phase instrumentation suites.
- **Max feedback latency target:** < 180 seconds for task-level loops.

---

## Per-Task Verification Map

| Task ID  | Plan  | Wave | Requirement  | Threat Ref      | Secure Behavior                                                           | Test Type           | Automated Command                                                                                                                                                                                | File Exists | Status  |
| -------- | ----- | ---- | ------------ | --------------- | ------------------------------------------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------- | ------- |
| 04-01-01 | 04-01 | 1    | UI-01        | T-04-01         | Theme token constraints enforce light and OLED rules                      | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.theme.AiraThemeTokensTest"`                                                                                       | planned     | pending |
| 04-01-02 | 04-01 | 1    | UI-01, UI-04 | T-04-02         | Score and vitals atoms clamp and render bounded values                    | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.theme.AiraThemeTokensTest" :app:compileDevDebugKotlin`                                                            | planned     | pending |
| 04-04-01 | 04-04 | 2    | UI-05, UI-06 | T-04-10         | Workout DAO CRUD behavior is complete and bounded                         | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.local.dao.WorkoutSessionDaoTest"`                                                                                         | planned     | pending |
| 04-04-02 | 04-04 | 2    | UI-06        | T-04-10         | Nutrition DAO mutation behavior is complete and bounded                   | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.local.dao.NutritionLogDaoTest"`                                                                                           | planned     | pending |
| 04-04-03 | 04-04 | 2    | UI-05, UI-06 | T-04-11         | DAO mutation invariants remain stable for downstream plans                | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.local.dao.WorkoutSessionDaoTest" --tests "com.aira.health.data.local.dao.NutritionLogDaoTest" :app:compileDevDebugKotlin` | planned     | pending |
| 04-07-01 | 04-07 | 2    | UI-01        | T-04-19         | Deep-link routing is deterministic and safe-fallback                      | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.navigation.DeepLinkRouterTest"`                                                                                   | planned     | pending |
| 04-07-02 | 04-07 | 2    | UI-01        | T-04-20         | Root nav shell compiles with fixed tab ordering contract                  | androidTest-compile | `./gradlew.bat :app:compileDevDebugAndroidTestKotlin`                                                                                                                                            | planned     | pending |
| 04-02-01 | 04-02 | 3    | UI-02, UI-04 | T-04-04/T-04-05 | Cached-first state and refresh behavior are deterministic                 | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.home.HomeViewModelTest"`                                                                                | planned     | pending |
| 04-02-02 | 04-02 | 3    | UI-02        | T-04-04/T-04-06 | Fixed order and anomaly visibility contract remains intact                | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.home.HomeViewModelTest" :app:compileDevDebugKotlin`                                                     | planned     | pending |
| 04-02-03 | 04-02 | 3    | UI-02        | T-04-06         | Home dashboard androidTest sources compile for wave gate execution        | androidTest-compile | `./gradlew.bat :app:compileDevDebugAndroidTestKotlin`                                                                                                                                            | planned     | pending |
| 04-08-01 | 04-08 | 3    | UI-05, UI-06 | T-04-21         | Repository layer maps safely onto DAO contracts                           | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.local.dao.WorkoutSessionDaoTest" --tests "com.aira.health.data.local.dao.NutritionLogDaoTest" :app:compileDevDebugKotlin` | planned     | pending |
| 04-08-02 | 04-08 | 3    | UI-05, UI-06 | T-04-12         | Hilt bindings and scanner dependency baseline compile safely              | compile             | `./gradlew.bat :app:compileDevDebugKotlin`                                                                                                                                                       | planned     | pending |
| 04-03-01 | 04-03 | 4    | UI-03        | T-04-07/T-04-08 | Detail route and explanation contract are deterministic                   | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.details.MetricDetailViewModelTest"`                                                                     | planned     | pending |
| 04-03-02 | 04-03 | 4    | UI-03        | T-04-07/T-04-08 | Route-state and bottom-sheet invariants compile and hold                  | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.details.MetricDetailViewModelTest" :app:compileDevDebugKotlin`                                          | planned     | pending |
| 04-05-01 | 04-05 | 4    | UI-05        | T-04-13         | Train quick-add/deep-edit/history state transitions are stable            | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.train.TrainViewModelTest"`                                                                                        | planned     | pending |
| 04-05-02 | 04-05 | 4    | UI-05        | T-04-14/T-04-15 | Train composables compile with destructive confirmation behavior          | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.train.TrainViewModelTest" :app:compileDevDebugKotlin`                                                             | planned     | pending |
| 04-05-03 | 04-05 | 4    | UI-05        | T-04-14         | Train androidTest sources compile for wave gate execution                 | androidTest-compile | `./gradlew.bat :app:compileDevDebugAndroidTestKotlin`                                                                                                                                            | planned     | pending |
| 04-06-01 | 04-06 | 4    | UI-06        | T-04-17         | Nutrition quick-add/manual/scanner state behavior is stable               | unit                | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.nutrition.NutritionViewModelTest"`                                                                                | planned     | pending |
| 04-06-02 | 04-06 | 4    | UI-06        | T-04-16/T-04-17 | Nutrition screens and scanner gateway compile with safe draft mapping     | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.nutrition.NutritionViewModelTest" :app:compileDevDebugKotlin`                                                     | planned     | pending |
| 04-06-03 | 04-06 | 4    | UI-06        | T-04-18         | Nutrition androidTest sources compile for wave gate execution             | androidTest-compile | `./gradlew.bat :app:compileDevDebugAndroidTestKotlin`                                                                                                                                            | planned     | pending |
| 04-09-01 | 04-09 | 5    | UI-03        | T-04-22         | Shared detail primitives compile and preserve confidence-visible behavior | unit+compile        | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.presentation.dashboard.details.MetricDetailViewModelTest" :app:compileDevDebugKotlin`                                          | planned     | pending |
| 04-09-02 | 04-09 | 5    | UI-03        | T-04-22/T-04-23 | Four full-depth metric detail screens compile against shared contracts    | compile             | `./gradlew.bat :app:compileDevDebugKotlin`                                                                                                                                                       | planned     | pending |
| 04-09-03 | 04-09 | 5    | UI-03        | T-04-24         | Detail androidTest sources compile for wave gate execution                | androidTest-compile | `./gradlew.bat :app:compileDevDebugAndroidTestKotlin`                                                                                                                                            | planned     | pending |

_Status: pending execution in this phase._

---

## Wave-Gate Instrumentation Suites

| Wave Gate    | Suites                                     | Command                                                                                                                                                                         |
| ------------ | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Wave 2 close | App shell navigation                       | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.navigation.AppNavHostTest"`                                                              |
| Wave 3 close | Home dashboard interactions                | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.dashboard.home.HomeDashboardTest"`                                                       |
| Wave 4 close | Train and Nutrition flows                  | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.train.TrainFlowTest" --tests "com.aira.health.presentation.nutrition.NutritionFlowTest"` |
| Wave 5 close | Metric detail route and sheet interactions | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "com.aira.health.presentation.dashboard.details.MetricDetailSheetTest"`                                                |

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
