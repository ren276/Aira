# Stack Research

**Domain:** Android Health & Fitness App (On-device AI + Wearables)
**Researched:** 2026-04-15
**Confidence:** HIGH

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.0.0+ | Primary App Language | Fully native Android language, heavily supported by Google |
| Jetpack Compose | BOM 2024.10.x | UI Framework | Declarative UI, great for complex dynamic charting and dashboards |
| Health Connect | 1.1.0-alpha07+ | Health Data Reading | Single source of truth for all Android health data, replaces Google Fit |
| Supabase KT | 3.x | Backend / DB Sync | Native Kotlin SDK, PostgreSQL RLS for strict privacy |
| Room DB | 2.6.x | Local Storage | Encrypted (SQLCipher) on-device persistence of metrics and health data |
| TensorFlow Lite | 2.16.x | Custom ML | Minimal footprint for executing the PersonalisedScoreAdjuster and AnomalyDetector |
| MediaPipe Tasks GenAI | 0.10.14+ | LLM Inference | Best optimized runtime for Gemma 4 2B directly on Android devices |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hilt | 2.51.x | Dependency Injection | Standard for scalable Clean Architecture Android projects |
| Coil | 2.6.x | Image Loading | Loading avatars, food images efficiently in Compose |
| kotlinx.serialization | 1.6.x | JSON Parsing | Parsing complex schema objects like workout sets |
| GoogleSignIn | 21.2.x | Auth | Seamless integration with Supabase Auth for Android |
| YCharts / Vico | 2.x | Charting | Drawing biological trends, cardio loads, area charts quickly |
| RevenueCat | 7.x | Subscription Billing | Handling Aira Pro gating natively and easily |

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Supabase | Firebase / Convex | Firebase operates primarily as a NoSQL datastore Document DB (less ideal for heavy relational physiological time-series metrics). Convex is great but has a less robust Kotlin native SDK compared to Supabase's fully typed support. |
| Jetpack Compose | Flutter / React Native | When cross-platform iOS is required immediately (Aira explicitly targets Android ecosystem first). |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Google Fit API (Primary) | Deprecated by Google for biometric reads; many data types will no longer sync securely over REST. | Health Connect API |
| Cloud LLMs (for core features) | Transmitting raw bio-data off device breaks the privacy-first promise and creates compliance nightmares. | Gemma 4 2B (e2b) local via MediaPipe |

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Supabase Auth | Android Keystore | Ensure the SDK uses `EncryptedSharedPreferences` for token storage. |
| MediaPipe LLM | Gemma 4 2B (e2b) | Ensure appropriate `.task` / `.bin` weights are correctly converted and downloaded dynamically instead of packaging directly in the APK to avoid 2GB+ app sizes. |

---
*Stack research for: Android Health & Fitness App (On-device AI + Wearables)*
*Researched: 2026-04-15*
