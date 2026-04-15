# Phase 4: User Interface & Dashboards - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves alternatives considered.

**Date:** 2026-04-15
**Phase:** 04-user-interface-dashboards
**Areas discussed:** Theme and visual system, Navigation and tab IA, Home dashboard behavior, Detail + logging flows

---

## Theme and visual system

| Option | Description | Selected |
|--------|-------------|----------|
| Design-first fidelity | Match the Clinical Ghost direction closely even if implementation is heavier. | |
| Balanced fidelity | Preserve identity while using pragmatic Material3 patterns for speed/maintainability. | |
| Foundation-first | Minimal token/theme base now, advanced treatments later. | |

**User's choice:** Hybrid preference expressed as "design first with speed and performance" (design-first priority with selective pragmatic compromises).
**Notes:** User emphasized visual quality and runtime performance as joint top priorities.

| Option | Description | Selected |
|--------|-------------|----------|
| Three explicit palettes | Dedicated Light, Dark, OLED palettes with runtime switching. | |
| Dark+OLED explicit, light derived | Explicit dark/OLED, derive light from shared tokens. | |
| Material dynamic-first | Dynamic color plus minimal brand overrides. | |

**User's choice:** Two-theme model only: Light and OLED-focused Dark.
**Notes:** User explicitly stated dark should be OLED-style contrast treatment.

| Option | Description | Selected |
|--------|-------------|----------|
| Custom typography + purposeful motion now | Adopt design hierarchy and meaningful motion in Phase 4. | ✓ |
| Custom typography now, motion staged | Lock type now, delay richer motion. | |
| Defaults now | Defer custom type/motion polish. | |

**User's choice:** Custom typography + purposeful motion now.
**Notes:** Motion should be meaningful rather than decorative.

---

## Navigation and tab IA

| Option | Description | Selected |
|--------|-------------|----------|
| 5 tabs: Home, Insights, Train, Nutrition, Settings | Direct access to all Phase 4 capabilities. | ✓ |
| 4 tabs: Home, Insights, Journal, Settings | Group Train+Nutrition under Journal. | |
| 3 tabs: Home, Insights, Profile | Minimal nav; Train/Nutrition nested. | |

**User's choice:** 5-tab structure.
**Notes:** User prefers direct top-level access over grouped nesting.

| Option | Description | Selected |
|--------|-------------|----------|
| Full-screen detail + bottom-sheet explanations | Dedicated detail routes with in-detail explanation sheets. | ✓ |
| Home-anchored bottom sheets only | Keep details layered over home. | |
| Hybrid by metric | Mix full-screen and sheet by metric type. | |

**User's choice:** Full-screen detail + bottom-sheet explanations.
**Notes:** Decision optimizes scalability for deep metric interpretation.

| Option | Description | Selected |
|--------|-------------|----------|
| Always land on Home tab | Predictable entry behavior. | |
| Resume last-visited tab | Power-user continuity. | |
| Smart entry | Default home + contextual deep link from reminders/notifications. | ✓ |

**User's choice:** Smart entry.
**Notes:** Supports contextual routes without abandoning default-home model.

---

## Home dashboard behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Fixed clinical order | Recovery, Sleep, Strain, Stress fixed for muscle memory. | ✓ |
| Adaptive order by priority | Reorder cards by daily risk/importance. | |
| User-customizable order | User drag-reorder support. | |

**User's choice:** Fixed clinical order.
**Notes:** Prioritizes consistency over adaptive complexity.

| Option | Description | Selected |
|--------|-------------|----------|
| Show cached immediately + silent fast-sync | Local-first render, background refresh, confidence and recency cues. | ✓ |
| Blocking initial sync | Wait for fresh sync before rendering. | |
| Manual refresh first | Refresh only on pull gesture. | |

**User's choice:** Cached immediately + silent fast-sync.
**Notes:** Aligns with existing local-first architecture and foreground fast-sync behavior.

| Option | Description | Selected |
|--------|-------------|----------|
| Always-present smart card | Card always visible, fallback to preventive insight when no anomaly. | ✓ |
| Only show on anomaly | Hidden on normal days. | |
| Collapsed by default | Teaser card expands on tap. | |

**User's choice:** Always-present smart card.
**Notes:** Card is expected to remain a continuous guidance surface.

---

## Detail + logging flows

| Option | Description | Selected |
|--------|-------------|----------|
| Full detail from day one | Trend windows, factor breakdowns, confidence + guidance in v1. | ✓ |
| Core detail first | Start compact and defer deeper controls. | |
| Minimal detail | Mostly informational surface. | |

**User's choice:** Full detail from day one.
**Notes:** User prefers complete metric interpretability in Phase 4.

| Option | Description | Selected |
|--------|-------------|----------|
| Standardized multi-stop sheet | What changed / Why it matters / What to do next structure. | ✓ |
| Simple per-factor sheet | Independent short sheets per factor. | |
| Inline expanders | Avoid modal sheets; expand inline in detail. | |

**User's choice:** Standardized multi-stop sheet.
**Notes:** This pattern should be consistent across metric details.

| Option | Description | Selected |
|--------|-------------|----------|
| Quick-add first + optional deep edit | Fast logging with optional detailed edit path. | ✓ |
| Detailed form first | Full structured form every time. | |
| Conversational capture | Guided conversational logging flow. | |

**User's choice:** Quick-add first + optional deep edit.
**Notes:** Local-first save remains expected.

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, full edit/delete history | Allow full correction/removal from history views. | ✓ |
| Edit only, no delete | Corrections allowed, deletion blocked. | |
| No editing in v1 | Create-only flow this phase. | |

**User's choice:** Full edit/delete history.
**Notes:** Lifecycle tooling must be available in initial logger implementation.

---

## the agent's Discretion

- Motion curve tuning and exact animation durations.
- Token naming hierarchy in Compose theme code.
- Chart/rendering implementation details that preserve selected behavior.

## Deferred Ideas

None — no out-of-scope capability proposals were introduced in this discussion.
