package com.pha.hr.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pha.hr.shared.ConflictException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeaveRequestTest {

    @Test
    void countsCalendarDaysInclusively() {
        assertThat(request("2026-11-02", "2026-11-02").getCalendarDays()).isEqualTo(1);
        assertThat(request("2026-12-28", "2027-01-03").getCalendarDays()).isEqualTo(7);
    }

    @Test
    void cancelsOnlyOnce() {
        LeaveRequest request = request("2026-11-02", "2026-11-03");

        request.cancel();

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThatThrownBy(request::cancel)
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ConflictException) ex).reason())
                .isEqualTo("leave-not-cancellable");
    }

    @Test
    void fingerprintsDependOnEveryField() {
        SubmitLeaveRequest base = submit(LeaveType.ANNUAL, "2026-11-02", "2026-11-03");

        assertThat(LeaveFingerprint.of(base)).hasSize(64).isEqualTo(LeaveFingerprint.of(
                submit(LeaveType.ANNUAL, "2026-11-02", "2026-11-03")));
        assertThat(LeaveFingerprint.of(base))
                .isNotEqualTo(LeaveFingerprint.of(submit(LeaveType.SICK, "2026-11-02", "2026-11-03")))
                .isNotEqualTo(LeaveFingerprint.of(submit(LeaveType.ANNUAL, "2026-11-01", "2026-11-03")))
                .isNotEqualTo(LeaveFingerprint.of(submit(LeaveType.ANNUAL, "2026-11-02", "2026-11-04")));
    }

    @Test
    void acceptsOnlyEndDatesOnOrAfterTheStart() {
        assertThat(submit(LeaveType.ANNUAL, "2026-11-02", "2026-11-02").isEndOnOrAfterStart()).isTrue();
        assertThat(submit(LeaveType.ANNUAL, "2026-11-02", "2026-11-01").isEndOnOrAfterStart()).isFalse();
        assertThat(new SubmitLeaveRequest(LeaveType.ANNUAL, null, null).isEndOnOrAfterStart()).isTrue();
    }

    private static LeaveRequest request(String start, String end) {
        return new LeaveRequest(UUID.randomUUID(), "PMGH", LeaveType.ANNUAL, LocalDate.parse(start),
                LocalDate.parse(end), "user-1", "key-12345", "f".repeat(64));
    }

    private static SubmitLeaveRequest submit(LeaveType type, String start, String end) {
        return new SubmitLeaveRequest(type, LocalDate.parse(start), LocalDate.parse(end));
    }
}
