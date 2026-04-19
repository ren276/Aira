# Phase 7: On-Device AI Runtime Foundation - Research

**Researched:** 2026-04-17
**Domain:** Android on-device LLM runtime integration (MediaPipe Tasks GenAI + TensorFlow Lite)
**Confidence:** MEDIUM

## User Constraints

- No `CONTEXT.md` exists for this phase, so there are no locked discuss-phase decisions to copy verbatim. [VERIFIED: phase init output has `has_context: false`]
- Phase scope is constrained to requirements `AIM-01`, `AIM-02`, `AIM-03`, `AIM-04`, `PERF-01`, `PERF-02`, `PERF-03`. [VERIFIED: .planning/ROADMAP.md and .planning/REQUIREMENTS.md]
- Implementation must be Kotlin Android and on-device runtime oriented using MediaPipe/TFLite path for this phase. [VERIFIED: user request + .planning/phases/07-on-device-ai-runtime-foundation/07-AI-SPEC.md]

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AIM-01 | App can load and run a compact on-device text model (TFLite class) for inference on supported Android devices. | Standard stack pins MediaPipe Tasks GenAI + TFLite, model artifact loading pattern, and device compatibility caveats. [VERIFIED: requirements + AI spec + Google AI Edge docs] |
| AIM-02 | All AI inference runs fully on-device for core coaching features. | Architecture keeps prompt assembly, inference, and post-processing local; no network dependency in generation path. [VERIFIED: requirements + AI spec] |
| AIM-03 | Raw biometric records never leave local storage during AI processing and syncing. | Security and architecture sections define strict local-only data boundary, redacted telemetry, and no raw biometrics in cloud sync path. [VERIFIED: requirements + project scope docs] |
| AIM-04 | App falls back to deterministic non-AI summaries when model is unavailable. | Inference service contract includes deterministic fallback branch with explicit reason codes and tests. [VERIFIED: roadmap success criteria + AI spec] |
| PERF-01 | AI inference executes off the UI thread and is cancellable. | Coroutine + ViewModel scope pattern, async listener usage, and cancellation/timeout policy. [CITED: developer.android.com/topic/libraries/architecture/coroutines, kotlinlang.org/docs/cancellation-and-timeouts.html, ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android] |
| PERF-02 | AI feature latency remains within acceptable interactive budget on target devices. | Macrobenchmark + instrumentation benchmark plan with p50/p95 gates and CI pass/fail thresholds. [CITED: developer.android.com/topic/performance/benchmarking/macrobenchmark-overview] |
| PERF-03 | AI memory usage remains within defined mobile safety budget without app instability. | Memory instrumentation and release-build validation with guardrails and watchdog fallback. [CITED: developer.android.com/topic/performance/vitals/anr; VERIFIED: AI spec budget targets] |

</phase_requirements>

## Summary

Phase 7 should be planned as a runtime-foundation phase, not a feature-completeness phase: deliver a reliable local inference substrate first, then expand domain intelligence in later phases. The strongest implementation path is to keep a single long-lived engine handle and create short-lived per-request sessions, with explicit timeout and cancellation semantics, while preserving deterministic fallback output for all failure modes. [VERIFIED: AI spec architecture + Android coroutine docs + Kotlin cancellation docs]

MediaPipe LLM Inference on Android is currently usable but explicitly marked as deprecated in favor of LiteRT-LM, so the plan should avoid hard-coding runtime-specific APIs into domain layers. Build a narrow runtime adapter interface now, keep MediaPipe in one infrastructure module, and plan migration hooks for LiteRT-LM in a later phase. [CITED: ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android and ai.google.dev/edge/litert-lm/overview]

Performance and safety verification must be first-class in this phase. There is no current AI runtime code in the repo, no macrobenchmark module, and no AI-specific tests; therefore, Wave 0 should create benchmark/test scaffolding before integration work to avoid late regressions. [VERIFIED: repo search and Gradle task listing]

**Primary recommendation:** Implement a strict `AiRuntimeGateway` adapter over MediaPipe 0.10.22 first, with cancellable coroutines, deterministic fallback, and benchmark gates committed in the same phase. [VERIFIED: AI spec + requirements]

