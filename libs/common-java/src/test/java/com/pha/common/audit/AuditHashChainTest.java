package com.pha.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditHashChainTest {

    @Test
    void firstEntryLinksToGenesisHash() {
        SealedAuditEntry entry = AuditHashChain.seal(AuditHashChain.GENESIS_HASH, record("{\"days\":3}"));

        assertThat(entry.previousHash()).isEqualTo(AuditHashChain.GENESIS_HASH);
        assertThat(entry.hash()).hasSize(64).isNotEqualTo(AuditHashChain.GENESIS_HASH);
    }

    @Test
    void hashIsDeterministic() {
        AuditRecord record = record("{}");

        assertThat(AuditHashChain.computeHash("abc", record)).isEqualTo(AuditHashChain.computeHash("abc", record));
    }

    @Test
    void intactChainHasNoBrokenLink() {
        assertThat(AuditHashChain.findFirstBrokenLink(chainOf(3))).isEmpty();
    }

    @Test
    void emptyChainIsIntact() {
        assertThat(AuditHashChain.findFirstBrokenLink(List.of())).isEmpty();
    }

    @Test
    void verifiesALaterSliceFromItsPredecessorHash() {
        List<SealedAuditEntry> chain = chainOf(4);
        List<SealedAuditEntry> secondHalf = chain.subList(2, 4);

        assertThat(AuditHashChain.findFirstBrokenLink(secondHalf, chain.get(1).hash())).isEmpty();
        assertThat(AuditHashChain.findFirstBrokenLink(secondHalf)).hasValue(0);
    }

    @Test
    void detectsTamperedPayload() {
        List<SealedAuditEntry> chain = chainOf(3);
        SealedAuditEntry original = chain.get(1);
        AuditRecord tampered = withPayload(original.record(), "{\"days\":30}");
        chain.set(1, new SealedAuditEntry(tampered, original.previousHash(), original.hash()));

        assertThat(AuditHashChain.findFirstBrokenLink(chain)).hasValue(1);
    }

    @Test
    void detectsDeletedEntry() {
        List<SealedAuditEntry> chain = chainOf(3);
        chain.remove(1);

        assertThat(AuditHashChain.findFirstBrokenLink(chain)).hasValue(1);
    }

    @Test
    void canonicalFormIsUnambiguousAcrossFieldBoundaries() {
        AuditRecord a = new AuditRecord(UUID.randomUUID(), Instant.EPOCH, "ab", "c", "t", "r", "p");
        AuditRecord b = new AuditRecord(a.id(), Instant.EPOCH, "a", "bc", "t", "r", "p");

        assertThat(AuditHashChain.canonicalForm(a)).isNotEqualTo(AuditHashChain.canonicalForm(b));
    }

    @Test
    void truncatesTimestampToMicroseconds() {
        AuditRecord record = new AuditRecord(UUID.randomUUID(), Instant.ofEpochSecond(0, 1_234_567),
                "actor", "UPDATE", "leave-request", "42", "{}");

        assertThat(record.occurredAt().getNano()).isEqualTo(1_234_000);
    }

    private static List<SealedAuditEntry> chainOf(int size) {
        List<SealedAuditEntry> chain = new ArrayList<>();
        String previous = AuditHashChain.GENESIS_HASH;
        for (int i = 0; i < size; i++) {
            SealedAuditEntry entry = AuditHashChain.seal(previous, record("{\"seq\":" + i + "}"));
            chain.add(entry);
            previous = entry.hash();
        }
        return chain;
    }

    private static AuditRecord record(String payload) {
        return new AuditRecord(UUID.randomUUID(), Instant.parse("2026-10-10T00:00:00Z"),
                "user-123", "CREATE", "leave-request", UUID.randomUUID().toString(), payload);
    }

    private static AuditRecord withPayload(AuditRecord r, String payload) {
        return new AuditRecord(r.id(), r.occurredAt(), r.actorId(), r.action(), r.resourceType(), r.resourceId(), payload);
    }
}
