# Codebase Concerns

**Analysis Date:** 2026-04-16

## Runtime Data Wiring & UI Alignment Phase 5

**Primary scope:** runtime Compose surfaces that still need contract alignment, static-copy cleanup, or tighter binding to local data flows.

### Include in the phase

**Home dashboard and score shell:**

- Files: `app/src/main/java/com/aira/health/presentation/dashboard/home/HomeDashboardScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/HomeViewModel.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/HomeUiState.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/components/CausalAnomalyCard.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/components/ForecastGuidanceCard.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/components/EnergyBankChart.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/components/MetricGridCard.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/components/VitalSparkline.kt`
- Flow: `DailyMetricsDao.observeByDate()` / `observeRecent()` feed `HomeViewModel`, which triggers `HealthSyncWorker.scheduleImmediate()` for refresh and maps `DailyMetrics` into the 2x2 card grid.
- Concern: `HomeUiState.Success.lastUpdated` is present in the state contract but the screen does not surface it, and `ConfidenceMetaRow` exists in `presentation/common/components/ConfidenceMetaRow.kt` but is not used here.

**Metric detail surfaces:**

- Files: `app/src/main/java/com/aira/health/presentation/dashboard/details/MetricDetailRoute.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/MetricDetailViewModel.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/MetricDetailUiState.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/RecoveryDetailScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/SleepDetailScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/StrainDetailScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/StressDetailScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/components/ExplanationBottomSheet.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/components/MetricTrendWindow.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/components/FactorBreakdownCard.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/details/components/ActionGuidanceCard.kt`
- Flow: `MetricDetailViewModel` reads `DailyMetricsDao.getLast14Days()` and `getRange()`, derives the selected metric from `MetricType`, and passes the 3-part explanation contract into the bottom sheet.
- Concern: the detail screens are already data-backed, but they still rely on a shared `DailyMetrics` row only; if the rewrite needs richer factor attribution, that is a domain/data dependency rather than a UI-only change.

**Train and nutrition CRUD flows:**

- Files: `app/src/main/java/com/aira/health/presentation/train/TrainScreen.kt`, `app/src/main/java/com/aira/health/presentation/train/TrainViewModel.kt`, `app/src/main/java/com/aira/health/presentation/train/TrainEditScreen.kt`, `app/src/main/java/com/aira/health/presentation/train/TrainEditViewModel.kt`, `app/src/main/java/com/aira/health/presentation/nutrition/NutritionScreen.kt`, `app/src/main/java/com/aira/health/presentation/nutrition/NutritionViewModel.kt`, `app/src/main/java/com/aira/health/presentation/nutrition/NutritionEditScreen.kt`, `app/src/main/java/com/aira/health/presentation/nutrition/NutritionEditViewModel.kt`, `app/src/main/java/com/aira/health/presentation/nutrition/scanner/BarcodeScannerGateway.kt`, `app/src/main/java/com/aira/health/presentation/nutrition/scanner/MlKitBarcodeScannerGateway.kt`
- Flow: `WorkoutRepositoryImpl` and `NutritionRepositoryImpl` bridge the screen state to `WorkoutSessionDao` and `NutritionLogDao`; the edit screens load by ID and commit update/delete mutations back through the repositories.
- Concern: these flows are structurally complete, so the phase should focus on state alignment, empty/error behavior, and route semantics rather than new persistence work.

**Secondary runtime surfaces that are still data-wiring sensitive:**

- Files: `app/src/main/java/com/aira/health/presentation/dashboard/body/BodyScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachScreen.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/WeeklyReportScreen.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/WhatIfSimulatorScreen.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/InsightsPredictionsScreen.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/DataConfidenceScreen.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/DataCorrectionsScreen.kt`, `app/src/main/java/com/aira/health/presentation/settings/SettingsScreen.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/AccountScreen.kt`
- Flow: most of these surfaces read `HomeViewModel` or `DailyMetricsDao` directly and render derived text or summary cards from local data.
- Concern: several are heuristic or static-copy surfaces rather than fully independent data products, so they are better treated as alignment/polish work after the core dashboard and edit flows are stable.