## Project Constraints (from copilot-instructions.md)

- Use GSD skills when user invokes `gsd-*` workflows. [VERIFIED: .github/copilot-instructions.md]
- Treat `gsd-*` commands as command invocations from `.github/skills/gsd-*`. [VERIFIED: .github/copilot-instructions.md]
- Prefer matching custom agents when commands request spawning subagents. [VERIFIED: .github/copilot-instructions.md]
- Do not apply GSD workflows unless explicitly asked. [VERIFIED: .github/copilot-instructions.md]
- After completing `gsd-*` deliverables, offer the user next-step prompts. [VERIFIED: .github/copilot-instructions.md]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Model artifact provisioning and integrity checks | Android Client Infrastructure | Local Storage | Model is consumed on device; artifact path and checksum belong in app infra, not UI. [VERIFIED: AI spec + MediaPipe Android setup docs] |
| Inference execution and streaming | Native Runtime Layer (MediaPipe/TFLite JNI) | Android Client Domain Service | Runtime performs generation; domain orchestrates prompt contract and fallback policy. [VERIFIED: AI spec + MediaPipe API docs] |
| UI interaction and cancellation | Android UI/ViewModel Layer | Domain Service | `viewModelScope` lifecycle cancellation should own request lifetime control. [CITED: developer.android.com/topic/libraries/architecture/coroutines] |
| Privacy boundary enforcement | Local Data Layer | Domain Service | Raw biometrics remain local and are transformed into compact prompt features before inference. [VERIFIED: requirements + project docs] |
| Fallback deterministic summaries | Domain Service | UI Layer | Fallback logic must be deterministic and testable independent of runtime availability. [VERIFIED: roadmap criteria + AI spec] |
| Latency and memory telemetry gates | Benchmark/Test Layer | CI Pipeline | Macrobenchmark/instrumentation outputs enforce PERF requirements during merges. [CITED: developer.android.com/topic/performance/benchmarking/macrobenchmark-overview] |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.mediapipe:tasks-genai` | `0.10.22` (phase lock), latest available `0.10.33` | On-device Android LLM runtime API | AI-SPEC locks `0.10.22` for this phase; official Android guide documents API and async listeners. [VERIFIED: AI-SPEC + Google Maven metadata + MediaPipe docs] |
| `org.tensorflow:tensorflow-lite` | `2.17.0` | TFLite runtime used under/alongside on-device inference path | Version already pinned in repo and appears as latest in Maven Central query used in this session. [VERIFIED: gradle/libs.versions.toml + Maven Central query] |
| Kotlin Coroutines (`kotlinx-coroutines-android`) | `1.9.0` | Async execution, cancellation, timeout handling | Cancellation semantics map directly to PERF-01 requirements. [VERIFIED: version catalog + Kotlin cancellation docs] |
| Android Lifecycle KTX | `2.8.7` in repo | `viewModelScope` and lifecycle-aware cancellation | Official Android guidance recommends lifecycle scopes for coroutine work tied to UI lifecycle. [VERIFIED: version catalog; CITED: developer.android.com/topic/libraries/architecture/coroutines] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.benchmark:benchmark-macro-junit4` | `1.4.1` stable (latest line has `1.5.0-alpha05`) | Macrobenchmark for p95 latency and frame impact | Use for PERF-02 gates on physical devices and CI perf reports. [VERIFIED: Google Maven metadata; CITED: macrobenchmark docs] |
| `androidx.profileinstaller:profileinstaller` | `1.4.1` | Required support for profile capture/reset in benchmark setup | Add when creating benchmark module and benchmark build type. [CITED: macrobenchmark docs; VERIFIED: Google Maven metadata] |
| WorkManager (`androidx.work:work-runtime-ktx`) | `2.9.1` | Background prefetch/warmup/model download orchestration | Use only for non-UI-bound background tasks, not foreground inference calls. [VERIFIED: version catalog; ASSUMED] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| MediaPipe LLM Inference API | LiteRT-LM Kotlin API | LiteRT-LM is the forward path and production-ready, but AI-SPEC for this phase selected MediaPipe 0.10.22; defer migration to a dedicated phase with adapter seam. [CITED: LiteRT-LM overview + MediaPipe deprecation note + AI-SPEC] |
| Full custom TFLite Kotlin wrapper | MediaPipe Tasks GenAI abstraction | Custom wrapper gives more control but increases JNI/config complexity and schedule risk for foundation phase. [VERIFIED: AI-SPEC rationale] |

