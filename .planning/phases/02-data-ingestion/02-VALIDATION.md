---
phase: 02
slug: data-ingestion
status: partial
nyquist_compliant: false
wave_0_complete: true
created: 2026-04-15
---

# Phase 02 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property               | Value                                                                                                                                                                |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Framework**          | JUnit 5 + MockK + kotlinx-coroutines-test                                                                                                                            |
| **Config file**        | none (configured through Gradle dependencies in `app/build.gradle.kts`)                                                                                              |
| **Quick run command**  | `./gradlew :app:testDevDebugUnitTest --tests "com.aira.health.data.model.ConfidenceRouterTest" --tests "com.aira.health.domain.usecase.IngestHealthDataUseCaseTest"` |
| **Full suite command** | `./gradlew :app:test`                                                                                                                                                |
| **Estimated runtime**  | ~30-120 seconds                                                                                                                                                      |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDevDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID  | Plan  | Wave | Requirement                        | Threat Ref       | Secure Behavior                                                                 | Test Type | Automated Command                                                                                          | File Exists | Status     |
| -------- | ----- | ---- | ---------------------------------- | ---------------- | ------------------------------------------------------------------------------- | --------- | ---------------------------------------------------------------------------------------------------------- | ----------- | ---------- |
| 02-01-01 | 02-01 | 1    | DATA-03                            | T-02-01          | Package-source confidence mapping deterministic and bounded                     | unit      | `./gradlew :app:testDevDebugUnitTest --tests "com.aira.health.data.model.ConfidenceRouterTest"`            | ✅          | ✅ green   |
| 02-01-02 | 02-01 | 1    | DATA-01                            | T-02-03          | Primary HC repository shape and DI routing compile                              | manual    | N/A                                                                                                        | ⚠️ manual   | ⬜ pending |
| 02-02-01 | 02-02 | 1    | DATA-02                            | T-02-03          | Google Fit fallback path selected when HC unavailable                           | manual    | N/A                                                                                                        | ⚠️ manual   | ⬜ pending |
| 02-02-02 | 02-02 | 1    | DATA-03                            | T-02-01          | Confidence rules apply to fallback source arbitration                           | unit      | `./gradlew :app:testDevDebugUnitTest --tests "com.aira.health.data.model.ConfidenceRouterTest"`            | ✅          | ✅ green   |
| 02-03-01 | 02-03 | 2    | DATA-01, DATA-02, DATA-03, DATA-04 | T-02-01, T-02-02 | Ingestion use case resolves conflicts, persists, purges, updates sync timestamp | unit      | `./gradlew :app:testDevDebugUnitTest --tests "com.aira.health.domain.usecase.IngestHealthDataUseCaseTest"` | ✅          | ✅ green   |
| 02-03-02 | 02-03 | 2    | DATA-04                            | T-02-02, T-02-04 | Worker schedule and reboot resubscribe behavior remain robust                   | manual    | N/A                                                                                                        | ⚠️ manual   | ⬜ pending |

_Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky_

---

## Wave 0 Requirements

- [x] `app/src/test/java/com/aira/health/data/model/ConfidenceRouterTest.kt` - source confidence routing invariants
- [x] `app/src/test/java/com/aira/health/domain/usecase/IngestHealthDataUseCaseTest.kt` - conflict resolution and sync timestamp persistence
- [ ] Add instrumentation test for `HealthDataModule` runtime fallback selection on HC unavailable devices
- [ ] Add integration/instrumentation test for `BootReceiver` + `HealthSyncWorker.schedule()` restoration path

---

## Manual-Only Verifications

| Behavior                                                     | Requirement | Why Manual                                                             | Test Instructions                                                                    |
| ------------------------------------------------------------ | ----------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Health Connect SDK availability and provider-update handling | DATA-01     | Depends on device OS/provider app state and Android framework services | Test on supported/unsupported devices and verify DI routes to the correct repository |
| Google Fit fallback on Android 10-12                         | DATA-02     | Requires Google account auth and Play services runtime behavior        | Disable/uninstall HC provider, grant Fit access, verify fallback ingestion path      |
| Periodic WorkManager + boot recovery scheduling              | DATA-04     | Requires OS scheduler + reboot event simulation                        | Verify worker registration in WorkManager inspector and reboot reschedule behavior   |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending - partial (automated core covered, Android runtime paths manual-only)

---

## Validation Audit 2026-04-15

| Metric                  | Count |
| ----------------------- | ----- |
| Gaps found              | 6     |
| Resolved (automated)    | 3     |
| Escalated (manual-only) | 3     |
