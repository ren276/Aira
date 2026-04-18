---
phase: 10
slug: cloud-continuity-snapshot-and-milestone-hardening
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-18
---

# Phase 10 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Android Gradle test tasks |
| **Config file** | app/build.gradle.kts |
| **Quick run command** | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Continuity*" --tests "*Snapshot*"` |
| **Full suite command** | `.\gradlew.bat :app:testDevDebugUnitTest; .\gradlew.bat :app:compileDevDebugKotlin` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Continuity*" --tests "*Snapshot*"`
- **After every plan wave:** Run `.\gradlew.bat :app:testDevDebugUnitTest; .\gradlew.bat :app:compileDevDebugKotlin`
- **Before /gsd-verify-work:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | BACK-01 | T-10-01 | Snapshot payload excludes raw biometric rows | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Continuity*" --tests "*Snapshot*"` | ❌ W0 | pending |
| 10-01-02 | 01 | 1 | BACK-01 | T-10-02 | Sync retries/backoff do not duplicate destructive writes | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*ContinuitySync*"` | ❌ W0 | pending |
| 10-01-03 | 01 | 1 | BACK-01 | T-10-03 | Restore path applies selected snapshot deterministically | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Restore*"` | ❌ W0 | pending |
| 10-02-01 | 02 | 2 | BACK-02 | T-10-04 | Reset blocks wipe when final upload fails by default | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Reset*" --tests "*Account*"` | ❌ W0 | pending |
| 10-02-02 | 02 | 2 | BACK-02 | T-10-05 | Irreversible override requires explicit confirmation state | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Reset*"` | ❌ W0 | pending |
| 10-02-03 | 02 | 2 | BACK-01, BACK-02 | T-10-06 | Hardening gate reports no unresolved high-severity threats | integration | `.\gradlew.bat :app:testDevDebugUnitTest; .\gradlew.bat :app:compileDevDebugKotlin` | ✅ | pending |

*Status: pending, green, red, flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/aira/health/domain/usecase/UploadContinuitySnapshotUseCaseTest.kt` - upload payload and retry policy coverage
- [ ] `app/src/test/java/com/aira/health/domain/usecase/RestoreContinuitySnapshotUseCaseTest.kt` - restore selection and apply behavior
- [ ] `app/src/test/java/com/aira/health/presentation/supplementary/AccountResetFlowViewModelTest.kt` - reset blocking and irreversible override checks
- [ ] `app/src/androidTest/java/com/aira/health/data/local/db/AiraDatabaseMigrationTest.kt` - Phase 10 migration path verification extension

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Restore prompt clarity and destructive reset warning copy | BACK-02 | UX language quality cannot be fully asserted by unit tests | Launch app, navigate to settings/account reset flow, verify warning and decision copy are explicit and irreversible action is clearly separated |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all missing references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
