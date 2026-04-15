---
phase: 03
slug: scoring-engines-logic
status: verified
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-15
updated: 2026-04-15
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4/5 + MockK (Kotlin) |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.*"` |
| **Full suite command** | `./gradlew.bat :app:testDevDebugUnitTest` |
| **Audited run** | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.RecoveryEngineTest" --tests "com.aira.health.domain.engine.SleepEngineTest" --tests "com.aira.health.domain.engine.StrainEngineTest" --tests "com.aira.health.domain.engine.StressEngineTest" --tests "com.aira.health.domain.engine.EnergyBankEngineTest" --tests "com.aira.health.domain.engine.EmaEngineTest" --tests "com.aira.health.domain.usecase.BaselineRecalculatorUseCaseTest" --tests "com.aira.health.domain.usecase.ComputeDailyScoresUseCaseTest" --tests "com.aira.health.data.worker.HealthSyncWorkerScheduleTest" --no-daemon` |
| **Result** | BUILD SUCCESSFUL (2026-04-15) |

---

## Sampling Rate

- **After every task commit:** Run the target class test command from the task verify block.
- **After every plan wave:** Run all touched class tests for that wave.
- **Before `/gsd-verify-work`:** Run full `:app:testDevDebugUnitTest`.
- **Max feedback latency target:** < 15 seconds per focused class run.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | SCORE-01 | T-03-01 | Clamp + deterministic weighted recovery with sparse-input support | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.RecoveryEngineTest"` | ✅ | ✅ green |
| 03-01-02 | 01 | 1 | SCORE-02 | T-03-02/T-03-03 | Weighted sleep scoring, confidence as non-gating parallel signal | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.SleepEngineTest"` | ✅ | ✅ green |
| 03-02-01 | 02 | 2 | SCORE-04 (D-01) | T-03-04 | Non-linear zone scaling bounded to 0..100 | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.StrainEngineTest"` | ✅ | ✅ green |
| 03-02-02 | 02 | 2 | SCORE-03 (D-01) | T-03-04/T-03-06 | Hourly + daily stress with spike amplification and sparse-hour confidence downgrade | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.StressEngineTest"` | ✅ | ✅ green |
| 03-02-03 | 02 | 2 | D-05, D-06 | T-03-05 | Energy Bank visible/internal separation and deterministic carry-over | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.EnergyBankEngineTest"` | ✅ | ✅ green |
| 03-03-01 | 03 | 3 | SCORE-05, D-02, D-07, D-08 | T-03-07 | Sequential EMA cold-start and replay-safe baseline recomputation | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.engine.EmaEngineTest" --tests "com.aira.health.domain.usecase.BaselineRecalculatorUseCaseTest"` | ✅ | ✅ green |
| 03-03-02 | 03 | 3 | D-03, D-04, D-09, D-10, D-11, D-12 | T-03-08 | Full DailyMetrics persistence with null-safe confidence-aware scoring | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.domain.usecase.ComputeDailyScoresUseCaseTest"` | ✅ | ✅ green |
| 03-03-03 | 03 | 3 | D-02 (pipeline path) | T-03-09 | Deterministic worker sequencing and retry behavior | unit | `./gradlew.bat :app:testDevDebugUnitTest --tests "com.aira.health.data.worker.HealthSyncWorkerScheduleTest"` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements.

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Audit 2026-04-15

| Metric | Count |
|--------|-------|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or existing coverage
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all previously missing references
- [x] No watch-mode flags
- [x] Feedback latency target remains practical for focused runs
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** verified 2026-04-15
