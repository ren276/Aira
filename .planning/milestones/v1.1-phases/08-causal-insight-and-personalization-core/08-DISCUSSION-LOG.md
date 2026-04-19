# Phase 8: Causal Insight and Personalization Core - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-04-18
**Phase:** 08-causal-insight-and-personalization-core
**Areas discussed:** Causal factor model, Confidence and recency rules, Personalization update policy, User correction behavior

---

## Causal Factor Model

| Option | Description | Selected |
|--------|-------------|----------|
| Top 3 weighted factors | Show top 3 contributors with contribution percentages and directional impact | ✓ |
| Top 5 weighted factors | Show deeper breakdown with smaller factors included | |
| Primary + secondary | Show 1 main driver and up to 2 supporting drivers | |
| the agent decides | Let implementation choose the best presentation | |

**User's choice:** Top 3 weighted factors
**Notes:** Prioritized concise, high-signal card output.

---

## Confidence and Recency Rules

| Option | Description | Selected |
|--------|-------------|----------|
| Tiered confidence + exact recency | Confidence buckets plus explicit window text (for example, last 7d) | ✓ |
| Numeric confidence only | Single 0-100 confidence score without bucket labels | |
| Recency only | Show freshness window without confidence score | |
| the agent decides | Let implementation choose the output format | |

**User's choice:** Tiered confidence + exact recency
**Notes:** Follow-up threshold decision selected: High >= 0.75, Medium 0.40-0.74, Low < 0.40.

---

## Personalization Update Policy

| Option | Description | Selected |
|--------|-------------|----------|
| Daily bounded EMA updates | Update once per day with bounded deltas and minimum data thresholds | ✓ |
| Event-driven updates | Update after each significant workout/sleep event | |
| Weekly batch recalibration | Recompute parameters once per week for stability | |
| the agent decides | Let implementation choose the update policy | |

**User's choice:** Daily bounded EMA updates
**Notes:** Follow-up guardrails selected: minimum 7 days data and max +/-3% daily parameter change.

---

## User Correction Behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Weighted decay over 14 days | Corrections are strongest early, then decay while still informing the model | ✓ |
| Persistent until replaced | Correction remains active until explicitly changed by user | |
| One-time hint only | Use correction only for next cycle | |
| the agent decides | Let implementation choose correction influence | |

**User's choice:** Weighted decay over 14 days
**Notes:** Follow-up cap selected: correction influence capped at 20%.

---

## the agent's Discretion

- Tie-break behavior for near-equal causal factor weights.
- Final wording for confidence and recency microcopy.
- Internal storage schema details for correction-decay state.

## Deferred Ideas

- None.
