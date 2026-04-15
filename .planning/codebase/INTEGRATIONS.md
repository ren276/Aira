# External Integrations

This document outlines the external services and APIs that Aira integrates with.

## Health Data Sources

### Health Connect
- **Purpose**: Single source of truth for all on-device health and biometric data.
- **Integration**: `androidx.health.connect:connect-client`.
- **Status**: Primary data source. Replaces legacy Google Fit API for biometric reads.

### Google Fit (Play Services Fitness)
- **Purpose**: Secondary data source for legacy integrations or specific data types not yet in Health Connect.
- **Integration**: `com.google.android.gms:play-services-fitness`.

## Backend & Cloud

### Supabase
- **Purpose**: Identity management, telemetry sync (computed metrics only), and configuration.
- **SDKs**: Auth, Postgrest, Realtime, Storage.
- **Ktor Client**: Used as the underlying HTTP engine for Supabase Kotlin SDK.

### Firebase
- **Crashlytics**: Real-time crash reporting for stability monitoring.
- **Analytics**: Anonymous usage statistics to improve user experience.
- **Performance**: Monitoring app start time and screen rendering.

## On-Device AI/ML

### MediaPipe Tasks GenAI
- **Purpose**: Running local LLMs (Gemma 2B) for explainable health narratives.
- **Requirement**: Models are downloaded dynamically at runtime to keep APK size manageable.

### TensorFlow Lite
- **Purpose**: Executing custom `PersonalisedScoreAdjuster` and `AnomalyDetector` models.
- **Backend**: NNAPI or GPU acceleration where available.

## Monetization

### RevenueCat
- **Purpose**: Managing subscriptions for "Aira Pro" features.
- **Integration**: Handles cross-platform verification and receipt validation natively.
- **Status**: Configured for Google Play Store billing.
