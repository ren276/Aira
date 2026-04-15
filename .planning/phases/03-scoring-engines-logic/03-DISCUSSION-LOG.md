# Phase 3: Scoring Engines & Logic - Discussion Log

> Audit trail only. Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-04-15
**Phase:** 3-Scoring Engines & Logic
**Areas discussed:** Recovery + Sleep, Strain + Stress + Energy Bank, EMA baselines + cold start, Extra DailyMetrics outputs, Confidence + low-data handling

---

## Recovery + Sleep

| Option                                       | Description                                                          | Selected |
| -------------------------------------------- | -------------------------------------------------------------------- | -------- |
| Show the score anyway, with lower confidence | Use the available signals and keep the score visible.                | ✓        |
| Hide the score until inputs are complete     | Show a placeholder instead of a partially inferred number.           |          |
| Use a fallback estimate                      | Fill the gap with the best available proxy and mark it as estimated. |          |

**User's choice:** Show the score anyway, with lower confidence
**Notes:** User preferred visible scores over suppression when inputs are incomplete.

## Strain + Stress + Energy Bank

| Option              | Description                                                                | Selected |
| ------------------- | -------------------------------------------------------------------------- | -------- |
| Public score        | Show Energy Bank directly as one of the user-facing outputs.               |          |
| Internal state only | Use it to inform other scores, but do not surface it directly.             |          |
| Hybrid              | Show a public score and keep a separate internal depletion/recharge state. | ✓        |

**User's choice:** Hybrid
**Notes:** Energy Bank should remain visible and also support internal state transitions.

## EMA baselines + cold start

| Option                  | Description                                                            | Selected |
| ----------------------- | ---------------------------------------------------------------------- | -------- |
| Core input metrics only | HRV, RHR, sleep duration, sleep efficiency, and similar engine inputs. |          |
| All scores and inputs   | Give every major score and supporting metric its own baseline.         | ✓        |
| Core now, expand later  | Start with the core metrics and leave the rest for a later phase.      |          |

**User's choice:** All scores and inputs
**Notes:** Baselines should cover the full score surface, not only the smallest set of input signals.

## Extra DailyMetrics outputs

| Option                      | Description                                                                                        | Selected |
| --------------------------- | -------------------------------------------------------------------------------------------------- | -------- |
| Yes, compute all fields now | Treat nutrition, readiness-to-learn, burnout risk, and composite readiness as first-class outputs. | ✓        |
| Only roadmap scores         | Keep the extra fields as placeholders for now.                                                     |          |
| Core plus natural extras    | Compute the extras only where they naturally fall out of the core engines.                         |          |

**User's choice:** Yes, compute all fields now
**Notes:** The user wants the existing DailyMetrics schema fully populated in Phase 3.

## Confidence + low-data handling

| Option                         | Description                                                      | Selected |
| ------------------------------ | ---------------------------------------------------------------- | -------- |
| Always show a score            | Expose confidence separately, but keep the score visible.        | ✓        |
| Suppress low-confidence scores | Hide or replace the score when confidence drops too low.         |          |
| Show provisional scores        | Keep the score visible and label it as provisional or estimated. |          |

**User's choice:** Always show a score
**Notes:** Confidence should inform explanation and trust, not hide the score.

## the agent's Discretion

None.

## Deferred Ideas

None.
