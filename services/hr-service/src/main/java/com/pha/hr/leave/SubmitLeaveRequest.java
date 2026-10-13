package com.pha.hr.leave;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SubmitLeaveRequest(@NotNull LeaveType leaveType, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {

    @AssertTrue(message = "must not be before startDate")
    public boolean isEndOnOrAfterStart() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
