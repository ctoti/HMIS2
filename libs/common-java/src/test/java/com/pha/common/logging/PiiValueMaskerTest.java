package com.pha.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonStreamContext;
import org.junit.jupiter.api.Test;

class PiiValueMaskerTest {

    private final PiiValueMasker masker = new PiiValueMasker();

    @Test
    void masksWholeValueOfSensitiveField() {
        assertThat(masker.mask(field("dateOfBirth"), "1990-01-01")).isEqualTo(PiiRedactor.MASK);
    }

    @Test
    void redactsPiiInsideFreeText() {
        assertThat(masker.mask(field("message"), "emailed a.b@pha.gov.pg"))
                .isEqualTo("emailed " + PiiRedactor.MASK);
    }

    @Test
    void returnsNullWhenNothingToMask() {
        assertThat(masker.mask(field("message"), "leave approved")).isNull();
    }

    @Test
    void ignoresNonTextValues() {
        assertThat(masker.mask(field("count"), 42)).isNull();
    }

    private static JsonStreamContext field(String name) {
        JsonStreamContext context = mock(JsonStreamContext.class);
        when(context.hasCurrentName()).thenReturn(true);
        when(context.getCurrentName()).thenReturn(name);
        return context;
    }
}
