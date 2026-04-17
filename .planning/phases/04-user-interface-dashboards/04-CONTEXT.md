# Phase 4: User Interface & Dashboards - Context

**Gathered:** 2026-04-15
**Status:** Ready for planning

<domain>
## Phase Boundary

Surface the calculated health insights through high-performance Jetpack Compose screens that match the design direction, including global theming, animated score-ring atoms, the home dashboard, metric detail experiences with explanation sheets, and baseline Train/Nutrition logging flows.

This phase clarifies how to implement UI-01 through UI-06 inside existing scope. It does not add new platform capabilities.

</domain>

<decisions>
## Implementation Decisions

### Theme and Visual System
- **D-01:** Implementation should be design-first (high fidelity to provided visual direction) while still preserving runtime speed/performance guardrails.
- **D-02:** Ship two theme modes for this phase: Light and OLED-style Dark (high-contrast dark palette). No separate non-OLED dark variant.
- **D-03:** Adopt custom typography hierarchy and purposeful motion from the start of Phase 4 (not deferred).

### Navigation and Tab IA
- **D-04:** Use a 5-tab structure for v1: Home, Insights, Train, Nutrition, Settings.
- **D-05:** Metric cards open full-screen detail routes; explanation content is presented as bottom sheets within detail screens.
- **D-06:** Use smart launch routing: default-home behavior with notification/reminder deep links to relevant tabs when applicable.

### Home Dashboard Behavior
- **D-07:** Keep a fixed clinical 2x2 grid order: Recovery, Sleep, Strain, Stress.
- **D-08:** Render cached local state immediately, run silent foreground fast-sync, then animate score deltas; always show confidence and last-updated context.
- **D-09:** Keep Causal Anomaly card always present; when no anomaly is detected, show preventative/forecast guidance instead of hiding the card.

### Detail and Logging Flows
- **D-10:** Ship full detail depth in v1 (trend windows, factor breakdowns, confidence explanation, and action guidance) for Recovery/Strain/Sleep/Stress.
- **D-11:** Standardize explanation sheets as a 3-part structure: What changed, Why it matters, What to do next.
- **D-12:** Train and Nutrition logging should be quick-add first with optional deep-edit screens.
- **D-13:** Historical Train/Nutrition entries support full edit and delete in this phase.

### the agent's Discretion
- Exact motion timing curves and easing profiles.
- Exact token naming scheme for Compose theme/color/typography classes.
- Exact chart implementation library and rendering primitives, as long as behavior matches decided UX.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements
- `.planning/ROADMAP.md` - Phase 4 goal, dependencies, and success criteria.
- `.planning/REQUIREMENTS.md` - UI-01 through UI-06 requirements.
- `.planning/PROJECT.md` - Product constraints and non-negotiables (privacy-first, native Android, local-first behavior).

### Prior phase decisions that constrain UI behavior
- `.planning/phases/01-environment-persistence/01-CONTEXT.md` - Local-first persistence, security posture, flavor boundaries.
- `.planning/phases/02-data-ingestion/02-CONTEXT.md` - Foreground fast-sync expectation and ingestion confidence assumptions.
- `.planning/phases/03-scoring-engines-logic/03-CONTEXT.md` - Confidence behavior and scoring visibility rules that UI must respect.

### Design references
- `designs/aira_intelligence/DESIGN.md` - Clinical Ghost visual direction and design system rules.
- `designs/home_dashboard_oled/code.html` - Home dashboard composition direction.
- `designs/insights_predictions_oled/code.html` - Insights and predictive card patterns.
- `designs/recovery_intelligence/code.html` - Recovery intelligence visual language.
- `designs/strain_detail_oled/code.html` - Strain detail interaction reference.
- `designs/stress_detail_oled/code.html` - Stress detail interaction reference.
- `designs/data_confidence_light/code.html` - Confidence/state communication cues.
- `designs/train_fitness/code.html` - Strength Builder interaction direction.
- `designs/nutrition_oled/code.html` - Nutrition logging layout and hierarchy.

### Existing code anchors
- `app/src/main/java/com/aira/health/MainActivity.kt` - Compose host and Phase 4 nav/theme placeholder.
- `app/src/main/java/com/aira/health/presentation/onboarding/PermissionBatchScreen.kt` - Existing Compose screen patterns and Material usage.
- `app/src/main/res/values/themes.xml` - Current base app theme.
- `app/src/main/res/values-night/themes.xml` - Current night theme base.

### Codebase maps
- `.planning/codebase/ARCHITECTURE.md` - Current app layering and integration points.
- `.planning/codebase/STRUCTURE.md` - Package placement expectations for new presentation code.
- `.planning/codebase/CONVENTIONS.md` - Naming/style conventions and Compose patterns.
- `.planning/codebase/STACK.md` - Runtime/dependency constraints.
- `.planning/codebase/CONCERNS.md` - Known brittle areas that can affect UI reliability.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PermissionBatchScreen` already establishes a Compose + ViewModel pattern with controlled state/event flow.
- `MainActivity` is intentionally prepared for Phase 4 NavHost + theme wiring.
- `DailyMetrics` schema already includes all UI-facing score/confidence fields needed for dashboard and detail surfaces.
- `HealthSyncWorker.scheduleImmediate` provides the immediate-sync hook that supports the selected silent refresh behavior.

### Established Patterns
- UI state is expected from `StateFlow` + ViewModel (not direct mutable composable state for business data).
- Domain/data layers remain separated from Android/Compose concerns.
- Local-first behavior is already established: UI should prioritize local reads then hydrate with background sync updates.
- Confidence is a parallel signal and should not hide score visibility.

### Integration Points
- Theme system and navigation root should be introduced through `MainActivity` and new `presentation` subpackages for theme/nav/features.
- Dashboard and detail screens should consume score/confidence data from Room-backed flows aligned with existing ingestion/scoring pipeline.
- Train/Nutrition logging flows should persist into existing local entities and DAOs with local-first responsiveness.

</code_context>

<specifics>
## Specific Ideas

- User preference: design-first output with speed/performance preserved (not a purely minimal implementation).
- Dark mode should be OLED-leaning high-contrast styling.
- Confidence and last-updated cues should be explicit in home/detail surfaces.
- Causal anomaly card should remain visible even when no active anomaly exists (as proactive guidance).

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope.

</deferred>

---

*Phase: 04-user-interface-dashboards*
*Context gathered: 2026-04-15*
