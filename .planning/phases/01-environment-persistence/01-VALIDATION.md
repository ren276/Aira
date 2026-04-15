---
phase: 01
slug: environment-persistence
status: partial
nyquist_compliant: false
wave_0_complete: true
created: 2026-04-15
---

# Phase 01 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property               | Value                                                               |
| ---------------------- | ------------------------------------------------------------------- |
| **Framework**          | JUnit 5 + MockK + kotlinx-coroutines-test                           |
| **Config file**        | none (configured via Gradle dependencies in `app/build.gradle.kts`) |
| **Quick run command**  | `./gradlew :app:testDevDebugUnitTest`                               |
| **Full suite command** | `./gradlew :app:test`                                               |
| **Estimated runtime**  | ~60-180 seconds                                                     |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDevDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID  | Plan  | Wave | Requirement | Threat Ref | Secure Behavior                                                          | Test Type  | Automated Command                                                       | File Exists | Status     |
| -------- | ----- | ---- | ----------- | ---------- | ------------------------------------------------------------------------ | ---------- | ----------------------------------------------------------------------- | ----------- | ---------- |
| 01-01-01 | 01-01 | 1    | ENV-01      | T-01-01    | Build scaffold compiles with target SDK/min SDK and secure flavor config | build/unit | `./gradlew :app:testDevDebugUnitTest`                                   | ✅          | ✅ green   |
| 01-01-02 | 01-01 | 1    | ENV-04      | T-01-01    | `FLAG_SECURE` can be enabled by build config on sensitive builds         | manual     | N/A                                                                     | ⚠️ manual   | ⬜ pending |
| 01-02-01 | 01-02 | 2    | DB-01       | T-01-02    | Room schema and entities remain constructible and typed                  | manual     | N/A                                                                     | ⚠️ manual   | ⬜ pending |
| 01-02-02 | 01-02 | 2    | DB-02       | T-01-02    | SQLCipher + Keystore passphrase path protects DB at rest                 | manual     | N/A                                                                     | ⚠️ manual   | ⬜ pending |
| 01-03-01 | 01-03 | 3    | AUTH-01     | T-01-04    | Guest auth path returns local session without cloud requirement          | unit       | `./gradlew :app:testDevDebugUnitTest --tests "*UserRepositoryImplTest"` | ✅          | ✅ green   |
| 01-03-02 | 01-03 | 3    | ENV-02      | T-01-03    | Permission batch flow enforces Core -> Body -> Advanced progression      | manual     | N/A                                                                     | ⚠️ manual   | ⬜ pending |
| 01-03-03 | 01-03 | 3    | ENV-03      | T-01-03    | Health Connect install prompt path works on unsupported devices          | manual     | N/A                                                                     | ⚠️ manual   | ⬜ pending |
| 01-03-04 | 01-03 | 3    | SYNC-01     | T-01-04    | Secure sync channel controls (TLS pinning) are enforced                  | manual     | N/A                                                                     | ⚠️ manual   | ⬜ pending |

_Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky_

---

## Wave 0 Requirements

- [x] `app/src/test/java/com/aira/health/data/repository/UserRepositoryImplTest.kt` - guest auth automated baseline
- [x] `app/src/test/java/com/aira/health/domain/model/AuthStateTest.kt` - domain auth state baseline
- [ ] Add Android/JVM integration coverage for SQLCipher bootstrap path (`AiraDatabase` + `DatabaseModule`)
- [ ] Add instrumentation coverage for permission flow and Health Connect install prompt behavior

---

## Manual-Only Verifications

| Behavior                                              | Requirement | Why Manual                                                                          | Test Instructions                                                                                |
| ----------------------------------------------------- | ----------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Core/Body/Advanced onboarding permission progression  | ENV-02      | UI + Health Connect runtime interactions are device and permission-dialog dependent | Launch app on device/emulator with Health Connect and verify batch sequence plus denial handling |
| Health Connect install prompt for Android 10-13 paths | ENV-03      | Depends on Play Store availability and OS version matrix                            | Test on API levels lacking Health Connect and confirm install guidance appears                   |
| SQLCipher bootstrap through Keystore on app start     | DB-02       | Android Keystore and SQLCipher integration requires runtime environment             | Cold-start app, verify DB initializes and data remains readable across relaunch                  |
| FLAG_SECURE sensitive-screen policy                   | ENV-04      | Window flag behavior is runtime/display stack specific                              | Validate screenshot/screen-record behavior differs by flavor as expected                         |
| Transport hardening / certificate pinning evidence    | SYNC-01     | Pinning implementation not yet explicit in phase artifacts                          | Verify network client pinning configuration or add implementation tests before marking compliant |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending - partial coverage with manual-only items retained

---

## Validation Audit 2026-04-15

| Metric                  | Count |
| ----------------------- | ----- |
| Gaps found              | 8     |
| Resolved (automated)    | 2     |
| Escalated (manual-only) | 6     |
