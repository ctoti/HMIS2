# 0009 — Leave Requests in hr-service Ahead of the BPM Workflow

- Status: Proposed
- Date: 2026-10-13
- Deciders: Platform engineering lead, PHA HR manager (pending review)
- Context: Phase 2 starts hr-service, and leave requests are its first write. `.cursorrules` puts workflow state in the BPM engine ("Do NOT store BPMN state outside the BPM engine") and asks for `Idempotency-Key` on critical POSTs. bpm-service does not exist yet: the engine choice (ADR 0002) and its Spring Boot 4 support (Q8) are open, and so is the event bus (ADR 0003). Leave rules (types, day counting, balances) are not yet confirmed (Q11).
- Decision:
  - hr-service owns the leave request record and only the states that need no approval: `SUBMITTED` and `CANCELLED` (by the requester). Approval, rejection, escalation and SLAs belong to the BPM workflow. When bpm-service exists, hr-service will publish `leave-request.submitted` and record outcomes it receives as events; it will never decide them.
  - Staff submit only for their own staff record, found through the Keycloak subject stored on it (`staff.user_id`). HR officers read and list requests at the facilities in their `/facilities/<code>` groups; platform admins everywhere.
  - Idempotency lives on the request row: a unique `(requested_by, idempotency_key)` constraint plus a SHA-256 fingerprint of the body. The same key and body returns the original request with `200`; the same key with a different body is `409`. The caller's staff row is locked for the duration of a submission, so retries and overlap checks for one person run one at a time.
  - The request copies the staff member's facility code at submission, so facility scoping of past requests survives a transfer.
  - Leave is counted in calendar days until Q11 is answered.
- Consequences:
  - Positive: staff can record leave now; the BPM workflow can be added without changing the API's existing fields; retries from flaky rural connections never create duplicates.
  - Negative: until bpm-service exists, requests stay `SUBMITTED` and nobody can approve them in the platform. Per-person locking serialises one person's submissions (negligible at expected volumes).
  - Neutral: idempotency keys are kept with the request forever rather than expiring; a separate key store can replace this if other endpoints need idempotency too.
- Alternatives considered:
  - Approve and reject in hr-service now — rejected: it would store workflow state outside the BPM engine and have to be migrated out later.
  - A generic idempotency-key table with stored responses — rejected for now: only one endpoint needs it, and the request row already holds everything a replay returns.
  - Wait for bpm-service before building leave requests — rejected: blocks Phase 2 on two open questions.
