---
phase: 01
slug: environment-persistence
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-15
---

# Phase 01 - Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary                        | Description                                                | Data Crossing                                       |
| ------------------------------- | ---------------------------------------------------------- | --------------------------------------------------- |
| Device UI -> App Runtime        | User interactions enter app process during onboarding/auth | Permission decisions, auth intents                  |
| App Runtime -> Local DB         | App writes and reads health and profile data in Room       | Sensitive biometrics and derived metrics            |
| App Runtime -> Android Keystore | App retrieves encryption material via Keystore APIs        | DB encryption key material                          |
| App Runtime -> Supabase         | App sends auth and optional sync traffic to backend        | Auth tokens, account metadata, non-raw derived data |

---

## Threat Register

| Threat ID | Category               | Component                | Disposition | Mitigation                                                                                                                        | Status |
| --------- | ---------------------- | ------------------------ | ----------- | --------------------------------------------------------------------------------------------------------------------------------- | ------ |
| T-01-01   | Information Disclosure | App window capture       | mitigate    | `FLAG_SECURE` enabled on sensitive builds in `MainActivity` using `BuildConfig.ENABLE_FLAG_SECURE`                                | closed |
| T-01-02   | Information Disclosure | Local persistence        | mitigate    | SQLCipher `SupportFactory` with passphrase sourced from `KeystoreManager.getDatabasePassphrase()` and `AndroidKeyStore` key alias | closed |
| T-01-03   | Elevation of Privilege | Health data access scope | mitigate    | Permission batching with explicit Core/Body/Advanced gating via `HealthPermissionManager` and onboarding flow                     | closed |
| T-01-04   | Information Disclosure | Guest auth path          | mitigate    | Guest path returns local session without Supabase calls (`signInAsGuest`) and provider notes guest path bypasses initialization   | closed |

_Status: open · closed_
_Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)_

---

## Evidence

- `app/src/main/java/com/aira/health/MainActivity.kt` (`ENABLE_FLAG_SECURE`, `FLAG_SECURE`)
- `app/src/main/java/com/aira/health/data/local/db/AiraDatabase.kt` (`SupportFactory`)
- `app/src/main/java/com/aira/health/di/DatabaseModule.kt` (`getDatabasePassphrase` usage)
- `app/src/main/java/com/aira/health/util/security/KeystoreManager.kt` (`AndroidKeyStore`, key derivation)
- `app/src/main/java/com/aira/health/util/permission/HealthPermissionManager.kt` (batching)
- `app/src/main/java/com/aira/health/presentation/onboarding/PermissionBatchScreen.kt` (core denial/limited mode UX contract)
- `app/src/main/java/com/aira/health/data/repository/UserRepositoryImpl.kt` (`signInAsGuest` local session)
- `app/src/main/java/com/aira/health/data/remote/supabase/SupabaseClientProvider.kt` (guest-mode initialization note)

---

## Accepted Risks Log

No accepted risks.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By           |
| ---------- | ------------- | ------ | ---- | ---------------- |
| 2026-04-15 | 4             | 4      | 0    | gsd-secure-phase |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-04-15

---

## Notes

No `<threat_model>` block was present in phase PLAN artifacts. This file was generated from executed phase artifacts and implementation evidence per secure-phase State B.
