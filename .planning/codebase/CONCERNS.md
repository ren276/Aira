# Architecture & Security Concerns

This document tracks technical debt, security risks, and privacy considerations for Aira.

## Privacy & Security

### Raw Data Isolation
- **Concern**: Biometric data (HRV, Sleep Stages, Heart Rate) must never leave the device in raw form.
- **Mitigation**: SQLCipher encrypts the Room database. All scoring calculations happen on-device using custom TFLite models.
- **Audit Requirement**: Periodic review of outbound network calls to ensure no sensitive metadata is leaked.

### Build Security
- **Concern**: Leakage of API keys or sensitive configurations.
- **Mitigation**: Use of `ENABLE_FLAG_SECURE` in `MainActivity` for sensitive builds. Environment-specific flavors (dev, staging, prod) manage distinct Supabase environments.

## Data Consistency

### Health Connect Synchronization
- **Concern**: Data duplication or gaps when syncing from Health Connect.
- **Mitigation**: Robust deduplication logic in the data layer using Health Connect's `clientRecordId`. Use of `WorkManager` for reliable periodic syncing.

### Local AI Model Lifecycle
- **Concern**: Large model file sizes (>100MB) bloating the APK.
- **Mitigation**: Dynamically download the `Gemma 2B` task file from Supabase Storage instead of packaging it in the APK.

## Technical Debt

### Scaling Use Cases
- **Concern**: As the number of scores increases, the domain layer may become cluttered with highly specific Use Cases.
- **Mitigation**: Consider a pattern for composite Use Cases or scoring pipelines to reduce boilerplate.

### Complex UI States
- **Concern**: The `UiState` data classes in ViewModels can grow very large, leading to unnecessary UI recompositions.
- **Mitigation**: Use optimized `copy()` operations and ensure Composable functions are stable.
