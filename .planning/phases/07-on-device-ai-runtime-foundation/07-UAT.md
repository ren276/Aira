---
status: testing
phase: 07-on-device-ai-runtime-foundation
source:
  - .planning/phases/07-on-device-ai-runtime-foundation/07-01-SUMMARY.md
  - .planning/phases/07-on-device-ai-runtime-foundation/07-02-SUMMARY.md
  - .planning/phases/07-on-device-ai-runtime-foundation/07-03-SUMMARY.md
started: "2026-04-17T22:27:00.000Z"
updated: "2026-04-17T22:27:00.000Z"
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

number: 5
name: Prompt Privacy Redaction
expected: |
  Verify that `PromptAssembler` strips URLs and biometric literals before passing context over the network to the Gemini API layer (AIM-03).
status: completed

## Tests

### 1. Gemini Cloud Foundation Test
expected: |
  Start the application on a device. Verify that the `GeminiCloudRuntimeGateway` initializes without crash and compilation succeeds with the new Google Generative AI SDK dependencies.
result: [pass]

### 2. Cloud API Integration Verification
expected: |
  Verify the Gemini Cloud Gateway returns a valid streaming response using the injected `GEMINI_API_KEY` (AIM-01).
result: [pending]

### 3. Generation Cancellation & Timeout
expected: |
  Trigger a long-running inference and cancel it. Verify the gateway gracefully handles network cancellation or timeout and returns `RuntimeFailureReason.TIMEOUT` or `CANCELLED` (PERF-01).
result: [pass] [CloudApiTest.kt:testGeminiCloudTimeout](file:///e:/Aira/app/src/androidTest/java/com/aira/health/ai/runtime/CloudApiTest.kt)
  - [pass] [CloudApiTest.kt:testGeminiCloudCancellation](file:///e:/Aira/app/src/androidTest/java/com/aira/health/ai/runtime/CloudApiTest.kt)

### 4. Wellness-Safe Fallback
expected: |
  Simulate a total network failure or invalid API key. Verify `DeterministicSummaryService` generates a score-based summary that strictly avoids diagnostic language locally (AIM-04).
result: [pass] [DeterministicSummaryServiceTest.kt](file:///e:/Aira/app/src/test/java/com/aira/health/ai/fallback/DeterministicSummaryServiceTest.kt)

### 5. Prompt Privacy Redaction (Cloud Adaptation)
expected: |
  Verify that `PromptAssembler` strips URLs and biometric literals before passing context over the network to the Gemini API layer.
result: [pass] [PromptRedactionTest.kt](file:///e:/Aira/app/src/test/java/com/aira/health/ai/prompt/PromptRedactionTest.kt)


## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]
