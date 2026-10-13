package com.pha.hr.leave;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 of the request body's fields, used to tell an Idempotency-Key replay from a reuse with different content.
 */
final class LeaveFingerprint {

    private LeaveFingerprint() {
    }

    static String of(SubmitLeaveRequest request) {
        String canonical = request.leaveType() + "|" + request.startDate() + "|" + request.endDate();
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }
}
