---
plan: 07-03
phase: 07-on-device-ai-runtime-foundation
status: complete
completed_at: 2026-04-17
commit: 9ad5fdc
---

# 07-03 Summary: Performance CI Gates

## What Was Built

Added the physical-device macrobenchmark harness and CI bash script, enforcing memory and latency boundaries for the AI runtime layer.

### key-files.created

- `gradle/libs.versions.toml` — Added `benchmark-macro`, `profileinstaller`, and `uiautomator` dependencies.
- `app/build.gradle.kts` — Integrated benchmark dependencies into the `androidTest` source set.
- `app/src/androidTest/java/com/aira/health/ai/macrobenchmark/AiInferenceMacrobenchmark.kt` — Jetpack Macrobenchmark suite measuring cold startup latency (preventing main-thread initialisation regressions) and peak memory usage during inference. Runs strictly on physical test devices via the `StartupTimingMetric` and `MemoryUsageMetric`.
- `scripts/perf-gate.sh` — CI integration script that checks for a connected device, validates the presence of the Gemma 4 test artifact (`gemma4_q4.bin`), and triggers the connected macrobenchmarks with the correct flags.

## Requirements Covered

| Req | Coverage |
|-----|----------|
| PERF-01 | Baseline established for measuring main thread blocking via `StartupTimingMetric`. |
| PERF-02 | Memory payload measured via `MemoryUsageMetric(Mode.MAX)` to detect out-of-memory regressions on constrained devices. |

## Verification

Due to the nature of Macrobenchmark tests requiring physical hardware and `userdebug` mode, the tests act as CI gates rather than unit tests. Shell script permissions were verified and dependencies successfully resolved in Gradle.

## Self-Check: PASSED
