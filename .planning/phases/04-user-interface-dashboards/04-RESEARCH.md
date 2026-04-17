# Phase 04: User Interface & Dashboards - Research

**Researched:** 2026-04-15
**Domain:** Android Jetpack Compose dashboard architecture and feature UI flows (UI-01..UI-06)
**Confidence:** MEDIUM

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions

Source: [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md]

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

Source: [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md]

- Exact motion timing curves and easing profiles.
- Exact token naming scheme for Compose theme/color/typography classes.
- Exact chart implementation library and rendering primitives, as long as behavior matches decided UX.

### Deferred Ideas (OUT OF SCOPE)

Source: [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md]

None - discussion stayed within phase scope.
</user_constraints>

<phase_requirements>

## Phase Requirements

| ID    | Description                                                                       | Research Support                                                                                                                                                                                                                                                                                                                                                    |
| ----- | --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| UI-01 | Light/Dark/OLED Themes with Custom Canvas Score Arcs                              | Theme token system, dual Theme objects, Canvas drawArc/drawWithCache pattern, animation budget and recomposition controls [VERIFIED: gradle/libs.versions.toml] [CITED: https://developer.android.com/develop/ui/compose/graphics/draw/overview] [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices]                                |
| UI-02 | Home Dashboard with 2x2 Score Grid + Causal Insight Card                          | Cached-first flow wiring from Room, immediate sync trigger, fixed card order and persistent anomaly slot strategy [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/DailyMetricsDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt] [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md]             |
| UI-03 | Detail Screens for Recovery, Strain, Sleep, Stress with explanation bottom sheets | Full-screen route per metric, bottom sheet 3-part template, trend query ranges from DAOs [CITED: https://developer.android.com/develop/ui/compose/components/bottom-sheets] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/SleepSessionDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/DailyMetricsDao.kt]                         |
| UI-04 | Health Monitor real-time vitals strip component                                   | Vitals strip can be built from HrSample + confidence + calculatedAt metadata with lightweight recomposition controls [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/HrSampleDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/model/DailyMetrics.kt] [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices] |
| UI-05 | Basic Strength Builder (manual sets/reps logging)                                 | Existing WorkoutSession entity supports data model but DAO/repository surface is incomplete for CRUD, requiring Wave 0 DB API tasks before UI [VERIFIED: app/src/main/java/com/aira/health/data/local/model/WorkoutSession.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt]                                                          |
| UI-06 | Basic Nutrition Logger (barcode scanner integration + manual fields)              | Existing NutritionLog entity/DAO supports insert/read only; update/delete + scanner integration adapter required before complete UI flow [VERIFIED: app/src/main/java/com/aira/health/data/local/model/NutritionLog.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/NutritionLogDao.kt]                                                             |

</phase_requirements>

## Project Constraints (from copilot-instructions.md)

- Use GSD workflows when user explicitly invokes gsd-\* flows; this phase already follows that path. [VERIFIED: .github/copilot-instructions.md]
- Do not apply GSD workflows unless explicitly requested by the user. [VERIFIED: .github/copilot-instructions.md]

## Rework Guardrails (Design + Data Integrity)

Phase 04 implementation quality depends on two non-negotiable gates that must be checked before any task is marked done.

### Gate A: Design Source Fidelity

- Root `designs/` directory is the visual source of truth for this phase (`designs/*/code.html` plus `designs/aira_intelligence/DESIGN.md`).
- New or updated Compose screens must map to one of those source designs and preserve the intended hierarchy (hero focus, card density, typography emphasis, and spacing rhythm).
- Placeholder-first layouts are allowed only during active development and must be removed before phase verification.

### Gate B: Real Data Pipeline Binding

- Home, detail, coach, train, and nutrition surfaces must be bound to repository/Room flows and derived domain outputs.
- Hardcoded sample values may be used only in previews or debug-only fixtures, never in runtime screen state.
- Every score or narrative shown to users must be traceable to local pipeline inputs (`DailyMetrics`, feature repositories, or computed trend windows).

### Acceptance Checklist for Future GSD Runs

- Verify that no runtime composable in Phase 04 renders static mock metrics as production data.
- Verify confidence and last-updated context are visible on primary score surfaces.
- Verify metric cards and quick actions navigate to real routes (no placeholder destination strings).
- Verify onboarding gate is respected before main shell navigation.

## Summary

Phase 04 should be planned as a UI shell + data binding phase, not as new data-engine work. The backend/domain side already exposes enough baseline health surfaces for Home and metric detail screens through Room-backed models (`DailyMetrics`, `SleepSession`, `HrSample`) and a foreground immediate sync hook (`HealthSyncWorker.scheduleImmediate`). [VERIFIED: app/src/main/java/com/aira/health/data/local/model/DailyMetrics.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/SleepSessionDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/HrSampleDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt]

The highest planning risk is not Compose capability. It is capability mismatch in local data APIs for UI-05/UI-06: `WorkoutSession` exists as an entity but no DAO is currently present in source, and `NutritionLogDao` lacks update/delete methods required by D-13. This means plan tasks must include a small data-surface expansion before Train/Nutrition UI can be complete. [VERIFIED: app/src/main/java/com/aira/health/data/local/model/WorkoutSession.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/NutritionLogDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt]

Design direction is clear and should be implemented with tokenized Compose theming plus custom Canvas rings and restrained motion. Official docs support this path: state hoisting in ViewModel/state holders, Navigation Compose with argument IDs rather than object passing, Material3 `ModalBottomSheet`, and draw/performance best practices for custom graphics. [VERIFIED: .planning/phases/04-user-interface-dashboards/04-UI-SPEC.md] [CITED: https://developer.android.com/develop/ui/compose/state-hoisting] [CITED: https://developer.android.com/develop/ui/compose/navigation] [CITED: https://developer.android.com/develop/ui/compose/components/bottom-sheets] [CITED: https://developer.android.com/develop/ui/compose/graphics/draw/overview] [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices]

**Primary recommendation:** Plan Phase 04 in four executable waves: app shell/theme/nav, dashboard/detail surfaces, Train/Nutrition CRUD data API completion, then polish/performance/testing gates. [VERIFIED: .planning/ROADMAP.md] [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md]

## Architectural Responsibility Map

| Capability                                             | Primary Tier                           | Secondary Tier                      | Rationale                                                                                                                                                                                                                                                                                                                        |
| ------------------------------------------------------ | -------------------------------------- | ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Theme tokens and OLED/Light rendering (UI-01)          | Android client presentation (Compose)  | Android resources                   | Material3 and Compose theme definitions are client-side concerns mapped through `setContent` and resource overlays. [VERIFIED: app/src/main/java/com/aira/health/MainActivity.kt] [VERIFIED: app/src/main/res/values/themes.xml]                                                                                                 |
| Dashboard composition, 2x2 order, anomaly card (UI-02) | Android client presentation            | Local database                      | Layout/order rules are UI-tier logic; values come from Room flows. [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/DailyMetricsDao.kt]                                                                                                       |
| Detail routes and explanation sheets (UI-03)           | Android client presentation/navigation | Local database                      | Route control and sheet presentation are UI concerns; historical metrics come from DAOs. [CITED: https://developer.android.com/develop/ui/compose/navigation] [CITED: https://developer.android.com/develop/ui/compose/components/bottom-sheets] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/SleepSessionDao.kt] |
| Vitals strip real-time rendering (UI-04)               | Android client presentation            | Local database + Worker sync        | Strip is visual logic, fed by local samples refreshed by worker/sync. [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/HrSampleDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt]                                                                                                  |
| Strength quick-add + deep-edit (UI-05)                 | Android client presentation/forms      | Local database                      | Form UX belongs in presentation; persistence requires DAO/repository surface completion. [VERIFIED: app/src/main/java/com/aira/health/data/local/model/WorkoutSession.kt]                                                                                                                                                        |
| Nutrition quick-add + barcode/manual deep-edit (UI-06) | Android client presentation/forms      | Local database + scanner dependency | Input orchestration is presentation, while persisted entries and scanner output mapping are data concerns. [VERIFIED: app/src/main/java/com/aira/health/data/local/model/NutritionLog.kt] [ASSUMED]                                                                                                                              |
| Smart launch routing and deep links (D-06)             | Android client navigation              | Notification intent layer           | Destination selection and deep-link handling are owned by nav graph and launcher intent parsing. [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md] [CITED: https://developer.android.com/develop/ui/compose/navigation]                                                                                    |

## Standard Stack

### Core

| Library                     | Version                   | Purpose                                                        | Why Standard                                                                                                                                                                                                                                                                                    |
| --------------------------- | ------------------------- | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Jetpack Compose BOM         | 2024.10.01                | UI toolkit baseline for all Phase 04 composables               | Already configured in project and aligned with Material3 setup. [VERIFIED: gradle/libs.versions.toml]                                                                                                                                                                                           |
| Material3 Compose           | via BOM                   | Theming, Scaffold, NavigationBar, ModalBottomSheet             | Matches UI-SPEC contract and official bottom-sheet/component guidance. [VERIFIED: gradle/libs.versions.toml] [CITED: https://developer.android.com/develop/ui/compose/components/bottom-sheets]                                                                                                 |
| Navigation Compose          | 2.8.4 (project)           | Tab graph + detail routes + deep-link entry points             | Existing dependency already present; official docs recommend NavHost/NavController and ID-only argument passing. [VERIFIED: gradle/libs.versions.toml] [CITED: https://developer.android.com/develop/ui/compose/navigation]                                                                     |
| Lifecycle ViewModel Compose | 2.8.7 (project lifecycle) | ViewModel-owned screen state + lifecycle-safe state collection | Aligns with existing StateFlow/ViewModel conventions and state-hoisting guidance. [VERIFIED: gradle/libs.versions.toml] [VERIFIED: app/src/main/java/com/aira/health/presentation/onboarding/PermissionBatchScreen.kt] [CITED: https://developer.android.com/develop/ui/compose/state-hoisting] |

### Supporting

| Library                    | Version                | Purpose                                               | When to Use                                                                                                                                                                    |
| -------------------------- | ---------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Material Icons Extended    | via Compose dependency | Tab and dashboard iconography                         | Use for v1 icon set before any custom icon pack. [VERIFIED: gradle/libs.versions.toml]                                                                                         |
| Coil Compose               | 2.7.0                  | Async image rendering in cards/logs/profile surfaces  | Use for avatar/meal images if Phase 04 UI includes remote/local images. [VERIFIED: gradle/libs.versions.toml]                                                                  |
| Room + Flow DAOs           | 2.6.1                  | Local-first data streams for dashboard/detail/logging | Use as single source of truth for first paint and edits. [VERIFIED: gradle/libs.versions.toml] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/DailyMetricsDao.kt] |
| WorkManager immediate work | 2.9.1                  | Silent foreground fast-sync trigger from UI           | Use to satisfy D-08 without blocking first paint. [VERIFIED: gradle/libs.versions.toml] [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt]          |

### Alternatives Considered

| Instead of                                 | Could Use                            | Tradeoff                                                                                                                                                                                                                                          |
| ------------------------------------------ | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Custom chart library for all visuals       | Pure Canvas + DrawScope first        | Better control and less dependency churn for Phase 04, but more custom code for complex trend charts. [CITED: https://developer.android.com/develop/ui/compose/graphics/draw/overview] [ASSUMED]                                                  |
| Navigation Compose 2.8.4 (current project) | Navigation Compose 2.9.7 docs sample | 2.9.7 appears in official setup docs, but upgrading inside this phase adds dependency risk beyond UI scope unless needed by a blocker. [VERIFIED: gradle/libs.versions.toml] [CITED: https://developer.android.com/develop/ui/compose/navigation] |
| Build custom barcode decoder               | ML Kit barcode scanner integration   | Custom decoder is high-risk and unnecessary; use proven scanner SDK path. [ASSUMED]                                                                                                                                                               |

**Installation:**

```kotlin
// Already present in app/build.gradle.kts
implementation(platform(libs.compose.bom))
implementation(libs.compose.material3)
implementation(libs.compose.navigation)
implementation(libs.compose.material.icons.extended)
implementation(libs.compose.viewmodel)
implementation(libs.coil)
```

**Version verification:**

- Project-pinned versions are verified in `gradle/libs.versions.toml`. [VERIFIED: gradle/libs.versions.toml]
- Official Compose/navigation docs referenced in this research show updates dated 2026-03-30 UTC. [CITED: https://developer.android.com/develop/ui/compose/navigation] [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices]

## Architecture Patterns

### System Architecture Diagram

```text
App Launch / Deep Link Intent
        |
        v
MainActivity (setContent + FLAG_SECURE policy)
        |
        v
AppTheme (Light or OLED) + Root NavHost (5 tabs)
        |
        +------------------------------+
        |                              |
        v                              v
Home Dashboard Tab                Detail / Train / Nutrition Tabs
        |                              |
        | read cached Flow             | read/write via feature ViewModels
        v                              v
Room DAOs (DailyMetrics, Sleep, Hr, Nutrition, Workout)
        |
        | immediate silent refresh trigger
        v
HealthSyncWorker.scheduleImmediate()
        |
        v
Ingest + Compute use cases update Room
        |
        v
Flow emissions -> UI delta animations + confidence/last-updated
```

### Recommended Project Structure

```text
app/src/main/java/com/aira/health/presentation/
├── theme/                    # Color/typography/shape tokens + light/oled themes
├── navigation/               # Root app routes, tab graph, deep-link mapping
├── common/
│   ├── components/           # Score rings, confidence chips, anomaly card, app bars
│   └── model/                # UI-only state wrappers
├── dashboard/
│   ├── home/                 # Home screen + viewmodel + dashboard state mapper
│   └── details/              # Recovery/Strain/Sleep/Stress screens + bottom sheet
├── train/                    # Quick-add, deep-edit, history list
└── nutrition/                # Quick-add, deep-edit, barcode/manual entry
```

### Pattern 1: App Shell State Holder + Nav Callbacks

**What:** Keep `NavController` in root shell, pass callbacks and typed IDs into screens. [CITED: https://developer.android.com/develop/ui/compose/navigation]
**When to use:** All tab and detail destinations.
**Example:**

```kotlin
@Composable
fun AiraApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenDetail = { metricId -> navController.navigate("detail/$metricId") },
                onOpenTrain = { navController.navigate("train") }
            )
        }
        composable("detail/{metricId}") { backStack ->
            val metricId = backStack.arguments?.getString("metricId") ?: return@composable
            MetricDetailRoute(metricId = metricId)
        }
    }
}
// Source: [CITED: https://developer.android.com/develop/ui/compose/navigation]
```

### Pattern 2: Cached-First, Silent-Sync, Delta-Animation Pipeline

**What:** Render Room state immediately, trigger `scheduleImmediate`, animate only changed values. [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt]
**When to use:** Home dashboard and detail header values.
**Example:**

```kotlin
class HomeViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val today = dailyMetricsDao.observeByDate(LocalDate.now().toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun refreshSilently() {
        HealthSyncWorker.scheduleImmediate(context)
    }
}
// Source: [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/DailyMetricsDao.kt]
// Source: [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt]
```

### Pattern 3: Modal Bottom Sheet for D-11 Explanation Contract

**What:** One reusable explanation-sheet composable with fixed 3 sections.
**When to use:** Recovery/Strain/Sleep/Stress detail screens.
**Example:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationSheet(
    sections: Triple<String, String, String>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text("What changed")
        Text(sections.first)
        Text("Why it matters")
        Text(sections.second)
        Text("What to do next")
        Text(sections.third)
    }
}
// Source: [CITED: https://developer.android.com/develop/ui/compose/components/bottom-sheets]
```

### Pattern 4: Canvas Rings with drawWithCache

**What:** Draw concentric arcs with `drawArc`; cache expensive geometry with `drawWithCache`.
**When to use:** UI-01 score rings and compact metric ring atoms.
**Example:**

```kotlin
@Composable
fun ScoreRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawWithCache {
            onDrawBehind {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
            }
        }
    )
}
// Source: [CITED: https://developer.android.com/develop/ui/compose/graphics/draw/overview]
```

### Anti-Patterns to Avoid

- **Routing mismatch with locked IA:** Design HTML references Body/Coach tabs, but Phase 04 locked IA is Home/Insights/Train/Nutrition/Settings; do not mirror design tab labels blindly. [VERIFIED: designs/home_dashboard_oled/code.html] [VERIFIED: designs/strain_detail_oled/code.html] [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md]
- **Passing full objects through navigation:** Pass only route IDs and load data from Room/ViewModel. [CITED: https://developer.android.com/develop/ui/compose/navigation]
- **Recomposition-heavy animations:** Avoid reading fast-changing state high in the tree; use `remember`, `derivedStateOf`, and draw/layout-phase lambdas. [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices]
- **Blocking first paint with loading spinners:** D-08 requires immediate cached rendering and silent foreground sync instead of spinner-first UX. [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md]

## Don't Hand-Roll

| Problem                                   | Don't Build                           | Use Instead                                 | Why                                                                                                                                                                                                                                  |
| ----------------------------------------- | ------------------------------------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Navigation state machine                  | Custom backstack and route parser     | Navigation Compose NavHost/NavController    | Handles back stack, deep links, and test hooks with less bug surface. [CITED: https://developer.android.com/develop/ui/compose/navigation]                                                                                           |
| Bottom sheet gestures/physics             | Custom draggable sheet implementation | Material3 `ModalBottomSheet` + `SheetState` | Built-in dismiss/show/hide semantics and coroutine APIs reduce edge-case bugs. [CITED: https://developer.android.com/develop/ui/compose/components/bottom-sheets]                                                                    |
| Arc drawing math cache by hand each frame | Per-frame recompute in composition    | `drawWithCache` + DrawScope arcs            | Better draw-phase efficiency and clearer composable boundaries. [CITED: https://developer.android.com/develop/ui/compose/graphics/draw/overview] [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices] |
| Barcode decoding engine                   | Custom camera frame decoder           | Existing scanner SDK (ML Kit or equivalent) | Decoder reliability and device edge cases are non-trivial; avoid bespoke implementation. [ASSUMED]                                                                                                                                   |

**Key insight:** Most Phase 04 risk is integration and state-flow correctness, not missing UI primitives; use platform primitives and spend effort on data/UI contract alignment. [VERIFIED: app/src/main/java/com/aira/health/presentation/onboarding/PermissionBatchScreen.kt] [CITED: https://developer.android.com/develop/ui/compose/state-hoisting]

## Common Pitfalls

### Pitfall 1: Locked IA Drift vs Design Mockups

**What goes wrong:** Implementation follows mockup tab labels (Body/Coach) instead of locked IA (Insights/Settings), creating behavioral regressions.
**Why it happens:** Design references are inspirational and not always aligned with contextual decisions.
**How to avoid:** Add a nav-graph acceptance checklist tied to D-04 before coding feature screens.
**Warning signs:** Route constants or bottom-nav labels that do not include `Insights` and `Settings`.
Sources: [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md] [VERIFIED: designs/home_dashboard_oled/code.html]

### Pitfall 2: Edit/Delete UX planned without DAO support

**What goes wrong:** Train/Nutrition edit/delete screens are built but cannot persist changes.
**Why it happens:** `NutritionLogDao` currently has insert/read only; no workout DAO exists in source.
**How to avoid:** Wave 0 task to add `WorkoutSessionDao` and update/delete in Nutrition DAO before UI-05/UI-06 implementation tasks.
**Warning signs:** Placeholder button handlers or ViewModel methods without DAO calls.
Sources: [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/NutritionLogDao.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/model/WorkoutSession.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt]

### Pitfall 3: Recomposition spikes in animated dashboard

**What goes wrong:** Ring animation and vitals updates cause dropped frames.
**Why it happens:** Frequent state reads at high composable levels and expensive calculations in composition.
**How to avoid:** Use `remember`, `derivedStateOf`, and draw/layout lambda modifiers for high-frequency values.
**Warning signs:** CPU spikes while scrolling/animating and visible stutter when sync updates land.
Sources: [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices]

### Pitfall 4: Last-updated/confidence omitted under loading transitions

**What goes wrong:** Cached values appear without confidence or timestamp context, violating D-08.
**Why it happens:** UI state model only tracks score primitives.
**How to avoid:** Include confidence + calculatedAt/lastSync fields in every dashboard/detail UI state.
**Warning signs:** Score card preview models without metadata fields.
Sources: [VERIFIED: .planning/phases/04-user-interface-dashboards/04-CONTEXT.md] [VERIFIED: app/src/main/java/com/aira/health/data/local/model/DailyMetrics.kt]

## Code Examples

Verified patterns from official sources and current codebase:

### Lifecycle-safe state collection from ViewModel

```kotlin
@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onRefresh = viewModel::refreshSilently)
}
// Source: [CITED: https://developer.android.com/develop/ui/compose/state-hoisting]
```

### Decoupled navigation callbacks

```kotlin
@Composable
fun HomeScreen(onOpenDetail: (String) -> Unit) {
    MetricCard(metricId = "recovery", onClick = { onOpenDetail("recovery") })
}
// Source: [CITED: https://developer.android.com/develop/ui/compose/navigation]
```

### Fast-sync trigger from UI action

```kotlin
fun onPullToRefresh() {
    HealthSyncWorker.scheduleImmediate(appContext)
}
// Source: [VERIFIED: app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt]
```

## State of the Art

| Old Approach                                               | Current Approach                                        | When Changed                                                  | Impact                                                                                                                            |
| ---------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Pass complex objects between destinations                  | Pass IDs/primitive args and reload from source of truth | Current Navigation Compose guidance (docs updated 2026-03-30) | Reduces stale object and config-change bugs. [CITED: https://developer.android.com/develop/ui/compose/navigation]                 |
| Build one-off visual widgets without draw caching          | Use DrawScope + drawWithCache for custom graphics       | Current Compose graphics guidance                             | Better performance for arc-heavy dashboards. [CITED: https://developer.android.com/develop/ui/compose/graphics/draw/overview]     |
| Recompute and recompose aggressively on every state change | Use remember/keys/derivedStateOf/deferred reads         | Current Compose performance guidance                          | Lower recomposition cost and smoother motion. [CITED: https://developer.android.com/develop/ui/compose/performance/bestpractices] |

**Deprecated/outdated:**

- Passing full mutable data objects through navigation routes is discouraged in current docs. [CITED: https://developer.android.com/develop/ui/compose/navigation]

## Assumptions Log

| #   | Claim                                                                                                                       | Section                              | Risk if Wrong                                                                          |
| --- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | -------------------------------------------------------------------------------------- |
| A1  | Barcode scanning for UI-06 is locked to ML Kit (`com.google.mlkit:barcode-scanning`) behind a gateway interface. [RESOLVED] | Standard Stack / Don't Hand-Roll     | Low: adapter abstraction contains scanner SDK coupling if replacement is needed later. |
| A2  | Trend charts can be delivered with Compose Canvas without adding a chart library in Phase 04. [ASSUMED]                     | Alternatives / Architecture Patterns | Medium: if feature depth increases, later refactor to chart library may be needed.     |

## Open Questions

None. All previously open implementation questions are now resolved for execution.

### Resolved Decision: Barcode scanning dependency (UI-06)

- **Decision:** Use ML Kit barcode scanning and integrate it through `BarcodeScannerGateway` with `MlKitBarcodeScannerGateway` implementation.
- **Execution impact:** Add dependency in version catalog and app module during Phase 04 data-layer prerequisite plan, then consume via interface in Nutrition feature plan.
- **Rationale:** Proven Android scanner path with low integration risk and clear abstraction boundary.
- **Traceability:** Applies to D-12 and UI-06.

### Resolved Decision: Workout persistence API shape (UI-05)

- **Decision:** Introduce `WorkoutSessionDao` with `insert`, `update`, `deleteById`, `getById`, and `observeRange`; expose via `AiraDatabase`; wire through `WorkoutRepository` and implementation.
- **Execution impact:** DAO/repository prerequisites run in a dedicated plan before Train/Nutrition UI plans.
- **Rationale:** Meets D-13 full edit/delete requirement while keeping local-first Flow-backed history updates.
- **Traceability:** Applies to D-13, UI-05, and UI-06 dependencies.

## Environment Availability

| Dependency            | Required By                            | Available                             | Version       | Fallback                                              |
| --------------------- | -------------------------------------- | ------------------------------------- | ------------- | ----------------------------------------------------- |
| Java runtime          | Gradle build/test execution            | Yes                                   | 23.0.2        | None                                                  |
| Gradle wrapper        | Build/test task execution              | Yes                                   | 8.13          | None                                                  |
| Android SDK directory | Android compile/link tasks             | Yes (from local.properties `sdk.dir`) | Path verified | None                                                  |
| adb on PATH           | Connected device instrumentation tests | No                                    | -             | Use emulator setup task or add platform-tools to PATH |

Sources: [VERIFIED: local.properties] [VERIFIED: terminal java -version] [VERIFIED: terminal gradlew -version] [VERIFIED: terminal ADB_AVAILABLE=false]

**Missing dependencies with no fallback:**

- None for planning and local unit-test execution. [VERIFIED: terminal gradlew -version]

**Missing dependencies with fallback:**

- `adb` not on PATH; instrumentation tests can still be planned but require environment setup before execution. [VERIFIED: terminal ADB_AVAILABLE=false]

## Validation Architecture

### Test Framework

| Property           | Value                                                                                 |
| ------------------ | ------------------------------------------------------------------------------------- |
| Framework          | JUnit 5 + MockK + Turbine (unit), Compose UI test dependency declared for androidTest |
| Config file        | none - Gradle `testOptions` in app build config                                       |
| Quick run command  | `./gradlew.bat :app:testDevDebugUnitTest`                                             |
| Full suite command | `./gradlew.bat :app:test`                                                             |

Sources: [VERIFIED: app/build.gradle.kts] [VERIFIED: app/src/test/java/com/aira/health/*]

### Phase Requirements -> Test Map

| Req ID | Behavior                                                            | Test Type                               | Automated Command                                                    | File Exists?                     |
| ------ | ------------------------------------------------------------------- | --------------------------------------- | -------------------------------------------------------------------- | -------------------------------- |
| UI-01  | Theme switching + score ring rendering contracts                    | screenshot/android UI test              | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "*Theme*"`  | No - Wave 0                      |
| UI-02  | Home 2x2 order + anomaly card always visible + cached-first refresh | android UI test + viewmodel unit test   | `./gradlew.bat :app:testDevDebugUnitTest --tests "*Home*"`           | No - Wave 0                      |
| UI-03  | Detail route navigation + 3-part explanation sheet                  | android UI test                         | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "*Detail*"` | No - Wave 0                      |
| UI-04  | Vitals strip renders latest state without jank                      | android UI test + benchmark/manual perf | `./gradlew.bat :app:connectedDevDebugAndroidTest --tests "*Vitals*"` | No - Wave 0                      |
| UI-05  | Train quick-add/deep-edit + edit/delete history                     | DAO unit tests + android UI test        | `./gradlew.bat :app:testDevDebugUnitTest --tests "*Workout*"`        | Partial - DAO missing            |
| UI-06  | Nutrition quick-add/deep-edit + edit/delete + barcode path          | DAO unit tests + android UI test        | `./gradlew.bat :app:testDevDebugUnitTest --tests "*Nutrition*"`      | Partial - mutation tests missing |

### Sampling Rate

- **Per task commit:** `./gradlew.bat :app:testDevDebugUnitTest`
- **Per wave merge:** `./gradlew.bat :app:test`
- **Phase gate:** unit tests green + key android UI flows green before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `app/src/androidTest/java/com/aira/health/presentation/navigation/AppNavHostTest.kt` - nav start/tab/deep-link checks
- [ ] `app/src/androidTest/java/com/aira/health/presentation/dashboard/HomeDashboardTest.kt` - 2x2 order + anomaly persistence
- [ ] `app/src/androidTest/java/com/aira/health/presentation/details/MetricDetailSheetTest.kt` - D-11 sheet structure
- [ ] `app/src/test/java/com/aira/health/data/local/dao/NutritionLogDaoTest.kt` - add update/delete coverage after DAO expansion
- [ ] `app/src/test/java/com/aira/health/data/local/dao/WorkoutSessionDaoTest.kt` - create DAO + CRUD tests
- [ ] Add instrumentation runner setup docs for emulator/device because `adb` is not on PATH

## Security Domain

### Applicable ASVS Categories

| ASVS Category         | Applies | Standard Control                                                                                                                                                    |
| --------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| V2 Authentication     | yes     | Session/auth state remains owned by existing auth layer; UI reads state only. [VERIFIED: app/src/main/java/com/aira/health/domain/repository/UserRepository.kt]     |
| V3 Session Management | yes     | Keep routing and sensitive screen exposure aligned with existing session model. [VERIFIED: app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt] |
| V4 Access Control     | yes     | Limit sensitive routes/features based on auth/guest state flags. [ASSUMED]                                                                                          |
| V5 Input Validation   | yes     | Validate numeric/manual inputs for Train/Nutrition before DAO writes. [VERIFIED: app/src/main/java/com/aira/health/data/local/model/NutritionLog.kt]                |
| V6 Cryptography       | yes     | Persist data in SQLCipher-backed Room; do not bypass local encrypted path. [VERIFIED: app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt]              |

### Known Threat Patterns for Android Compose + local health data

| Pattern                                          | STRIDE                 | Standard Mitigation                                                                                                                                                           |
| ------------------------------------------------ | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Sensitive dashboard data exposed via screenshots | Information Disclosure | Respect `FLAG_SECURE` build flag behavior and avoid disabling on secured flavors. [VERIFIED: app/src/main/java/com/aira/health/MainActivity.kt]                               |
| Input tampering in quick-add forms               | Tampering              | Client-side validation and bounded numeric parsing before persistence. [ASSUMED]                                                                                              |
| Stale confidence/score mismatch after sync       | Integrity              | Atomic UI state mapping from single Room snapshot and explicit last-updated/confidence labels. [VERIFIED: app/src/main/java/com/aira/health/data/local/model/DailyMetrics.kt] |
| Deep-link route misuse                           | Spoofing/Tampering     | Restrict deep-link argument parsing to primitive IDs and validate destination args. [CITED: https://developer.android.com/develop/ui/compose/navigation]                      |

## Sources

### Primary (HIGH confidence)

- Repository evidence: `.planning/phases/04-user-interface-dashboards/04-CONTEXT.md`, `.planning/phases/04-user-interface-dashboards/04-UI-SPEC.md`, `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md`, `.planning/codebase/ARCHITECTURE.md`, `.planning/codebase/STRUCTURE.md`, `.planning/codebase/CONVENTIONS.md`
- Implementation anchors: `app/src/main/java/com/aira/health/MainActivity.kt`, `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`, `app/src/main/java/com/aira/health/data/local/model/DailyMetrics.kt`, `app/src/main/java/com/aira/health/data/local/model/WorkoutSession.kt`, `app/src/main/java/com/aira/health/data/local/model/NutritionLog.kt`, `app/src/main/java/com/aira/health/data/local/dao/*`, `app/build.gradle.kts`, `gradle/libs.versions.toml`
- Official docs:
  - https://developer.android.com/develop/ui/compose/navigation
  - https://developer.android.com/develop/ui/compose/components/bottom-sheets
  - https://developer.android.com/develop/ui/compose/graphics/draw/overview
  - https://developer.android.com/develop/ui/compose/state-hoisting
  - https://developer.android.com/develop/ui/compose/performance/bestpractices

### Secondary (MEDIUM confidence)

- None.

### Tertiary (LOW confidence)

- Scanner-library recommendation and no-chart-library recommendation remain assumptions pending team confirmation.

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH - dependencies and Compose primitives are already present and documented. [VERIFIED: gradle/libs.versions.toml] [CITED: https://developer.android.com/develop/ui/compose/navigation]
- Architecture: MEDIUM - route/theme/home/detail structure is clear, but UI-05/UI-06 require data API expansion. [VERIFIED: app/src/main/java/com/aira/health/data/local/model/WorkoutSession.kt] [VERIFIED: app/src/main/java/com/aira/health/data/local/dao/NutritionLogDao.kt]
- Pitfalls: HIGH - directly evidenced by locked decisions, design mismatches, and current DAO surface.

**Research date:** 2026-04-15
**Valid until:** 2026-05-15 (recheck sooner if dependency upgrades or scanner decision lands)
