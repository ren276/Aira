---
phase: 07
slug: on-device-ai-runtime-foundation
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-17
---

# Phase 07 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit5 + JUnit4 compatibility + Android instrumentation (`androidTest`) |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew :app:testDevDebugUnitTest` |
| **Full suite command** | `./gradlew :app:testDevDebugUnitTest :app:testStagingDebugUnitTest :app:testProdDebugUnitTest :app:connectedDevDebugAndroidTest` |
| **Estimated runtime** | ~360 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDevDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:testDevDebugUnitTest :app:testStagingDebugUnitTest :app:testProdDebugUnitTest`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds (quick gate)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | AIM-01 | T-07-01 | Model load and generation path succeeds on supported device | integration | `./gradlew :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.ai.runtime.MediapipeRuntimeIntegrationTest` | ❌ W0 | ⬜ pending |
| 07-01-02 | 01 | 1 | AIM-02 | T-07-02 | Inference path is local-only and does not require network | unit + integration | `./gradlew :app:testDevDebugUnitTest --tests "*AiRuntimePrivacy*"` | ❌ W0 | ⬜ pending |
| 07-02-01 | 02 | 1 | AIM-04 | T-07-03 | Deterministic fallback returned for unavailable/timeout/error runtime states | unit | `./gradlew :app:testDevDebugUnitTest --tests "*DeterministicSummaryServiceTest"` | ❌ W0 | ⬜ pending |
| 07-02-02 | 02 | 1 | PERF-01 | T-07-04 | Generation runs off main thread and supports cancellation | unit + instrumentation | `./gradlew :app:testDevDebugUnitTest --tests "*InferenceCancellation*"` | ❌ W0 | ⬜ pending |
| 07-02-03 | 02 | 1 | AIM-03 | T-07-05 | Prompt assembly/logging excludes raw biometric payload leakage | unit | `./gradlew :app:testDevDebugUnitTest --tests "*PromptRedaction*"` | ❌ W0 | ⬜ pending |
| 07-03-01 | 03 | 2 | PERF-02 | T-07-06 | p50/p95 latency stays within budget on target device tiers | macrobenchmark | `./gradlew :macrobenchmark:connectedCheck` | ❌ W0 | ⬜ pending |
| 07-03-02 | 03 | 2 | PERF-03 | T-07-07 | Peak additional RAM remains within safety budget | macrobenchmark + instrumentation | `./gradlew :macrobenchmark:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.ai.benchmark.AiMemoryBenchmark` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/main/java/com/aira/health/ai/runtime/AiRuntimeGateway.kt` - gateway seam for runtime abstraction
- [ ] `app/src/main/java/com/aira/health/ai/runtime/MediapipeRuntimeGateway.kt` - concrete MediaPipe adapter
- [ ] `app/src/main/java/com/aira/health/ai/fallback/DeterministicSummaryService.kt` - deterministic fallback implementation
- [ ] `app/src/test/java/com/aira/health/ai/fallback/DeterministicSummaryServiceTest.kt` - AIM-04 coverage
- [ ] `app/src/test/java/com/aira/health/ai/runtime/InferenceCancellationTest.kt` - PERF-01 coverage
- [ ] `app/src/test/java/com/aira/health/ai/runtime/PromptRedactionTest.kt` - AIM-03 coverage
- [ ] `app/src/androidTest/java/com/aira/health/ai/runtime/MediapipeRuntimeIntegrationTest.kt` - AIM-01/AIM-02 coverage
- [ ] `macrobenchmark/` module with benchmark dependencies and runner setup

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| On-device latency on representative physical devices | PERF-02 | Emulator metrics are not representative for AI runtime | Attach at least one target low-mid device and one high-tier device; run macrobenchmark and collect p50/p95 outputs |
| Peak memory pressure and thermal impact under repeated runs | PERF-03 | Needs real hardware thermals/memory behavior | Run 20 repeated inference loops via benchmark profile and inspect memory/thermal logs |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all missing test references
- [x] No watch-mode flags
- [x] Feedback latency < 120s for quick gate
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
