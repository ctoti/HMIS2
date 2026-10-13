package com.pha.hr.leave;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestResponse(
        UUID id,
        UUID staffId,
        String facilityCode,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        long calendarDays,
        LeaveStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