**Installation:**

```kotlin
// app/build.gradle.kts
implementation("com.google.mediapipe:tasks-genai:0.10.22")
implementation("org.tensorflow:tensorflow-lite:2.17.0")
```

**Version verification (executed this session):**

- `tasks-genai` metadata reports latest/release `0.10.33`, `lastUpdated` `20260320235248`. [VERIFIED: https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-genai/maven-metadata.xml]
- `tasks-genai-0.10.22.pom` `Last-Modified: Tue, 11 Mar 2025 16:49:26 GMT`. [VERIFIED: HTTP HEAD request]
- `tasks-genai-0.10.27.pom` `Last-Modified: Mon, 18 Aug 2025 17:06:54 GMT`. [VERIFIED: HTTP HEAD request]
- `tensorflow-lite` latest observed in Maven Central query: `2.17.0` (`timestamp 1736448815193`). [VERIFIED: search.maven.org API query]

## Architecture Patterns

### System Architecture Diagram

```text
[Local Health Data (Room/SQLCipher)]
                |
                v
[Feature Extractor / Prompt Contract Builder] ---> [Token budget + truncation policy]
                |                                             |
                v                                             v
        [AiRuntimeGateway Interface] --------------> [MediaPipe Engine Handle]
                |                                             |
                |<-- success stream (partial/final text) -----|
                |
                |-- runtime error / timeout / memory guard --> [DeterministicFallbackService]
                |
                v
       [ViewModel StateFlow (loading/partial/final/fallbackReason)]
                |
                v
               UI

Telemetry side channel:
[Runtime timers + memory stats + fallback reason] -> [Local metrics log + benchmark assertions]
```

### Recommended Project Structure

```text
app/src/main/java/com/aira/health/
  ai/
    runtime/
      AiRuntimeGateway.kt
      MediapipeRuntimeGateway.kt
      RuntimeSession.kt
      RuntimeConfig.kt
      RuntimeHealthProbe.kt
    prompt/
      PromptContract.kt
      PromptAssembler.kt
      PromptBudgeter.kt
    fallback/
      DeterministicSummaryService.kt
      FallbackReason.kt
    orchestration/
      InferenceOrchestrator.kt
      InferenceRequest.kt
      InferenceResult.kt
  di/
    AiRuntimeModule.kt
app/src/test/java/com/aira/health/ai/
  runtime/
  fallback/
  orchestration/
app/src/androidTest/java/com/aira/health/ai/
  runtime/
```

### Pattern 1: Engine-Singleton, Session-Per-Request

**What:** Keep one `LlmInference` engine alive and create/close `LlmInferenceSession` per generation request. [VERIFIED: AI-SPEC]

**When to use:** Always for repeated in-app requests to avoid reload overhead and lifecycle leaks. [ASSUMED]

**Example:**

```kotlin
// Source: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
val options = LlmInference.LlmInferenceOptions.builder()
    .setModelPath(modelPath)
    .setMaxTopK(64)
    .build()

val llm = LlmInference.createFromOptions(context, options)
val session = LlmInferenceSession.createFromOptions(llm, sessionOptions)
```

### Pattern 2: Cancellable Async Generation from ViewModel Scope

**What:** Run generation from `viewModelScope`, cancel on navigation/teardown, and never block main thread waiting for completion. [CITED: Android coroutines guide + Kotlin cancellation docs]

**When to use:** Every interactive generation path triggered from Compose screens. [ASSUMED]

**Example:**

