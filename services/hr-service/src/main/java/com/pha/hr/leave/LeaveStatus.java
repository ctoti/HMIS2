package com.pha.hr.leave;

/**
 * Lifecycle owned by hr-service. Approval decisions will come from the BPM workflow (ADR 0009), not from here.
 */
public enum LeaveStatus {
    SUBMITTED,
    CANCELLED
}
