package com.pha.hr.staff;

import java.time.Instant;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String employeeNumber,
        String userId,
        String givenName,
        String familyName,
        String facilityCode,
        String positionTitle,
        Instant createdAt,
        Instant updatedAt) {
}