### Stale static state and contract mismatches

**Home header metadata is under-rendered:**

- Issue: `HomeUiState.Success.lastUpdated` is part of the contract, but `HomeDashboardScreen.kt` only displays the confidence badge and does not surface the last-updated context.
- Files: `app/src/main/java/com/aira/health/presentation/dashboard/home/HomeUiState.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/HomeDashboardScreen.kt`, `app/src/main/java/com/aira/health/presentation/common/components/ConfidenceMetaRow.kt`
- Impact: the primary score surface does not meet its own state contract and hides freshness information that should be visible during runtime wiring.

**Visual scaffolding still contains transitional content:**

- Issue: `HomeDashboardScreen.kt` still contains placeholder comments for the top app bar and avatar slot, `ForecastGuidanceCard.kt` is explicitly static fallback text, and `EnergyBankChart.kt` draws a dummy curve for visual approximation.
- Files: `app/src/main/java/com/aira/health/presentation/dashboard/home/HomeDashboardScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/components/ForecastGuidanceCard.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/home/components/EnergyBankChart.kt`
- Impact: these surfaces can look finished while still shipping transitional copy or approximated visuals.

**Heuristic narrative surfaces should not be mistaken for independent data pipelines:**

- Issue: `BodyScreen.kt` and `CoachScreen.kt` derive their narrative from `HomeUiState.Success`, while `WeeklyReportViewModel.kt`, `WhatIfViewModel.kt`, and `InsightsPredictionsViewModel.kt` compute lightweight projections from recent `DailyMetrics` rows.
- Files: `app/src/main/java/com/aira/health/presentation/dashboard/body/BodyScreen.kt`, `app/src/main/java/com/aira/health/presentation/dashboard/coach/CoachScreen.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/WeeklyReportViewModel.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/WhatIfViewModel.kt`, `app/src/main/java/com/aira/health/presentation/supplementary/InsightsPredictionsViewModel.kt`
- Impact: the UI is data-connected, but the phase should avoid promising stronger model-backed behavior than the current runtime actually provides.

**Settings toggles are local preferences only:**

- Issue: `SettingsViewModel.kt` persists `cloud_backup_enabled` in DataStore, but there is no downstream consumer in the runtime data flow.
- Files: `app/src/main/java/com/aira/health/presentation/settings/SettingsViewModel.kt`, `app/src/main/java/com/aira/health/presentation/settings/SettingsScreen.kt`
- Impact: the UI suggests a functional backup feature when the current code only stores a preference flag.

### Main implementation risks and dependencies

**Freshness depends on the sync worker chain:**

- Dependency: `HealthSyncWorker.kt` orchestrates `IngestHealthDataUseCase.kt` and `ComputeDailyScoresUseCase.kt`, then persists `DailyMetrics` for the UI to consume.
- Files: `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`, `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt`, `app/src/main/java/com/aira/health/domain/usecase/ComputeDailyScoresUseCase.kt`
- Risk: if this chain stalls, the Compose surfaces will continue rendering cached rows and derived heuristics, which can mask a stale runtime state problem.

**Source-selection fallback can look like empty data:**

- Dependency: `HealthDataModule.kt` switches between Health Connect and Google Fit at runtime.
- Files: `app/src/main/java/com/aira/health/di/HealthDataModule.kt`, `app/src/main/java/com/aira/health/data/repository/HealthConnectRepositoryImpl.kt`, `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`
- Risk: Google Fit failures are currently best-effort, so a wiring problem can present as a legitimate no-data state in Home and detail screens.

**Edit flows are sensitive to ID routing and mutation round trips:**

