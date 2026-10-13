package com.pha.hr.shared;

/**
 * A request that is valid on its own but conflicts with current state. Returned as 409; {@code reason} becomes the
 * problem type and the message its detail, so neither may contain PII.
 */
public class ConflictException extends RuntimeException {

    private final String reason;

    public ConflictException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
