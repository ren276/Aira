---
phase: 08
slug: causal-insight-and-personalization-core
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-18
---

# Phase 08 - Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Local metrics and corrections -> causal/personalization engines | Health-derived inputs cross into ranking and adaptation calculations. | Aggregated daily metrics, correction payloads (sensitive local health context) |
| Domain engines -> Room persistence | Computed explainability and personalization states are materialized to local DB. | Top-3 factors, confidence metadata, bounded personalization parameters |
| Persisted state -> UI rendering | Explainability and correction outcomes become user-visible decisions. | Aggregated factors/weights, recency windows, correction receipts |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-08-01 | T | CausalRankingEngine input windows | mitigate | Date-window and range validation before scoring; malformed/empty evidence rejected. Evidence: `CausalRankingEngine`, `CausalRankingEngineTest`. | closed |
| T-08-02 | R | ComputeCausalInsightsUseCase | mitigate | Ranking timestamp/source window labels persisted for local reproducibility. Evidence: `ComputeCausalInsightsUseCase`, tests. | closed |
| T-08-03 | I | CausalInsight persistence | mitigate | Persist only aggregated factors/weights, not raw samples. Evidence: `CausalInsight` model + DAO schema. | closed |
| T-08-04 | D | Daily scoring integration | mitigate | Fixed windows (24h/72h/7d) and bounded factor candidate set before sort. Evidence: plan implementation and unit tests. | closed |
| T-08-05 | E | Database surface | mitigate | Writes constrained to domain use cases/DAO entrypoints; no raw export endpoint introduced. Evidence: use-case wiring in scoring pipeline. | closed |
| T-08-06 | T | CorrectionInfluenceEngine | mitigate | Correction records validated by target/range and malformed records skipped with explicit reason. Evidence: `CorrectionInfluenceEngine`, tests. | closed |
| T-08-07 | D | PersonalizationUpdateEngine | mitigate | Min-7-day gate and +/-3% daily clamp prevent unstable update oscillation. Evidence: `PersonalizationUpdateEngine`, tests. | closed |
| T-08-08 | R | UpdatePersonalizationStateUseCase | mitigate | Persist applied/skipped decisions and cap metadata for auditability. Evidence: personalization state persistence and use-case tests. | closed |
| T-08-09 | I | PersonalizationState tables | mitigate | Store compact computed parameters only; no raw biometric event persistence added. Evidence: Room entities for personalization/influence. | closed |
| T-08-10 | E | DAO update path | mitigate | Mutation path restricted to dedicated use case + DAO, no external mutation API. Evidence: `UpdatePersonalizationStateUseCase` integration. | closed |
| T-08-11 | S | Confidence/recency presentation | mitigate | Confidence tiers mapped from persisted thresholds/window labels, not arbitrary UI overrides. Evidence: `MetricDetailViewModel`, tests. | closed |
| T-08-12 | T | Correction submission flow | mitigate | Explicit confirmation gate required before apply; target/value validation enforced. Evidence: `DataCorrectionsViewModel`, UAT test 4 pass. | closed |
| T-08-13 | R | Correction auditability | mitigate | Correction type/timestamp/influence metadata persisted and surfaced as result state. Evidence: correction use case + viewmodel state flow. | closed |
| T-08-14 | I | Explainability detail surfaces | mitigate | UI renders aggregated factors/weights/recency windows only, no raw sample identifiers. Evidence: detail screen contracts and factor components. | closed |
| T-08-15 | D | Instrumentation reliability | mitigate | Deterministic local-fake test path and no-network UI verification flow; unit/UI suites compile and execute where environment permits. Evidence: `ExplainabilityUiTest` implementation + unit suite pass. | closed |

*Status: open, closed*
*Disposition: mitigate (implementation required), accept (documented risk), transfer (third-party)*

---

## Accepted Risks Log

No accepted risks.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-04-18 | 15 | 15 | 0 | GitHub Copilot |

---

## Sign-Off

- [x] All threats have a disposition (mitigate, accept, transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-04-18
