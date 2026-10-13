# 0008 — Audit Log Storage

- Status: Proposed
- Date: 2026-10-13
- Deciders: Platform engineering lead (pending review)
- Context: `.cursorrules` requires an append-only, hash-chained audit log for every write (each row = hash(prev_hash + row_data)). `common-java` already seals records into a SHA-256 chain; services need a shared way to store, protect and verify them. Services run as several instances, so appends can be concurrent.
- Decision: Provide `libs/common-audit`, a Spring Boot auto-configured library used by each service against its own PostgreSQL database:
  - Tables live in an `audit` schema, migrated by a dedicated Flyway instance with its own history table (`audit.flyway_audit_history`), so audit migrations never collide with a service's own migration versions.
  - `audit.audit_log` holds the sealed entries with a gap-free `sequence_no`. A single-row `audit.audit_chain_head` records the end of the chain; `AuditLog.append` locks it (JPA `PESSIMISTIC_WRITE`), seals the record against the head's hash, inserts the entry and advances the head, so concurrent writers across instances extend one linear chain.
  - `append` joins the caller's transaction: an audit entry commits or rolls back with the change it records.
  - A PostgreSQL trigger rejects `UPDATE`, `DELETE` and `TRUNCATE` on `audit.audit_log`. This is the only database-specific SQL, and it lives in the migration; application code uses JPA and Spring Data only.
  - `AuditLog.verify` re-hashes the chain in pages of 500 and checks the head, so edited, removed or reordered rows (including removed newest rows) are detected even if someone bypasses the trigger.
- Consequences:
  - Positive: tamper evidence does not depend on database permissions; one implementation for every service; audit entries can never exist for writes that rolled back.
  - Negative: audited writes within one service are serialised on the chain head row, which caps audited-write throughput per service (fine at PHA volumes; revisit if a service needs more). A superuser can still disable the trigger, which the hash chain then exposes rather than prevents.
  - Neutral: PostgreSQL-specific (`gen_random_uuid()`, PL/pgSQL trigger), matching the platform's database choice.
- Alternatives considered:
  - A central audit service fed by Kafka — rejected for now: writes and their audit entries could diverge if the event is lost or delayed; can be added later by publishing sealed entries.
  - Advisory locks instead of a chain-head row — rejected: needs native SQL in application code.
  - Hash chain without a database trigger — rejected: ordinary application bugs could still modify history; the trigger stops them, the chain detects anything that gets past it.
