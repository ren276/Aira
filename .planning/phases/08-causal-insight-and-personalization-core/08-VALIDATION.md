---
phase: 08
slug: causal-insight-and-personalization-core
status: partial
nyquist_compliant: false
wave_0_complete: true
created: 2026-04-18
---

# Phase 08 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4/5 via Gradle Android test tasks |
| **Config file** | app/build.gradle.kts |
| **Quick run command** | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Causal*" --tests "*Personalization*"` |
| **Full suite command** | `.\gradlew.bat :app:testDevDebugUnitTest :app:compileDevDebugKotlin :app:connectedDevDebugAndroidTest` |
| **Estimated runtime** | ~240 seconds |

---

## Sampling Rate

- **After every task commit:** Run `.\gradlew.bat :app:testDevDebugUnitTest --tests "*Causal*" --tests "*Personalization*"`
- **After every plan wave:** Run `.\gradlew.bat :app:testDevDebugUnitTest :app:compileDevDebugKotlin`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 300 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 08-01-01 | 01 | 1 | CAUS-01, CAUS-02 | T-08-01 | Ranking uses real recent data windows and deterministic ordering | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*CausalRanking*"` | ✅ | green |
| 08-01-02 | 01 | 1 | CAUS-03 | T-08-02 | Confidence tiers and recency metadata map correctly and safely | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*ComputeCausalInsightsUseCaseTest" --tests "*ComputeDailyScoresUseCaseTest"` | ✅ | green |
| 08-02-01 | 02 | 2 | PPM-01, PPM-02 | T-08-03 | Adaptation uses bounded EMA and 7-day minimum data gate | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*PersonalizationUpdate*" --tests "*BaselineRecalculatorUseCaseTest"` | ✅ | green |
| 08-02-02 | 02 | 2 | PPM-03 | T-08-04 | Correction influence decays over 14 days and is capped at 20% | unit | `.\gradlew.bat :app:testDevDebugUnitTest --tests "*CorrectionInfluence*" --tests "*UpdatePersonalizationStateUseCaseTest"` | ✅ | green |
| 08-03-01 | 03 | 3 | CAUS-03, PPM-03 | T-08-05 | UI exposes confidence/recency and safe correction loops without leaking internals | androidTest | `.\gradlew.bat :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.dashboard.details.ExplainabilityUiTest` | ✅ | partial (device unavailable) |

*Status: pending, green, red, flaky, partial*

---

## Wave 0 Requirements

- [x] `app/src/test/java/com/aira/health/domain/engine/CausalRankingEngineTest.kt` - ranking and tie-break test scaffolding
- [x] `app/src/test/java/com/aira/health/domain/usecase/ComputeCausalInsightsUseCaseTest.kt` - causal persistence and confidence/recency evidence tests
- [x] `app/src/test/java/com/aira/health/domain/engine/PersonalizationUpdateEngineTest.kt` - bounded EMA and guardrail tests
- [x] `app/src/test/java/com/aira/health/domain/engine/CorrectionInfluenceEngineTest.kt` - decay and cap tests
- [x] `app/src/androidTest/java/com/aira/health/presentation/dashboard/details/ExplainabilityUiTest.kt` - UI confidence/recency/correction flow coverage (execution requires connected device)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Perceived clarity of factor explanations and confidence labels | CAUS-01, CAUS-03 | UX quality and trust perception are subjective and require human evaluation | Run app with seeded realistic data, review card copy and ordering, confirm labels align with perceived evidence |
| User correction understandability and intent safety | PPM-03 | Human intent and comprehension cannot be fully asserted in automation | Apply correction scenarios and verify wording, confirmation prompts, and post-action explanation quality |
| Connected-device instrumentation execution | CAUS-03, PPM-03 | Current environment has no connected device; instrumentation run is environment-gated | Connect emulator/device and run `./gradlew.bat :app:connectedDevDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.presentation.dashboard.details.ExplainabilityUiTest'` |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all missing references
- [x] No watch-mode flags
- [x] Feedback latency < 300s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** partial 2026-04-18

## Validation Audit 2026-04-18

| Metric | Count |
|--------|-------|
| Gaps found | 1 |
| Resolved | 0 |
| Escalated | 1 |
