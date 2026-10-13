package com.pha.common.audit.jpa;

import com.pha.common.audit.AuditRecord;
import com.pha.common.audit.SealedAuditEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

/**
 * One row of the append-only audit log. Hibernate never updates it, and a database trigger rejects changes.
 */
@Entity
@Immutable
@Table(schema = "audit", name = "audit_log")
public class AuditLogEntry implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private String actorId;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(name = "resource_type", nullable = false, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private String resourceId;

    @Column(nullable = false, updatable = false)
    private String payload;

    @Column(name = "previous_hash", nullable = false, updatable = false)
    private String previousHash;

    @Column(nullable = false, updatable = false)
    private String hash;

    @Transient
    private boolean persisted;

    protected AuditLogEntry() {
    }

    private AuditLogEntry(long sequenceNo, SealedAuditEntry sealed) {
        AuditRecord record = sealed.record();
        this.id = record.id();
        this.sequenceNo = sequenceNo;
        this.occurredAt = record.occurredAt();
        this.actorId = record.actorId();
        this.action = record.action();
        this.resourceType = record.resourceType();
        this.resourceId = record.resourceId();
        this.payload = record.payload();
        this.previousHash = sealed.previousHash();
        this.hash = sealed.hash();
    }

    static AuditLogEntry of(long sequenceNo, SealedAuditEntry sealed) {
        return new AuditLogEntry(sequenceNo, sealed);
    }

    public SealedAuditEntry toSealed() {
        AuditRecord record = new AuditRecord(id, occurredAt, actorId, action, resourceType, resourceId, payload);
        return new SealedAuditEntry(record, previousHash, hash);
    }

    public long sequenceNo() {
        return sequenceNo;
    }

    public String hash() {
        return hash;
    }

    @Override
    public UUID getId() {
        return id;
    }

    // Ids are assigned by the caller, so tell Spring Data to insert rather than look the row up first.
    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        this.persisted = true;
    }
}
