package com.pha.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PiiRedactorTest {

    @ParameterizedTest
    @ValueSource(strings = {"name", "firstName", "last_name", "patientName", "dob", "dateOfBirth",
            "MRN", "nationalId", "email", "phoneNumber", "homeAddress"})
    void recognisesSensitiveKeys(String key) {
        assertThat(PiiRedactor.isSensitiveKey(key)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"status", "facilityCode", "leaveType", "fileName", "id"})
    void leavesOrdinaryKeysAlone(String key) {
        assertThat(PiiRedactor.isSensitiveKey(key)).isFalse();
    }

    @Test
    void nullKeyIsNotSensitive() {
        assertThat(PiiRedactor.isSensitiveKey(null)).isFalse();
    }

    @Test
    void redactsJsonFieldValues() {
        String json = "{\"patientName\":\"Mary Kila\",\"mrn\":\"PMGH-00123\",\"ward\":\"B\"}";

        assertThat(PiiRedactor.redact(json))
                .isEqualTo("{\"patientName\":\"[REDACTED]\",\"mrn\":\"[REDACTED]\",\"ward\":\"B\"}");
    }

    @Test
    void redactsKeyValuePairs() {
        assertThat(PiiRedactor.redact("lookup mrn=PMGH-00123 status=OK"))
                .isEqualTo("lookup mrn=[REDACTED] status=OK");
    }

    @Test
    void redactsEmailAddresses() {
        assertThat(PiiRedactor.redact("sent to nurse.kila@pha.gov.pg today"))
                .isEqualTo("sent to [REDACTED] today");
    }

    @Test
    void redactsPngPhoneNumbers() {
        assertThat(PiiRedactor.redact("call +675 7123 4567 or 7123-4567"))
                .isEqualTo("call [REDACTED] or [REDACTED]");
    }

    @Test
    void keepsOrdinaryNumbers() {
        assertThat(PiiRedactor.redact("processed 1234567 rows in 812 ms"))
                .isEqualTo("processed 1234567 rows in 812 ms");
    }

    @Test
    void passesThroughNullAndEmpty() {
        assertThat(PiiRedactor.redact(null)).isNull();
        assertThat(PiiRedactor.redact("")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "a", "a."})
    void staysFastOnHostileInput(String unit) {
        String hostile = "a@" + unit.repeat(50_000);

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> PiiRedactor.redact(hostile));
    }

    @Test
    void stillMasksLongQuotedValues() {
        String json = "{\"name\":\"" + "x".repeat(2000) + "\"}";

        assertThat(PiiRedactor.redact(json)).isEqualTo("{\"name\":\"[REDACTED]\"}");
    }
}
