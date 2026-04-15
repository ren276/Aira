---
status: complete
phase: 03-scoring-engines-logic
source:
  - 03-01-SUMMARY.md
  - 03-02-SUMMARY.md
  - 03-03-SUMMARY.md
started: 2026-04-15T21:02:41.1453983+05:30
updated: 2026-04-15T21:31:53.7218797+05:30
---

## Current Test

[testing complete]

## Tests

### 1. Recovery and Sleep with partial data

expected: After a sync day where some sleep/recovery inputs are missing, the app still shows visible Recovery and Sleep scores (not blank or hidden), and confidence is shown as lower than a full-data day.
result: issue
reported: "App crashes in background worker with java.lang.IllegalStateException: Cannot extract Keystore key bytes from KeystoreManager.getDatabasePassphrase."
severity: blocker

### 2. Strain non-linear intensity behavior

expected: A workout with more high-zone time (Zone 4/5) yields a noticeably higher Strain score than a similar-duration lower-zone session.
result: pass

### 3. Stress spike amplification

expected: A day with a few highly stressful hours and mostly calm hours shows a higher daily Stress score than a simple average would suggest.
result: pass

### 4. Energy Bank distinctness and carry-over

expected: Energy Bank does not simply mirror Recovery/Strain/Stress, and it changes day to day based on recharge and depletion trends.
result: pass

### 5. Backfill recomputes subsequent days

expected: After adding older historical health data and re-running sync, subsequent days update consistently rather than only the inserted historical date changing.
result: pass

### 6. End-to-end worker scoring write

expected: A normal sync run completes ingest then scoring, and all expected daily score fields are present in app-visible daily metrics.
result: pass

## Summary

total: 6
passed: 5
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "After a sync day where some sleep/recovery inputs are missing, the app still shows visible Recovery and Sleep scores (not blank or hidden), and confidence is shown as lower than a full-data day."
  status: failed
  reason: "User reported: App crashes in background worker with java.lang.IllegalStateException: Cannot extract Keystore key bytes from KeystoreManager.getDatabasePassphrase."
  severity: blocker
  test: 1
  root_cause: "KeystoreManager attempted to read key.encoded from Android Keystore AES key, but Android Keystore keys are non-exportable and return null for encoded bytes."
  artifacts:
    - path: "app/src/main/java/com/aira/health/util/security/KeystoreManager.kt"
      issue: "getDatabasePassphrase used key.encoded and threw IllegalStateException on null."
  missing:
    - "Persist a random SQLCipher passphrase encrypted with the Keystore key instead of extracting key bytes."
  debug_session: "logcat-2026-04-15-keystore-passphrase"
