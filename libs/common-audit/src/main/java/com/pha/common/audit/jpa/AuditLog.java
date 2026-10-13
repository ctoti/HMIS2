package com.pha.common.audit.jpa;

import com.pha.common.audit.AuditRecord;
import com.pha.common.audit.SealedAuditEntry;

/**
 * Records audited writes. {@link #append} joins the caller's transaction, so an audit entry commits or rolls back
 * together with the change it describes.
 */
public interface AuditLog {

    SealedAuditEntry append(AuditRecord record);

    ChainVerification verify();

    static IllegalStateException missingHead() {
        return new IllegalStateException("audit.audit_chain_head row is missing; the audit schema migration did not run");
    }
}
