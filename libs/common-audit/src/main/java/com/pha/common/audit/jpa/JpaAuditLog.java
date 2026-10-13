package com.pha.common.audit.jpa;

import com.pha.common.audit.AuditHashChain;
import com.pha.common.audit.AuditRecord;
import com.pha.common.audit.SealedAuditEntry;
import java.util.List;
import java.util.OptionalInt;
import org.springframework.transaction.annotation.Transactional;

public class JpaAuditLog implements AuditLog {

    private static final int PAGE_SIZE = 500;

    private final AuditLogEntryRepository entries;
    private final AuditChainHeadRepository heads;

    public JpaAuditLog(AuditLogEntryRepository entries, AuditChainHeadRepository heads) {
        this.entries = entries;
        this.heads = heads;
    }

    @Override
    @Transactional
    public SealedAuditEntry append(AuditRecord record) {
        AuditChainHead head = heads.findForUpdate(AuditChainHead.SINGLETON_ID).orElseThrow(AuditLog::missingHead);
        SealedAuditEntry sealed = AuditHashChain.seal(head.lastHash(), record);
        long sequenceNo = head.lastSequenceNo() + 1;
        entries.save(AuditLogEntry.of(sequenceNo, sealed));
        head.advance(sequenceNo, sealed.hash());
        return sealed;
    }

    @Override
    @Transactional(readOnly = true)
    public ChainVerification verify() {
        String expectedPrevious = AuditHashChain.GENESIS_HASH;
        long lastSequenceNo = 0;
        long checked = 0;
        List<AuditLogEntry> page;
        do {
            page = entries.findTop500BySequenceNoGreaterThanOrderBySequenceNoAsc(lastSequenceNo);
            OptionalInt broken = AuditHashChain.findFirstBrokenLink(
                    page.stream().map(AuditLogEntry::toSealed).toList(), expectedPrevious);
            if (broken.isPresent()) {
                return ChainVerification.brokenAt(checked + broken.getAsInt(), page.get(broken.getAsInt()).sequenceNo());
            }
            if (!page.isEmpty()) {
                AuditLogEntry last = page.get(page.size() - 1);
                expectedPrevious = last.hash();
                lastSequenceNo = last.sequenceNo();
                checked += page.size();
            }
        } while (page.size() == PAGE_SIZE);
        return checkHead(checked, lastSequenceNo, expectedPrevious);
    }

    // A chain whose newest rows were removed still links up, so it must also end where the head says it does.
    private ChainVerification checkHead(long checked, long lastSequenceNo, String lastHash) {
        AuditChainHead head = heads.findById(AuditChainHead.SINGLETON_ID).orElseThrow(AuditLog::missingHead);
        if (head.lastSequenceNo() != lastSequenceNo || !head.lastHash().equals(lastHash)) {
            return ChainVerification.brokenAt(checked, lastSequenceNo + 1);
        }
        return ChainVerification.intact(checked);
    }
}