```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/coroutines
class AiViewModel : ViewModel() {
    fun generate() {
        viewModelScope.launch {
            withTimeoutOrNull(2500) {
                gateway.generateAsync(prompt)
            } ?: fallbackService.buildTimeoutSummary()
        }
    }
}
```

### Pattern 3: Privacy-First Prompt Assembly

**What:** Build prompts only from transformed, compact features (scores, aggregates, confidence) and prevent raw biometric event payloads from entering prompt text or logs. [VERIFIED: requirements + project constraints]

**When to use:** All AI request composition paths and telemetry. [VERIFIED: phase requirements]

### Anti-Patterns to Avoid

- **Blocking generate call on main thread:** Causes ANR risk and violates `PERF-01`. [CITED: ANR docs]
- **Embedding model binaries in APK:** Model sizes are too large; use download/provisioning flow. [CITED: MediaPipe Android quickstart states model too large for APK]
- **Direct UI dependency on MediaPipe classes:** Prevents future migration to LiteRT-LM and complicates tests. [CITED: MediaPipe deprecation note + LiteRT-LM overview]
- **No deterministic fallback reason codes:** Makes `AIM-04` unverifiable and harms incident triage. [VERIFIED: requirements]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| End-to-end benchmark harness | Custom stopwatch scripts in app code | Jetpack Macrobenchmark module | Provides repeatable metrics, traces, and CI outputs. [CITED: macrobenchmark docs] |
| Coroutine lifecycle cancellation framework | Manual thread interruption + flags | `viewModelScope`, `withTimeoutOrNull`, cooperative cancellation | Standard cancellation propagation is safer and testable. [CITED: Android coroutines + Kotlin cancellation docs] |
| LLM runtime JNI bridge | Custom JNI/TFLite glue in Phase 7 | MediaPipe Tasks GenAI adapter | Lower integration risk for foundation milestone. [VERIFIED: AI-SPEC rationale] |
| Fallback text from ad-hoc templates in UI | Scattered string logic in composables | Central deterministic fallback service | Keeps behavior testable and requirement traceable. [VERIFIED: AIM-04 + roadmap success criteria] |

**Key insight:** Phase 7 should hand-roll domain policy, not runtime plumbing; runtime plumbing should stay behind adapter boundaries to reduce migration cost. [CITED: MediaPipe deprecation note + LiteRT-LM direction]

## Common Pitfalls

### Pitfall 1: API Drift and Deprecation Mismatch

**What goes wrong:** Team implements against snippets from older API names while current docs and artifacts drift, causing compile/runtime mismatch. [CITED: AI-SPEC pitfall + MediaPipe docs]

**Why it happens:** MediaPipe LLM Inference is still available but now explicitly marked deprecated and ecosystem momentum moved to LiteRT-LM. [CITED: MediaPipe Android page + LiteRT-LM overview]

**How to avoid:** Lock `0.10.22` in this phase, isolate API usage to one gateway, and write contract tests against adapter behavior. [VERIFIED: AI-SPEC + phase goal]

**Warning signs:** Frequent compile errors around options/session APIs, repeated dependency pin churn, or gateway leaking MediaPipe types. [ASSUMED]

### Pitfall 2: UI Thread Blocking Through Implicit Waits

**What goes wrong:** Inference callback bridged with blocking wait on main thread or long sync call leads to jank/ANR. [CITED: ANR docs]

**Why it happens:** Convenience wrappers call blocking APIs or misuse synchronization between callback and UI thread. [ASSUMED]

**How to avoid:** Enforce asynchronous generation path and cancellation checks, plus StrictMode in debug builds for accidental main-thread I/O. [CITED: ANR docs + Kotlin cancellation docs]

**Warning signs:** `Input dispatching timed out`, frame drops around generation, or ANR traces pointing to inference wait path. [CITED: ANR docs]

### Pitfall 3: Memory Pressure from Model Load and Session Leaks

**What goes wrong:** Recreating engines per request or not closing sessions increases memory and instability. [VERIFIED: AI-SPEC pitfall list]

**Why it happens:** Lifecycle ownership not explicit between app, ViewModel, and runtime objects. [ASSUMED]

