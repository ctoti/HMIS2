package com.pha.hr.audit;

import com.pha.common.audit.AuditRecord;
import com.pha.common.audit.jpa.AuditLog;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Records HR writes in the hash-chained audit log. Callers pass identifiers and codes only, never names or other
 * personal details, because audit entries can never be edited or removed.
 */
@Component
public class HrAudit {

    private final AuditLog auditLog;
    private final JsonMapper json;
    private final Clock clock;

    public HrAudit(AuditLog auditLog, JsonMapper json, Clock clock) {
        this.auditLog = auditLog;
        this.json = json;
        this.clock = clock;
    }

    public void record(String actorId, String action, String resourceType, UUID resourceId, Map<String, ?> details) {
        auditLog.append(new AuditRecord(
                UUID.randomUUID(),
                clock.instant(),
                actorId,
                action,
                resourceType,
                resourceId.toString(),
                json.writeValueAsString(details)));
    }
}
