# Phase 10 Security Hardening

threats_open: 0
high_severity_open: 0

## Scope

Phase 10 continuity snapshot flow, reset safety gate, and local reset override controls.

## Threat Register

| Threat ID | Category               | Surface                                  | Severity | Status | Mitigation                                                                                                                         |
| --------- | ---------------------- | ---------------------------------------- | -------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------- |
| T-10-01   | Information Disclosure | Continuity payload content               | High     | Closed | Snapshot model is derived-only and excludes raw biometric rows by contract.                                                        |
| T-10-02   | Tampering              | Restore payload selection path           | Medium   | Closed | Restore flow remains user-mediated with explicit selection/apply boundary and deterministic null-safe result path.                 |
| T-10-03   | Denial of Service      | Retry loop on failed upload before reset | Medium   | Closed | Reset flow blocks wipe by default and requires explicit user action (retry or override), preventing uncontrolled destructive loop. |
| T-10-04   | Elevation of Privilege | Irreversible override misuse             | High     | Closed | Override path requires a distinct arm step and separate confirm action before wipe execution.                                      |
| T-10-05   | Repudiation            | Wipe after failed upload                 | Medium   | Closed | UI state exposes blocked reason and explicit override state transitions; tests verify blocked vs override branches.                |

## Residual Risk

- Continuity snapshot persistence now depends on `scripts/supabase/migrations/20260418_phase10_continuity_snapshots.sql` being applied in Supabase and on `auth.uid()::text = user_id` policy alignment; misconfiguration can block uploads/restores but does not widen raw-data exposure boundary.
- No unresolved high-severity threats remain for this phase.
