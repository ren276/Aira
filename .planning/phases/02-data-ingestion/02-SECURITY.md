---
phase: 02
slug: data-ingestion
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-15
---

# Phase 02 - Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary                                    | Description                                                                         | Data Crossing                                   |
| ------------------------------------------- | ----------------------------------------------------------------------------------- | ----------------------------------------------- |
| Device Health Providers -> App Runtime      | Health Connect/Google Fit records are ingested into app process                     | Biometric samples, sleep/activity measurements  |
| App Runtime -> Local Storage                | Ingestion pipeline writes normalized records and sync metadata to local persistence | Time-series physiological data, sync timestamps |
| Android System Events -> App Runtime        | BOOT_COMPLETED and WorkManager scheduler trigger background ingestion               | Execution triggers and scheduling metadata      |
| DI/Worker Construction -> Runtime Execution | Hilt provisions repositories and workers for periodic ingestion                     | Repository instances, worker dependencies       |

---

## Threat Register

| Threat ID | Category               | Component                                  | Disposition | Mitigation                                                                                                                                                   | Status |
| --------- | ---------------------- | ------------------------------------------ | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------ |
| T-02-01   | Tampering              | Source confidence arbitration              | mitigate    | `ConfidenceRouter` centralizes package-based confidence tiers and `IngestHealthDataUseCase` resolves overlapping records via higher confidence selection     | closed |
| T-02-02   | Denial of Service      | Background sync continuity                 | mitigate    | `HealthSyncWorker` periodic scheduling + `BootReceiver` reschedule path on reboot with idempotent WorkManager policy                                         | closed |
| T-02-03   | Spoofing               | Data source fallback routing               | mitigate    | `HealthDataModule` gates Health Connect availability and only falls back to `GoogleFitRepositoryImpl` when provider unavailable                              | closed |
| T-02-04   | Information Disclosure | Background worker permissions and triggers | mitigate    | Manifest explicitly declares `RECEIVE_BOOT_COMPLETED` and constrained receiver path; worker reads local health providers without requiring network transport | closed |

_Status: open · closed_
_Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)_

---

## Evidence

- `app/src/main/java/com/aira/health/data/model/ConfidenceRouter.kt` (`getConfidenceWeight`, tier mapping)
- `app/src/main/java/com/aira/health/domain/usecase/IngestHealthDataUseCase.kt` (conflict resolution and ingestion orchestration)
- `app/src/main/java/com/aira/health/data/worker/HealthSyncWorker.kt` (periodic scheduling, worker policy)
- `app/src/main/java/com/aira/health/util/receiver/BootReceiver.kt` (reboot rescheduling)
- `app/src/main/java/com/aira/health/di/HealthDataModule.kt` (HC->GFit fallback routing)
- `app/src/main/java/com/aira/health/AiraApplication.kt` (`HiltWorkerFactory` wiring into WorkManager config)
- `app/src/main/AndroidManifest.xml` (`RECEIVE_BOOT_COMPLETED`, `BootReceiver` declaration)

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

No explicit `<threat_model>` block or `## Threat Flags` section was present in phase 2 plan/summary artifacts. Threat register was reconstructed from implemented phase artifacts and code evidence per secure-phase State B.
