package com.pha.hr.leave;

import com.pha.common.error.ResourceNotFoundException;
import com.pha.hr.audit.HrAudit;
import com.pha.hr.security.Caller;
import com.pha.hr.shared.ConflictException;
import com.pha.hr.shared.PageResponse;
import com.pha.hr.staff.Staff;
import com.pha.hr.staff.StaffRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveRequestService {

    static final String RESOURCE_TYPE = "leave-request";

    private final LeaveRequestRepository requests;
    private final StaffRepository staff;
    private final LeaveRequestMapper mapper;
    private final HrAudit audit;

    public LeaveRequestService(LeaveRequestRepository requests, StaffRepository staff, LeaveRequestMapper mapper,
            HrAudit audit) {
        this.requests = requests;
        this.staff = staff;
        this.mapper = mapper;
        this.audit = audit;
    }

    /**
     * Submits a leave request for the caller's own staff record. Repeating a call with the same Idempotency-Key and
     * body returns the original request instead of creating a second one.
     */
    @Transactional
    public LeaveSubmission submit(Caller caller, String idempotencyKey, SubmitLeaveRequest request) {
        // Locking the staff row serialises one person's submissions, so the checks below cannot race each other.
        Staff requester = staff.findByUserIdForUpdate(caller.userId()).orElseThrow(() -> new ConflictException(
                "staff-record-not-linked",
                "Your account is not linked to a staff record. Ask your HR officer to link it."));
        String fingerprint = LeaveFingerprint.of(request);
        var earlier = requests.findByRequestedByAndIdempotencyKey(caller.userId(), idempotencyKey);
        if (earlier.isPresent()) {
            return replay(earlier.get(), fingerprint);
        }
        if (requests.existsOverlapping(requester.getId(), LeaveStatus.SUBMITTED, request.startDate(), request.endDate())) {
            throw new ConflictException("overlapping-leave",
                    "You already have a submitted leave request that overlaps these dates.");
        }
        LeaveRequest saved = requests.saveAndFlush(new LeaveRequest(requester.getId(), requester.getFacilityCode(),
                request.leaveType(), request.startDate(), request.endDate(), caller.userId(), idempotencyKey,
                fingerprint));
        audit.record(caller.userId(), "leave-request.submitted", RESOURCE_TYPE, saved.getId(), Map.of(
                "staffId", requester.getId().toString(),
                "facilityCode", saved.getFacilityCode(),
                "leaveType", saved.getLeaveType().name(),
                "startDate", saved.getStartDate().toString(),
                "endDate", saved.getEndDate().toString()));
        return new LeaveSubmission(mapper.toResponse(saved), true);
    }

    private LeaveSubmission replay(LeaveRequest earlier, String fingerprint) {
        if (!earlier.getRequestFingerprint().equals(fingerprint)) {
            throw new ConflictException("idempotency-key-reused",
                    "This Idempotency-Key was already used for a different leave request.");
        }
        return new LeaveSubmission(mapper.toResponse(earlier), false);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> listMine(Caller caller, LeaveStatus status, Pageable pageable) {
        Page<LeaveRequest> page = status == null
                ? requests.findByRequestedBy(caller.userId(), pageable)
                : requests.findByRequestedByAndStatus(caller.userId(), status, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> listByFacility(String facilityCode, LeaveStatus status, Pageable pageable) {
        Page<LeaveRequest> page = status == null
                ? requests.findByFacilityCode(facilityCode, pageable)
                : requests.findByFacilityCodeAndStatus(facilityCode, status, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse get(Caller caller, UUID id) {
        LeaveRequest found = load(id);
        if (!caller.userId().equals(found.getRequestedBy()) && !caller.canManageFacility(found.getFacilityCode())) {
            throw new AccessDeniedException("Leave request is outside the caller's access");
        }
        return mapper.toResponse(found);
    }

    @Transactional
    public LeaveRequestResponse cancel(Caller caller, UUID id) {
        LeaveRequest found = load(id);
        if (!caller.userId().equals(found.getRequestedBy())) {
            throw new AccessDeniedException("Only the requester can cancel a leave request");
        }
        found.cancel();
        LeaveRequest saved = requests.saveAndFlush(found);
        audit.record(caller.userId(), "leave-request.cancelled", RESOURCE_TYPE, saved.getId(), Map.of(
                "staffId", saved.getStaffId().toString(),
                "facilityCode", saved.getFacilityCode()));
        return mapper.toResponse(saved);
    }

    private LeaveRequest load(UUID id) {
        return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request", id));
    }
}
