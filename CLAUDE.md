<!-- GSD:project-start source:PROJECT.md -->
## Project

**Aira**

A privacy-first, on-device health intelligence OS for Android that aggregates wearable and phone sensor data, learns the user's unique physiology over time, and converts noisy raw data into accurate, explainable daily scores with causal reasoning. It is designed to be Android-first, built natively with Jetpack Compose, integrating deeply with Health Connect and Google Fit.

**Core Value:** Empower Android users with true on-device, explainable health intelligence that learns their unique physiology without compromising their privacy or requiring their raw biome data to leave the device.

### Constraints

- **Tech Stack**: 100% Kotlin, Jetpack Compose, Room, WorkManager, Hilt, Supabase, Coil, kotlinx.serialization — Native Android approach for long-term maintainability.
- **Privacy/Security**: Must ensure complete raw data isolation locally on the device (SQLCipher). Only computed metrics, scores, and AI narratives sync to Supabase (if opted-in).
- **Target OS**: minSdk 29 to support Android 10 users, but pushing them towards Health Connect via Play Store.
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

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
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.agent/skills/`, `.agents/skills/`, `.cursor/skills/`, or `.github/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
