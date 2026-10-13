package com.pha.common.audit.jpa;

import java.util.OptionalLong;

/**
 * Result of checking the stored chain: how many entries were checked and, if broken, the first bad sequence number.
 */
public record ChainVerification(long entriesChecked, OptionalLong firstBrokenSequenceNo) {

    static ChainVerification intact(long entriesChecked) {
        return new ChainVerification(entriesChecked, OptionalLong.empty());
    }

    static ChainVerification brokenAt(long entriesChecked, long sequenceNo) {
        return new ChainVerification(entriesChecked, OptionalLong.of(sequenceNo));
    }

    public boolean intact() {
        return firstBrokenSequenceNo.isEmpty();
    }
}