**How to avoid:** One engine per active model profile, session-per-request, `close()` in `finally`, and memory watchdog fallback branch. [VERIFIED: AI-SPEC implementation guidance]

**Warning signs:** Rising native heap after repeated requests, fallback rate spikes by OOM reason, crashes only under release profile. [ASSUMED]

## Code Examples

Verified patterns from official sources:

### Async response streaming

```kotlin
// Source: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
val options = LlmInference.LlmInferenceOptions.builder()
    .setModelPath(modelPath)
    .setResultListener { partialResult, done ->
        // Stream partial output
    }
    .build()

val llmInference = LlmInference.createFromOptions(context, options)
llmInference.generateResponseAsync(inputPrompt)
```

### Lifecycle-aware cancellation in ViewModel

```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/coroutines
class RuntimeViewModel : ViewModel() {
    fun run(prompt: String) {
        viewModelScope.launch {
            val result = withTimeoutOrNull(2500) {
                runtimeGateway.generate(prompt)
            }
            emit(result ?: fallbackService.buildTimeoutSummary())
        }
    }
}
```

### Macrobenchmark skeleton

```kotlin
// Source: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
@RunWith(AndroidJUnit4::class)
class AiInferenceBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.aira.health",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
    ) {
        // trigger target app action
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| MediaPipe LLM Inference as primary direction | LiteRT-LM is the recommended forward path; MediaPipe LLM Inference marked deprecated but still available | Document updated 2026-03-31 | Keep Phase 7 adapter boundary so migration is low-cost. [CITED: MediaPipe Android page + LiteRT-LM overview] |
| Treating benchmark as optional post-work | Benchmark gating expected during implementation in CI for reliable regressions | Macrobenchmark docs current as of 2026-04-16 | Add benchmark scaffold in Wave 0, not at phase end. [CITED: macrobenchmark docs] |

**Deprecated/outdated:**

- MediaPipe LLM Inference as long-term target runtime: available but deprecated in docs; LiteRT-LM recommended for future migration. [CITED: MediaPipe Android page]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | WorkManager should handle model provisioning tasks for this phase | Standard Stack | May overcomplicate if model provisioning strategy is already fixed elsewhere |
| A2 | One engine per model profile is optimal for all target devices | Architecture Patterns | Could hurt low-memory devices if engine residency is too expensive |
| A3 | Initial timeout target around 2500 ms is suitable for interactive budget | Pattern 2 / Validation | Could fail UX expectations on lower-end devices or be too strict on first-token latency |
| A4 | Adapter seam is sufficient for future LiteRT-LM migration without data-model changes | Summary / SOTA | Migration may still require broader prompt/session contract changes |

## Open Questions (RESOLVED)

1. **Latency/RAM gate strictness for `PERF-02` and `PERF-03`**
    - **Resolution:** Treat AI-SPEC thresholds as hard Phase 7 release gates for the benchmarked path: p95 <= 2500 ms and peak additional RAM <= 1200 MB. Add device metadata to evidence so tier-specific tuning can be introduced in later phases without weakening current gate criteria.
    - **Planning impact:** Plan 07-03 must include parser-enforced thresholds and physical-device evidence before phase completion.

2. **`tasks-genai` version strategy in this phase**
    - **Resolution:** Keep `com.google.mediapipe:tasks-genai:0.10.22` locked for Phase 7 execution. Do not perform runtime migration in this phase; enforce adapter isolation so migration to newer stack can be planned in Phase 8+.
    - **Planning impact:** Plans must avoid leaking MediaPipe-specific types beyond runtime adapter boundaries.

3. **Model artifact distribution approach**
    - **Resolution:** For Phase 7 foundation work, use a development-compatible local provisioning path with integrity verification hooks (checksum/signature contract), and defer production rollout channel selection (staged/server policy) to later milestone planning.
    - **Planning impact:** Include explicit task acceptance criteria for artifact integrity checks and deterministic unavailable-model fallback behavior.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java runtime | Android builds/tests | Yes | `23.0.2` | - |
| `gradlew` wrapper | Build and test commands | Yes | `Gradle 8.13` | - |
| Android `adb` on PATH | Instrumented tests and on-device perf runs | No | - | Use SDK-local `platform-tools/adb.exe` from `local.properties` |
| Android `sdkmanager` on PATH | SDK package management for CI/dev boxes | No | - | Use SDK-local `cmdline-tools/latest/bin/sdkmanager.bat` |
| SDK-local `adb` | Device attach and instrumentation | Yes | `1.0.41 / 37.0.0-14910828` | - |
| SDK-local `sdkmanager` | Tool/package management | Yes (with warning) | `20.0` | Align cmdline-tools with Android Studio version |
| Connected physical Android device | Macrobenchmark and realistic on-device LLM validation | No device attached at audit time | - | Manual requirement: attach supported device before PERF validation |

**Missing dependencies with no fallback:**

- Active physical test device for realistic LLM performance measurement and macrobenchmark confidence.

**Missing dependencies with fallback:**

- `adb` and `sdkmanager` missing on PATH, but available through SDK-local paths from `local.properties`.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit5 + JUnit4 compatibility + Android instrumentation (`androidTest`) [VERIFIED: app/build.gradle.kts] |
| Config file | `app/build.gradle.kts` (`testOptions { unitTests.all { useJUnitPlatform() } }`) |
| Quick run command | `./gradlew :app:testDevDebugUnitTest` |
| Full suite command | `./gradlew :app:testDevDebugUnitTest :app:testStagingDebugUnitTest :app:testProdDebugUnitTest :app:connectedDevDebugAndroidTest` |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AIM-01 | Runtime loads model and returns generated response on supported device | integration (androidTest) | `./gradlew :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.ai.runtime.MediapipeRuntimeIntegrationTest` | No - Wave 0 |
| AIM-02 | Inference path executes fully on-device without network dependency | integration + unit | `./gradlew :app:testDevDebugUnitTest --tests "*AiRuntimePrivacy*"` | No - Wave 0 |
| AIM-03 | Raw biometrics are not sent off-device during AI flow | unit + integration | `./gradlew :app:testDevDebugUnitTest --tests "*PromptRedaction*"` | No - Wave 0 |
| AIM-04 | Deterministic fallback is emitted when model unavailable/timeout/error | unit | `./gradlew :app:testDevDebugUnitTest --tests "*DeterministicFallbackServiceTest"` | No - Wave 0 |
| PERF-01 | Generation is off main thread and cancellable | unit + instrumentation | `./gradlew :app:testDevDebugUnitTest --tests "*InferenceCancellation*"` | No - Wave 0 |
| PERF-02 | Latency p50/p95 stays inside interactive budget | macrobenchmark | `./gradlew :macrobenchmark:connectedCheck` | No - Wave 0 |
| PERF-03 | Additional memory stays within safety budget | macrobenchmark + instrumentation | `./gradlew :macrobenchmark:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.aira.health.ai.benchmark.AiMemoryBenchmark` | No - Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDevDebugUnitTest`
- **Per wave merge:** `./gradlew :app:testDevDebugUnitTest :app:testStagingDebugUnitTest :app:testProdDebugUnitTest`
- **Phase gate:** Full suite plus instrumented AI runtime tests and macrobenchmark report pass on physical device

