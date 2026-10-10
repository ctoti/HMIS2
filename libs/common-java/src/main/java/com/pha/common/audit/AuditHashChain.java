package com.pha.common.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.OptionalInt;

/**
 * Append-only audit chain where each entry's hash = SHA-256(previousHash + canonical record).
 */
public final class AuditHashChain {

    public static final String GENESIS_HASH = "0".repeat(64);

    private AuditHashChain() {
    }

    public static SealedAuditEntry seal(String previousHash, AuditRecord record) {
        return new SealedAuditEntry(record, previousHash, computeHash(previousHash, record));
    }

    /**
     * Returns the index of the first entry that breaks the chain, or empty if the chain is intact.
     */
    public static OptionalInt findFirstBrokenLink(List<SealedAuditEntry> entries) {
        String expectedPrevious = GENESIS_HASH;
        for (int i = 0; i < entries.size(); i++) {
            SealedAuditEntry entry = entries.get(i);
            boolean linked = entry.previousHash().equals(expectedPrevious);
            boolean intact = entry.hash().equals(computeHash(entry.previousHash(), entry.record()));
            if (!linked || !intact) {
                return OptionalInt.of(i);
            }
            expectedPrevious = entry.hash();
        }
        return OptionalInt.empty();
    }

    static String computeHash(String previousHash, AuditRecord record) {
        MessageDigest digest = sha256();
        digest.update(previousHash.getBytes(StandardCharsets.UTF_8));
        digest.update(canonicalForm(record).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }

    // Length-prefixing each field makes the encoding unambiguous regardless of field content.
    static String canonicalForm(AuditRecord record) {
        StringBuilder out = new StringBuilder();
        for (String field : List.of(
                record.id().toString(),
                record.occurredAt().toString(),
                record.actorId(),
                record.action(),
                record.resourceType(),
                record.resourceId(),
                record.payload())) {
            out.append(field.length()).append(':').append(field).append(';');
        }
        return out.toString();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}
