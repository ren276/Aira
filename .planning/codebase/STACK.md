# Technology Stack

This document tracks the core technologies, frameworks, and libraries used in the Aira project.

## Core Stack

| Layer | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | 2.0.21 | Primary application language. |
| **UI Framework** | Jetpack Compose | BOM 2024.10.01 | Declarative UI toolkit. |
| **Dependency Injection** | Hilt | 2.52 | Standard dependency injection framework. |
| **Local Database** | Room | 2.6.1 | Persistent data storage with SQLCipher encryption. |
| **Concurrency** | Kotlin Coroutines | 1.9.0 | Asynchronous programming and background tasks. |
| **Network Client** | Ktor | 3.0.1 | Type-safe HTTP client (used with Supabase). |
| **Image Loading** | Coil | 2.7.0 | Modern image loading library for Compose. |

## Data & Persistence

- **Room DB**: Used for all local biological and metric data.
- **SQLCipher (4.5.4)**: Provides full database encryption.
- **DataStore (1.1.1)**: Used for simple key-value settings and preferences.
- **Supabase (3.0.0)**: Backend service for Auth, Postgrest, Realtime, and Storage.

## Specialized Libraries

- **Health Connect (1.1.0-alpha07)**: Primary source for Android health data.
- **MediaPipe GenAI (0.10.22)**: On-device LLM inference (Gemma).
- **TensorFlow Lite (2.16.1)**: On-device ML execution.
- **RevenueCat (7.10.1)**: Subscription management and billing.
- **Firebase (33.6.0)**: Crashlytics, Analytics, and Performance monitoring.

## Build System

- **Gradle (8.7.3)**: Build automation tool.
- **AGP (8.7.3)**: Android Gradle Plugin.
- **Version Catalog**: Centralized dependency management in `gradle/libs.versions.toml`.
