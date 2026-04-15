---
phase: 04
slug: user-interface-dashboards
status: draft
shadcn_initialized: false
preset: none
created: 2026-04-15
---

# Phase 04 - UI Design Contract

Visual and interaction contract for Phase 04 only, covering UI-01 through UI-06.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (native Jetpack Compose + Material 3) |
| Preset | not applicable |
| Component library | Jetpack Compose Material 3 |
| Icon library | androidx.compose.material:material-icons-extended |
| Font | Manrope primary, SansSerif fallback |

Notes:
- No web UI design system is present in this repository.
- Phase 04 keeps implementation Android-native with Compose tokens and components.

---

## Spacing Scale

Declared values (multiples of 4 only):

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4dp | Inline icon gap, bullet spacing |
| sm | 8dp | Compact spacing in chips and rows |
| md | 16dp | Default card and content spacing |
| lg | 24dp | Section spacing in dashboard/detail screens |
| xl | 32dp | Major vertical rhythm between sections |
| 2xl | 48dp | Distinct section break spacing |
| 3xl | 64dp | Page-level top/bottom breathing space |

Exceptions:
- Minimum touch target: 44dp for icon-only and compact actions.

---

## Typography

Exactly 4 text sizes and 2 weights are allowed for Phase 04.

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Body | 16sp | 400 | 1.5 |
| Label | 14sp | 600 | 1.4 |
| Heading | 20sp | 600 | 1.2 |
| Display | 28sp | 600 | 1.2 |

Rules:
- Use weight 400 for descriptive/supporting copy.
- Use weight 600 for hierarchy, score labels, and actionable emphasis.
- Avoid introducing additional sizes or weights in this phase.

---

## Color

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | Light: #F6F8F7, OLED: #131318 | Root backgrounds and primary surfaces |
| Secondary (30%) | Light: #E7ECEB, OLED: #1F1F25 | Cards, tabs, secondary panels |
| Accent (10%) | #47EAED | Score-ring active arcs, selected tab indicator, primary CTA fill, live pulse dot, chart highlight |
| Destructive | #FFC9B7 | Delete actions and destructive confirmation emphasis only |

Accent reserved for:
- Active score-ring segment highlights
- Selected bottom-tab indicator
- Primary action button fill
- Live data pulse status dot
- Detail chart current-point highlight

Additional semantic:
- Caution color: #FFE2AB for watch-state warnings and forecast caution chips.

Theme constraints:
- Ship Light and OLED-style Dark only.
- No separate non-OLED dark variant in Phase 04.

---

## Copywriting Contract

| Element | Copy |
|---------|------|
| Primary CTA | Save Entry |
| Empty state heading | No health data yet |
| Empty state body | Grant health permissions and run sync. Recovery, Sleep, Strain, and Stress will populate after data ingestion completes. |
| Error state | Sync paused. Pull to retry, or open Settings > Data Sources to re-grant access. |
| Destructive confirmation | Delete workout entry: Remove this session from your history? This action cannot be undone. |
| Destructive confirmation | Delete nutrition entry: Remove this meal log permanently? This action cannot be undone. |

---

## Interaction Contract (Phase 04 Scope)

Navigation and information architecture:
- Use a fixed 5-tab layout: Home, Insights, Train, Nutrition, Settings.
- Support smart launch routing to relevant tabs for reminders/notifications; default launch remains Home.

Home dashboard behavior:
- Maintain fixed 2x2 card order: Recovery, Sleep, Strain, Stress.
- Render local cached state immediately, then run silent foreground fast-sync, then animate score deltas.
- Always show confidence and last-updated context with visible scores.
- Keep Causal Anomaly card always visible; if no anomaly exists, show preventive forecast guidance.

Detail screen behavior:
- Metric cards open full-screen detail routes.
- Explanation content uses bottom sheets with exactly three sections:
  - What changed
  - Why it matters
  - What to do next
- Detail screens for Recovery, Strain, Sleep, and Stress include trend windows, factor breakdowns, confidence explanation, and action guidance.

Train and Nutrition flows:
- Quick-add first interaction model; optional deep-edit screen for richer input.
- Historical entries must support edit and delete.
- Destructive actions require explicit confirmation dialog copy from this contract.

Performance and motion:
- Design-first visual fidelity is required, with performance guardrails.
- Animate score ring transitions and score deltas after sync completion.
- Avoid blocking spinners for foreground refresh when cached data is present.

---

## Requirement Coverage (UI-01 to UI-06)

| Requirement | Contract Coverage |
|-------------|-------------------|
| UI-01 | Light + OLED themes, custom canvas score arcs, accent/motion rules |
| UI-02 | Home 2x2 order, causal insight card always present, local-first refresh behavior |
| UI-03 | Full detail routes + explanation bottom sheets with fixed 3-part structure |
| UI-04 | Live pulse/status and confidence-aware vitals communication patterns |
| UI-05 | Train quick-add plus deep-edit; historical edit/delete |
| UI-06 | Nutrition quick-add plus deep-edit; historical edit/delete |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | none | not required |
| third-party | none | not applicable |

---

## Source Decisions Applied

| Source | Decisions Used |
|--------|---------------|
| 04-CONTEXT.md | D-01 through D-13 |
| 03-CONTEXT.md | Confidence-visible scoring behavior (D-11, D-12 alignment) |
| REQUIREMENTS.md | UI-01 through UI-06 scope boundaries |
| ROADMAP.md | Phase 04 success criteria and plan boundaries |
| designs/aira_intelligence/DESIGN.md | Clinical Ghost direction, OLED contrast, accent semantics, typography intent |
| Existing code | Compose Material3 baseline and Phase 4 MainActivity placeholder |

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

Approval: pending
