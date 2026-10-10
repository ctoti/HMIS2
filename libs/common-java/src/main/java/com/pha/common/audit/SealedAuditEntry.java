package com.pha.common.audit;

import java.util.Objects;

/**
 * An audit record linked into the chain by its predecessor's hash.
 */
public record SealedAuditEntry(AuditRecord record, String previousHash, String hash) {

    public SealedAuditEntry {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(previousHash, "previousHash");
        Objects.requireNonNull(hash, "hash");
    }
}
