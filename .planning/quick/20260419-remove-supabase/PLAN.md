---
title: Remove Supabase Dependencies and Transition to Firebase
slug: remove-supabase
status: in-progress
created_at: 2026-04-19T13:49:20+05:30
---

# Remove Supabase Dependencies

This task removes all remaining Supabase configurations, build fields, and code dependencies, ensuring the project is fully decoupled from Supabase and relies on Firebase for backend services and direct Gemini API access for AI features.

## Proposed Changes

1.  **Remove Supabase build config** in `app/build.gradle.kts`.
2.  **Add Gemini API Key build config** in `app/build.gradle.kts` (dev flavor).
3.  **Refactor Gemini Auth**: 
    - Replace `BackendGeminiAuthTokenProvider` with `LocalGeminiAuthTokenProvider`.
    - Update `GeminiCloudRuntimeGateway` to use `x-goog-api-key` header when an API key is provided.
4.  **Data Layer Migration**:
    - Rename `syncedToSupabase` in `HealthRecordRaw` to `syncedToRemote`.
    - Create Room migration v8 -> v9.
5.  **Documentation Update**:
    - Update `GEMINI.md` and `CLAUDE.md` to reflect Firebase replacement.

## Verification Plan

- [ ] Successful build: `./gradlew :app:assembleDevDebug`
- [ ] Room migration verified: Check schema export or run tests.
- [ ] AI Connectivity: Verify `GeminiCloudRuntimeGateway` uses API key.
