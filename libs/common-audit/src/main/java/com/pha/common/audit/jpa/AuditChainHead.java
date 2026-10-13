package com.pha.common.audit.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The single row recording the end of the chain. Appenders lock it, so entries are sealed one at a time.
 */
@Entity
@Table(schema = "audit", name = "audit_chain_head")
public class AuditChainHead {

    public static final short SINGLETON_ID = 1;

    @Id
    private short id;

    @Column(name = "last_sequence_no", nullable = false)
    private long lastSequenceNo;

    @Column(name = "last_hash", nullable = false)
    private String lastHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuditChainHead() {
    }

    public long lastSequenceNo() {
        return lastSequenceNo;
    }

    public String lastHash() {
        return lastHash;
    }

    void advance(long sequenceNo, String hash) {
        this.lastSequenceNo = sequenceNo;
        this.lastHash = hash;
        this.updatedAt = Instant.now();
    }
}