- Dependency: `AiraRoutes.kt`, `AiraNavHost.kt`, `TrainEditScreen.kt`, and `NutritionEditScreen.kt` all rely on ID-only navigation arguments and post-save back navigation.
- Files: `app/src/main/java/com/aira/health/presentation/navigation/AiraRoutes.kt`, `app/src/main/java/com/aira/health/presentation/navigation/AiraNavHost.kt`, `app/src/main/java/com/aira/health/presentation/train/TrainEditScreen.kt`, `app/src/main/java/com/aira/health/presentation/nutrition/NutritionEditScreen.kt`
- Risk: route parsing or mutation failure is user-visible immediately because these are interactive write paths.

### Suggested phase boundary and waves

**Include now:** `HomeDashboardScreen.kt`, `MetricDetailRoute.kt` and all four metric detail screens, `TrainScreen.kt` / `TrainEditScreen.kt`, `NutritionScreen.kt` / `NutritionEditScreen.kt`, `SettingsScreen.kt`, `AccountScreen.kt`, `DataConfidenceScreen.kt`, `DataCorrectionsScreen.kt`, `WeeklyReportScreen.kt`, `WhatIfSimulatorScreen.kt`, `InsightsPredictionsScreen.kt`, `BodyScreen.kt`, `CoachScreen.kt`.

**Defer:** onboarding/auth entry, ingestion math changes, new data-source integrations, and any AI/backend feature work that would expand beyond current runtime contracts.

**Wave 1:** root shell and route contract alignment, including last-updated/confidence visibility.

**Wave 2:** home dashboard and metric detail surfaces, including static-copy cleanup and fallback-state normalization.

**Wave 3:** Train and Nutrition edit flows, barcode/scanner path validation, and mutation round-trip checks.

**Wave 4:** secondary runtime surfaces (`Body`, `Coach`, reports, simulations, settings) plus final polish, accessibility, and regression tests.

## Tech Debt

**Encrypted database bootstrap is brittle:**

- Issue: `KeystoreManager.getDatabasePassphrase()` reads `SecretKey.encoded`, but Android Keystore secret keys are typically non-exportable. `DatabaseModule.provideAiraDatabase()` calls this during singleton creation, so database startup can fail before the app renders.
- Files: `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt`, `app/src/main/java/com/aira/health/di/DatabaseModule.kt`, `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Impact: A cold-start crash blocks access to all encrypted local health data.
- Fix approach: Use a stable exportable seed protected by Keystore, or derive the SQLCipher passphrase without relying on `SecretKey.encoded`.

**Room migration policy will destroy local history:**

- Issue: `AiraDatabase.create()` uses `.fallbackToDestructiveMigration()` while `exportSchema = true` is enabled.
- Files: `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Impact: Any future schema version bump will wipe stored biometrics, corrections, conversations, and baselines.
- Fix approach: Add explicit Room migrations before raising the schema version and keep the exported schema artifacts in sync.

**Build config handling is permissive instead of fail-fast:**

- Issue: `getLocalProperty()` returns an empty string when a required value is missing, and both the `dev` and `staging` flavors read `SUPABASE_STAGING_*` values.
- Files: `app/build.gradle.kts`, `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt`
- Impact: Builds can succeed with blank Supabase configuration, and dev builds can hit the staging backend instead of an isolated environment.
- Fix approach: Fail the build on missing required properties and give the dev flavor its own backend values.

**Guest account upgrade does not migrate local data yet:**

- Issue: `UserRepositoryImpl.upgradeGuestAccount()` signs up the user and returns the remote session, but the Room-to-Supabase migration is still a TODO.
- Files: `app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt`
- Impact: Guest users who upgrade can strand or lose locally accumulated history and corrections.
- Fix approach: Add a resumable migration job and treat account upgrade as incomplete until local data has been reconciled.

## Known Bugs

**Google Fit fallback can fail silently:**

- Symptoms: The fallback repository returns empty lists on any read failure, which makes permissions errors, deprecated API behavior, and account issues look like valid "no data" states.
- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`, `app/src/main/java/com/aira/health/di/HealthDataModule.kt`
- Trigger: Missing Google account, incomplete Fit permissions, or a read failure in any of the `runCatching { ... }.getOrElse { emptyList() }` blocks.
- Workaround: None in code; the failure is swallowed.

**Fallback permissions check is incomplete:**

- Symptoms: `GoogleFitRepositoryImpl.isAvailable()` checks only heart-rate access, but the repository also reads sleep, calories, and steps.
- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`
- Trigger: Devices where only a subset of Google Fit scopes is granted.
- Workaround: The repository quietly degrades to empty data for the missing reads.

