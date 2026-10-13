package com.pha.common.audit.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChainVerificationTest {

    @Test
    void reportsIntactAndBrokenChains() {
        assertThat(ChainVerification.intact(3).intact()).isTrue();
        ChainVerification broken = ChainVerification.brokenAt(2, 3);
        assertThat(broken.intact()).isFalse();
        assertThat(broken.firstBrokenSequenceNo()).hasValue(3);
        assertThat(broken.entriesChecked()).isEqualTo(2);
    }
}
