package com.pha.hr.leave;

/**
 * The result of a submission: {@code created} is false when an Idempotency-Key replay returned the original request.
 */
public record LeaveSubmission(LeaveRequestResponse leaveRequest, boolean created) {
}