## Security Considerations

**Telemetry gating is only partially controlled:**

- Risk: `FirebaseApp.initializeApp(this)` runs for every build and only Crashlytics collection is explicitly toggled. Analytics and Performance dependencies are still present without a visible runtime consent gate.
- Files: `app/src/main/java/com/aira/health/AiraApplication.kt`, `app/build.gradle.kts`
- Current mitigation: Crashlytics collection is disabled in builds where `BuildConfig.ENABLE_CRASH_REPORTING` is false.
- Recommendations: Gate all telemetry behind explicit consent and verify the default-disabled state in release builds.

**Boot receiver is exported:**

- Risk: `BootReceiver` is exported without an app-specific permission, so other apps can send it broadcasts and force sync scheduling.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt`
- Current mitigation: The receiver only reacts to `Intent.ACTION_BOOT_COMPLETED`.
- Recommendations: Keep the receiver minimal, verify the action defensively, and reassess whether public export is still necessary.

## Fragile Areas

**Sync worker hides root causes:**

- Files: `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt`
- Why fragile: `doWork()` catches all exceptions, retries or fails generically, and does not preserve the throwable context in logs.
- Safe modification: Surface structured diagnostics when sync fails so production issues can be triaged.
- Test coverage: `app/src/test/java/com/aira/health/data/worker/HealthSyncWorkerScheduleTest.kt` only verifies scheduling, not failure behavior.

**Google Fit integration is best-effort only:**

- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`, `app/src/main/java/com/aira/health/di/HealthDataModule.kt`
- Why fragile: The repository is injected whenever Health Connect is unavailable, but runtime success still depends on a Google account, Fit permissions, and deprecated API behavior.
- Safe modification: Validate all required scopes before using the fallback path and report partial ingestion explicitly.
- Test coverage: There are no direct unit tests for the Google Fit repository.

**Local data loss on version changes:**

- Files: `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Why fragile: Any schema evolution that reaches production without explicit migrations will delete user data.
- Safe modification: Add migration coverage before changing `version`.
- Test coverage: No database migration tests are present.

## Scaling Limits

**Anonymous and authenticated data paths are not fully separated yet:**

- Current capacity: Guest mode exists, but the upgrade path does not migrate local state.
- Limit: Scaling account conversion without a migration job will continue to create data islands.
- Scaling path: Treat local-to-cloud migration as a first-class workflow with retries and idempotency.

## Test Coverage Gaps

**Crypto/bootstrap path:**

- What's not tested: `KeystoreManager` and Room/SQLCipher startup.
- Files: `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt`, `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt`
- Risk: Startup-only encryption failures can ship unnoticed.
- Priority: High.

**Supabase and telemetry init:**

- What's not tested: `AiraApplication`, `SupabaseClientProvider`, and build-config driven environment wiring.
- Files: `app/src/main/java/com/aira/health/AiraApplication.kt`, `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt`, `app/build.gradle.kts`
- Risk: Privacy or initialization regressions can slip past unit tests.
- Priority: High.

**Manifest and boot-time behavior:**

- What's not tested: exported receiver behavior, boot rescheduling, and permission alias wiring.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt`
- Risk: Security or boot-time regressions only surface in on-device testing.
- Priority: Medium.

**Fallback ingestion behavior:**

- What's not tested: `GoogleFitRepositoryImpl` partial permissions, read failures, and empty-data scenarios.
- Files: `app/src/main/java/com/aira/health/data/repository/GoogleFitRepositoryImpl.kt`
- Risk: The fallback can look healthy while ingesting nothing.
- Priority: High.

---

_Concerns audit: 2026-04-15_
