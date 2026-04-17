# Phase 05: Runtime Data Wiring & UI Alignment - Context

**Gathered:** 2026-04-16
**Status:** Ready for planning
**Source:** User discussion + codebase survey + existing Phase 04.1 outcomes

<domain>
## Phase Boundary

This phase is a whole-app runtime wiring pass over the current Aira UI.
It must align every existing screen and navigation path to live local data flows, explicit empty states, and semantically correct route behavior.

This phase does not add new product areas. It focuses on closing wiring gaps, removing stale static copy or mock-like defaults, and making the current UI honestly reflect the data that exists on device.

</domain>

<decisions>
## Implementation Decisions

### Phase scope

- The phase covers the whole current app shell, not just the screens that are visibly broken.
- Include Home, Insights/Body/Coach, Train, Nutrition, Settings, Account, Data Confidence, Weekly Report, What-If, Predictions, Corrections, and the navigation and entry flow that ties them together.
- Keep the current design surfaces in scope, but treat runtime data correctness as higher priority than decorative completeness when the two conflict.

### Data rule

- Live repository/Room/domain data wins.
- Static sample values, seeded narration, and mock-like defaults are not acceptable in production screens.
- If real data is missing, the UI must say so explicitly with empty, unavailable, or insufficient-data states.

### Missing-data behavior

- Do not hide major modules just because data is incomplete.
- Do not silently fabricate values to keep a chart or card full.
- Use clear guidance, empty states, disabled controls, or temporary unavailable messaging where needed.

### Navigation rule

- Keep navigation behavior semantically aligned with the visible IA.
- Any legacy aliasing should be treated as temporary migration glue only, not as the long-term contract.
- Route targets, bottom tabs, and deep links must resolve to actual destinations.

### Scanner and deferred capabilities

- Camera-backed scanner work can remain deferred if it is not ready as a dependency, but the UI must clearly communicate when it is unavailable.
- Deferred capability is acceptable only if it is explicit and does not look like a working feature.

### Phase slot

- This is roadmap phase 05.

### the agent's Discretion

- Exact wave grouping and plan decomposition.
- Whether a surface should render an explicit empty state, placeholder guidance, or disabled controls when data is incomplete.
- Exact route compatibility strategy during migration, as long as the final visible IA is correct.

</decisions>

<canonical_refs>

## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Roadmap and project intent

- `.planning/ROADMAP.md` - Phase 05 placement and milestone context.
- `.planning/PROJECT.md` - Product constraints, supported stack, privacy rules, and active requirements.
- `.planning/codebase/CONCERNS.md` - Current runtime wiring gaps, fragile areas, and the recommended phase boundary.

### Prior phase context

- `.planning/phases/04.1-design-faithful-ui-implementation-all-18-screens-pixel-perfe/04.1-CONTEXT.md` - Locked fidelity/runtime decisions that Phase 05 must not reopen.
- `.planning/phases/04-user-interface-dashboards/04-UAT.md` - Existing UAT scenarios and evidence baseline.

### Runtime wiring surfaces

