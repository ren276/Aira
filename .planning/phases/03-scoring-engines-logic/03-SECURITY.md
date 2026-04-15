---
phase: 03
slug: scoring-engines-logic
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-15
---

# Phase 03 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| ingest pipeline -> scoring engines | Untrusted, sparse, and delayed sensor values enter deterministic score math | HRV, RHR, sleep, activity zone inputs |
| scoring engines -> persisted daily metrics | Derived scores become durable records shown to users | Recovery, Sleep, Strain, Stress, Energy Bank, readiness composites |
| backfill replay -> EMA baseline chain | Late historical inserts can alter downstream baseline state | Baseline values and sample counts |
| worker runtime -> persistence layer | Background jobs can fail mid-pipeline and create partial writes if not controlled | Ingest output + computed DailyMetrics upsert |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-03-01 | T | RecoveryEngine/SleepEngine input mapping | mitigate | Input clamp and explicit missing-value handling in `RecoveryEngine.compute` and `SleepEngine.compute`; validated by `RecoveryEngineTest` and `SleepEngineTest`. | closed |
| T-03-02 | D | RecoveryEngine/SleepEngine sparse-day path | mitigate | Null-safe renormalization returns bounded score/confidence instead of crash; verified in missing-input and all-null tests. | closed |
| T-03-03 | R | Confidence-visible scoring policy | mitigate | Confidence emitted as parallel signal, score still persisted/visible; validated by engine tests and Phase 03 UAT (`03-UAT.md`). | closed |
| T-03-04 | T | StrainEngine/StressEngine scaling | mitigate | Non-linear scaling with strict bounds (`coerceIn`) and extreme-value tests in `StrainEngineTest` and `StressEngineTest`. | closed |
| T-03-05 | I | EnergyBankEngine internal state | mitigate | Visible score and internal balance remain distinct with deterministic update rules; verified by `EnergyBankEngineTest`. | closed |
| T-03-06 | R | Confidence-vs-visibility policy | mitigate | Sparse inputs degrade confidence but do not suppress score output; verified in `StressEngineTest` and orchestrator tests. | closed |
| T-03-07 | T | BaselineRecalculatorUseCase sequencing | mitigate | Sequential day-by-day EMA recomputation prevents state skew; verified by `BaselineRecalculatorUseCaseTest` and deterministic `EmaEngineTest`. | closed |
| T-03-08 | D | ComputeDailyScoresUseCase sparse-data path | mitigate | Null-safe aggregation with bounded derived outputs and confidence fallback; verified by `ComputeDailyScoresUseCaseTest`. | closed |
| T-03-09 | R | HealthSyncWorker ingest->compute pipeline | mitigate | Deterministic retry/failure semantics and explicit ingest->compute ordering in `HealthSyncWorker`; schedule behavior covered by `HealthSyncWorkerScheduleTest`. | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

No accepted risks.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-04-15 | 9 | 9 | 0 | GitHub Copilot |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-04-15