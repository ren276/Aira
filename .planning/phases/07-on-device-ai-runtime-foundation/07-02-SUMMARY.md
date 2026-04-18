---
plan: 07-02
phase: 07-on-device-ai-runtime-foundation
status: complete
completed_at: 2026-04-17
commit: 54d43ce
---

# 07-02 Summary: Privacy-Safe Service Layer

## What Was Built

Delivered the prompt assembly and deterministic fallback orchestration layer, enforcing privacy boundaries and generating wellness-safe output on runtime failures.

### key-files.created

- `app/src/main/java/com/aira/health/ai/prompt/PromptContract.kt` — Typed prompt contract boundaries and `MetricSnapshot` aggregate DTO (AIM-03).
- `app/src/main/java/com/aira/health/ai/prompt/PromptAssembler.kt` — Pure prompt builder with strict type separation from Room entities, `redactFreeText` rules for free-text stripping (URLs/medical/biometric literals), and deterministic token pruning (AIM-03).
- `app/src/main/java/com/aira/health/ai/fallback/FallbackReason.kt` — 6 enum outcomes covering all possible AI-bypass paths.
- `app/src/main/java/com/aira/health/ai/fallback/DeterministicSummaryService.kt` — Pure score-threshold text generator enforcing strict wellness language boundaries (AIM-04).
- `app/src/main/java/com/aira/health/ai/orchestration/InferenceOrchestrator.kt` — Central `Flow` emit pipeline binding PromptAssembler, AiRuntimeGateway, and DeterministicSummaryService. Maps inner exceptions and cancellations seamlessly into `InferenceOutcome.Fallback` (PERF-01).

### unit-tests.created
- `PromptRedactionTest` — Validates biometric identifiers, medical terms, IP addresses, and URLs are properly stripped, and that raw biometric entities cannot leak into the `MetricSnapshot` mapper.
- `DeterministicSummaryServiceTest` — Validates wellness-language boundary (blocking diagnostic language) and deterministic stability of the fallback text generator.
- `InferenceCancellationTest` — Validates `InferenceOrchestrator` maps generation timeouts, cancellations, and MP runtime failures reliably into safe Fallback paths without crashing the caller, using a custom Kotlin fake gateway (`kotlinx-coroutines-test`) fully decoupled from the Java-21 MediaPipe Tasks GenAI jar.

## Refactors

- `RuntimeConfig` was decoupled from `LlmInference.Backend` to a custom `HardwareBackend` enum, preventing Robolectric host-side JVM 17 classloading issues with MediaPipe's JVM 21 binaries.

## Requirements Covered

| Req | Coverage |
|-----|----------|
| AIM-03 | Assembler relies purely on `MetricSnapshot`; raw Room entities intentionally blocked at compile time. |
| AIM-04 | Deterministic inference fails to standard baseline wording (No diagnostic terminology allowed). |

## Verification

- `./gradlew :app:testDevDebugUnitTest` → **BUILD SUCCESSFUL** (All 37 unit tests pass).

## Self-Check: PASSED