- `app/src/main/java/com/aira/health/presentation/navigation/AiraNavHost.kt` - Shell routes and destination registration.
- `app/src/main/java/com/aira/health/presentation/navigation/AiraRoutes.kt` - Route contract and legacy aliases.
- `app/src/main/java/com/aira/health/presentation/navigation/AppEntryRoute.kt` - Entry orchestration and onboarding gate.
- `app/src/main/java/com/aira/health/presentation/navigation/DeepLinkRouter.kt` - Deep-link normalization and fallback behavior.
- `app/src/main/java/com/aira/health/presentation/dashboard/home/HomeViewModel.kt` - Home data aggregation and refresh behavior.
- `app/src/main/java/com/aira/health/presentation/dashboard/details/MetricDetailRoute.kt` - Detail semantic dispatch.
- `app/src/main/java/com/aira/health/presentation/train/TrainViewModel.kt` - Train quick-add and history binding.
- `app/src/main/java/com/aira/health/presentation/nutrition/NutritionViewModel.kt` - Nutrition quick-add, scanner draft, and list binding.
- `app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt` - Runtime profile, sync, and local-model state.
- `app/src/main/java/com/aira/health/presentation/supplementary/InsightsPredictionsViewModel.kt` - Predictions from recent metrics.
- `app/src/main/java/com/aira/health/presentation/supplementary/DataCorrectionsViewModel.kt` - Correction timeline and baseline state.
- `app/src/main/java/com/aira/health/presentation/supplementary/WhatIfViewModel.kt` - Simulation state and insufficient-data handling.
- `app/src/main/java/com/aira/health/presentation/supplementary/WeeklyReportViewModel.kt` - Weekly rollup and trend state.
- `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt` - Sync and score refresh chain feeding UI freshness.
- `app/src/main/java/com/aira/health/data/local/dao/DailyMetricsDao.kt` - Local daily metrics source of truth.
- `app/src/main/java/com/aira/health/data/local/dao/NutritionLogDao.kt` - Nutrition history source of truth.
- `app/src/main/java/com/aira/health/data/local/dao/WorkoutSessionDao.kt` - Workout history source of truth.

### Design references

- `designs/aira_intelligence/DESIGN.md` - Core visual language.
- `designs/home_dashboard_oled/code.html` - Home dashboard target.
- `designs/recovery_intelligence/code.html` - Recovery detail target.
- `designs/strain_detail_oled/code.html` - Strain detail target.
- `designs/stress_detail_oled/code.html` - Stress detail target.
- `designs/insights_predictions_oled/code.html` - Predictions target.
- `designs/data_corrections_oled/code.html` - Data corrections target.
- `designs/what_if_simulator_light/code.html` - What-if target.
- `designs/weekly_report_light/code.html` - Weekly report target.
- `designs/weekly_report_oled/code.html` - Weekly report target.
- `designs/settings_privacy_1/code.html` - Settings target.
- `designs/settings_privacy_2/code.html` - Settings/privacy target.
- `designs/data_confidence_light/code.html` - Data confidence target.
- `designs/train_fitness/code.html` - Train target.
- `designs/nutrition_oled/code.html` - Nutrition target.
- `designs/gemma_3_intelligence/code.html` - Coach target.
- `designs/onboarding_wearables_light/code.html` - Onboarding target.

</canonical_refs>

<code_context>

## Existing Code Insights

### Reusable assets

- The core Room and repository wiring already exists for Home, detail, Train, Nutrition, settings, and supplementary surfaces.
- Navigation Compose is already established, so this phase should correct route semantics rather than invent a new nav architecture.
- The sync worker chain already produces local metrics that UI can consume if the viewmodels bind correctly.

### Established patterns

- Screen state should continue to come from `StateFlow`/ViewModel, not hardcoded composable state.
- Local-first rendering is the baseline: cached Room values first, then sync/update.
- Empty or unavailable states are better than fabricated values.

### Integration points

- `MainActivity` and app entry routing control the first visible surface.
- `AiraNavHost` is the main cross-screen wiring point.
- `HomeViewModel`, `MetricDetailRoute`, `TrainViewModel`, `NutritionViewModel`, and the supplementary viewmodels are the main runtime-state contracts to align.

</code_context>

<specifics>
## Specific Ideas

- Surface freshness and confidence where the state already contains them, especially on Home and Settings.
- Treat route aliases, fallback constants, and heuristic copy as migration or placeholder concerns that need explicit handling.
- Keep Body and Coach aligned to real local data, but do not overstate them as independent AI systems if they are only heuristics over Home state.
- Preserve scanner-unavailable behavior only if it is explicit to the user.

</specifics>

<deferred>
## Deferred Ideas

- New product capabilities beyond the current screen set.
- Full camera-backed scanner implementation if the dependency is not ready for this phase.
- Cloud-side feature expansion or new AI capability work.

</deferred>

---

_Phase: 05-runtime-data-wiring-ui-alignment_
_Context gathered: 2026-04-16_