### Wave 0 Gaps

- [ ] `app/src/main/java/com/aira/health/ai/runtime/AiRuntimeGateway.kt` - runtime abstraction seam
- [ ] `app/src/main/java/com/aira/health/ai/runtime/MediapipeRuntimeGateway.kt` - concrete MediaPipe implementation
- [ ] `app/src/main/java/com/aira/health/ai/fallback/DeterministicSummaryService.kt` - AIM-04 deterministic fallback
- [ ] `app/src/test/java/com/aira/health/ai/fallback/DeterministicSummaryServiceTest.kt` - AIM-04 coverage
- [ ] `app/src/test/java/com/aira/health/ai/runtime/InferenceCancellationTest.kt` - PERF-01 cancellation coverage
- [ ] `app/src/test/java/com/aira/health/ai/runtime/PromptRedactionTest.kt` - AIM-03 privacy boundary checks
- [ ] `app/src/androidTest/java/com/aira/health/ai/runtime/MediapipeRuntimeIntegrationTest.kt` - AIM-01/AIM-02 integration checks
- [ ] `macrobenchmark/` module - PERF-02/PERF-03 measurement harness
- [ ] Macrobenchmark deps: add `androidx.benchmark:benchmark-macro-junit4` and `androidx.profileinstaller:profileinstaller`

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No (phase focus is runtime foundation) | Reuse existing app auth; no new auth surface in this phase. [VERIFIED: phase scope] |
| V3 Session Management | No (no new remote session protocol) | Keep existing session behavior unchanged. [VERIFIED: phase scope] |
| V4 Access Control | Partial | Restrict AI runtime access behind app-layer use cases, not direct UI calls. [ASSUMED] |
| V5 Input Validation | Yes | Validate and bound prompt inputs, token budget, and fallback reason enums. [VERIFIED: AI-SPEC prompt discipline] |
| V6 Cryptography | Yes | Keep biometrics in SQLCipher-backed local store; hash/sign model artifacts before activation. [VERIFIED for SQLCipher in repo; ASSUMED for model hash policy] |

