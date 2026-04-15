---
phase: 03
slug: scoring-engines-logic
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-15
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4/5 + MockK (Kotlin) |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest --tests "com.aira.domain.engine.*"` |
| **Full suite command** | `./gradlew testDebugUnitTest` |
| **Estimated runtime** | ~10 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest --tests "{TargetClass}Test"`
- **After every plan wave:** Run `./gradlew testDebugUnitTest --tests "com.aira.domain.engine.*"`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | SCORE-01 | — | N/A | unit | `./gradlew testDebugUnitTest --tests "*RecoveryEngineTest*"` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | SCORE-02 | — | N/A | unit | `./gradlew testDebugUnitTest --tests "*SleepEngineTest*"` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | SCORE-04 | — | N/A | unit | `./gradlew testDebugUnitTest --tests "*StrainEngineTest*"` | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 2 | SCORE-03 | — | N/A | unit | `./gradlew testDebugUnitTest --tests "*StressEngineTest*"` | ❌ W0 | ⬜ pending |
| 03-03-01 | 03 | 3 | SCORE-05 | — | N/A | unit | `./gradlew testDebugUnitTest --tests "*EmaEngineTest*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/aira/domain/engine/RecoveryEngineTest.kt` — stubs for SCORE-01
- [ ] `app/src/test/java/com/aira/domain/engine/SleepEngineTest.kt` — stubs for SCORE-02
- [ ] `app/src/test/java/com/aira/domain/engine/StrainEngineTest.kt` — stubs for SCORE-04
- [ ] `app/src/test/java/com/aira/domain/engine/StressEngineTest.kt` — stubs for SCORE-03
- [ ] `app/src/test/java/com/aira/domain/engine/EmaEngineTest.kt` — stubs for SCORE-05

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Data sync integration | SCORE-01-05 | Verifying end-to-end SQLite saves across SDK | Monitor Room inspector for DailyMetrics logic accuracy post-calculation |

*If none: "All phase behaviors have automated verification."*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
