package com.pha.hr.leave;

import com.pha.hr.shared.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "leave_request")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID staffId;

    @Column(nullable = false, updatable = false)
    private String facilityCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LeaveType leaveType;

    @Column(nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(nullable = false, updatable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    @Column(nullable = false, updatable = false)
    private String requestedBy;

    @Column(nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private String requestFingerprint;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected LeaveRequest() {
    }

    LeaveRequest(UUID staffId, String facilityCode, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
            String requestedBy, String idempotencyKey, String requestFingerprint) {
        this.staffId = staffId;
        this.facilityCode = facilityCode;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = LeaveStatus.SUBMITTED;
        this.requestedBy = requestedBy;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
    }

    void cancel() {
        if (status != LeaveStatus.SUBMITTED) {
            throw new ConflictException("leave-not-cancellable", "Only submitted leave requests can be cancelled.");
        }
        status = LeaveStatus.CANCELLED;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Calendar days including both ends; working-day rules are an open question (Q11). */
    public long getCalendarDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public String getFacilityCode() {
        return facilityCode;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
