package com.pha.common.audit.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Read and insert only; the interface deliberately offers no update or delete.
 */
public interface AuditLogEntryRepository extends Repository<AuditLogEntry, UUID> {

    AuditLogEntry save(AuditLogEntry entry);

    List<AuditLogEntry> findTop500BySequenceNoGreaterThanOrderBySequenceNoAsc(long sequenceNo);

    long count();
}
