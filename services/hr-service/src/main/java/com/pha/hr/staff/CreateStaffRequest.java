package com.pha.hr.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,32}") String employeeNumber,
        @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9-]+") String userId,
        @NotBlank @Size(max = 100) String givenName,
        @NotBlank @Size(max = 100) String familyName,
        @NotBlank @Pattern(regexp = StaffRules.FACILITY_CODE) String facilityCode,
        @NotBlank @Size(max = 120) String positionTitle) {
}
