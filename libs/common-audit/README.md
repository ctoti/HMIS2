# common-audit

Append-only, hash-chained audit log stored in each service's PostgreSQL database (ADR 0008).

## Use it in a service

Add the dependency (version managed by the root POM):

```xml
<dependency>
    <groupId>com.pha</groupId>
    <artifactId>common-audit</artifactId>
</dependency>
```

Spring Boot auto-configuration then:

- creates the `audit` schema and its tables on startup, using a separate Flyway history table (`audit.flyway_audit_history`) so the service's own migrations are unaffected;
- registers the audit entities and repositories alongside the service's own;
- exposes an `AuditLog` bean.

Record a write in the same transaction as the change:

```java
@Transactional
public LeaveRequest approve(UUID id, String actorId) {
    LeaveRequest request = repository.approve(id);
    auditLog.append(new AuditRecord(UUID.randomUUID(), Instant.now(), actorId, "APPROVE",
            "leave-request", id.toString(), "{\"status\":\"APPROVED\"}"));
    return request;
}
```

If the transaction rolls back, the audit entry does too. **The payload must not contain personal data** (names, DOB, MRN, contact details, national ID); record identifiers and changed fields instead.

## Guarantees

| Threat | Protection |
|--------|------------|
| Application code updates or deletes history | Database trigger rejects `UPDATE`, `DELETE` and `TRUNCATE` on `audit.audit_log` |
| Someone with database rights disables the trigger and edits, removes or reorders rows | `AuditLog.verify()` re-hashes the chain and checks the chain head; returns the first bad `sequence_no` |
| Concurrent writers on several instances | Appends lock the single chain-head row, so the chain stays linear and gap-free |

Run `verify()` from a scheduled job or an admin endpoint; it reads the log in pages of 500.

## Tables

- `audit.audit_log` — `id`, `sequence_no`, `occurred_at`, `actor_id`, `action`, `resource_type`, `resource_id`, `payload`, `previous_hash`, `hash`, `created_at`; indexed by resource, actor and time.
- `audit.audit_chain_head` — one row with the last sequence number and hash.

## Test

```sh
mvn -pl libs/common-audit -am verify   # needs Docker for the PostgreSQL Testcontainer
```
