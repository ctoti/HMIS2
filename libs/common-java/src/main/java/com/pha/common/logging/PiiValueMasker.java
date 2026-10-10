package com.pha.common.logging;

import tools.jackson.core.TokenStreamContext;
import net.logstash.logback.mask.ValueMasker;

/**
 * Applies {@link PiiRedactor} to every JSON log field written by the logstash encoder.
 */
public class PiiValueMasker implements ValueMasker {

    @Override
    public Object mask(TokenStreamContext context, Object value) {
        if (context.hasCurrentName() && PiiRedactor.isSensitiveKey(context.currentName())) {
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