### Known Threat Patterns for Android on-device AI runtime

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Prompt injection from free-text user notes causing unsafe advice | Tampering | Prompt contract guardrails + deterministic safety post-check + fallback. [VERIFIED: AI-SPEC guardrails] |
| Sensitive health detail leakage in logs/telemetry | Information Disclosure | Redact prompt and output logs; store only aggregate counters/reason codes. [VERIFIED: AIM-03 intent; ASSUMED implementation detail] |
| Model artifact tampering before load | Tampering | Verify artifact checksum/signature before `createFromOptions`. [ASSUMED] |
| Denial of service through oversized prompts | DoS | Enforce strict token budget/truncation and timeout cancellation. [VERIFIED: AI-SPEC context strategy + Kotlin timeout docs] |
| Overconfident wellness claims crossing medical boundary | Repudiation/Integrity | Claim-boundary filter and deterministic fallback for disallowed language. [VERIFIED: AI-SPEC guardrail requirements] |

## Sources

### Primary (HIGH confidence)

- https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android - Android API usage, deprecation note, model packaging constraints, async generation, updated 2026-03-31.
- https://ai.google.dev/edge/litert-lm/overview - Current runtime direction and status of LiteRT-LM, updated 2026-04-02.
- https://developer.android.com/topic/libraries/architecture/coroutines - Lifecycle coroutine patterns and cancellation guidance, updated 2026-03-05.
- https://developer.android.com/topic/performance/vitals/anr - Main-thread blocking and ANR thresholds, updated 2026-03-05.
- https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview - Macrobenchmark setup and CI integration guidance, updated 2026-04-16.
- https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-genai/maven-metadata.xml - verified available versions and recency.
- https://dl.google.com/dl/android/maven2/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml - verified benchmark artifact versions.
- https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/maven-metadata.xml - verified profileinstaller artifact versions.
- https://search.maven.org/solrsearch/select?q=g:%22org.tensorflow%22+AND+a:%22tensorflow-lite%22&core=gav&rows=5&wt=json - verified TensorFlow Lite version recency.
- Repository evidence: `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md`, `.planning/phases/07-on-device-ai-runtime-foundation/07-AI-SPEC.md`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.

### Secondary (MEDIUM confidence)

- `.planning/codebase/TESTING.md` (helpful but stale against current tree; used only as supplemental context). [VERIFIED by mismatch with observed androidTest files]

### Tertiary (LOW confidence)

- None.

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH - versions and API direction verified from official metadata/docs.
- Architecture: MEDIUM - core pattern verified, but final boundaries depend on unresolved decisions (artifact distribution and migration timing).
- Pitfalls: MEDIUM - some signals verified by docs/spec, some operational warning signs are assumptions.

**Research date:** 2026-04-17
**Valid until:** 2026-05-17 (30 days)
