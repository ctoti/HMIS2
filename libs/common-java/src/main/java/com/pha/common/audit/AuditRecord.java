package com.pha.common.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * One audited write. {@code payload} must already be PII-redacted.
 */
public record AuditRecord(
        UUID id,
        Instant occurredAt,
        String actorId,
        String action,
        String resourceType,
        String resourceId,
        String payload) {

    public AuditRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(payload, "payload");
        // PostgreSQL timestamps hold microseconds; truncating keeps hashes stable after a round trip.
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
    }
}
