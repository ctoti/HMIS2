package com.pha.common.logging;

import com.fasterxml.jackson.core.JsonStreamContext;
import net.logstash.logback.mask.ValueMasker;

/**
 * Applies {@link PiiRedactor} to every JSON log field written by the logstash encoder.
 */
public class PiiValueMasker implements ValueMasker {

    @Override
    public Object mask(JsonStreamContext context, Object value) {
        if (context.hasCurrentName() && PiiRedactor.isSensitiveKey(context.getCurrentName())) {
            return PiiRedactor.MASK;
        }
        if (value instanceof CharSequence text) {
            String original = text.toString();
            String redacted = PiiRedactor.redact(original);
            return redacted.equals(original) ? null : redacted;
        }
        return null;
    }
}
