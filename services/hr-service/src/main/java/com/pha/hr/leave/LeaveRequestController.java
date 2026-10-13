package com.pha.hr.leave;

import com.pha.hr.security.Caller;
import com.pha.hr.shared.PageResponse;
import com.pha.hr.shared.SortableFields;
import com.pha.hr.staff.StaffRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(LeaveRequestController.PATH)
public class LeaveRequestController {

    static final String PATH = "/api/v1/hr/leave-requests";
    static final String IDEMPOTENCY_KEY = "[A-Za-z0-9_-]{8,100}";
    static final SortableFields SORTABLE = SortableFields.of(
            "startDate", "endDate", "leaveType", "status", "createdAt", "updatedAt");

    private final LeaveRequestService service;

    public LeaveRequestController(LeaveRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('staff')")
    public ResponseEntity<LeaveRequestResponse> submit(
            @RequestHeader("Idempotency-Key") @Pattern(regexp = IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody SubmitLeaveRequest request,
            Authentication authentication) {
        LeaveSubmission submission = service.submit(Caller.from(authentication), idempotencyKey, request);
        LeaveRequestResponse body = submission.leaveRequest();
        if (!submission.created()) {
            return ResponseEntity.status(HttpStatus.OK).body(body);
        }
        return ResponseEntity.created(URI.create(PATH + "/" + body.id())).body(body);
    }

    /** Without {@code facilityCode}, lists the caller's own requests; with it, the facility's (HR officers only). */
    @GetMapping
    @PreAuthorize("#facilityCode == null or @facilityAccess.canManage(authentication, #facilityCode)")
    public PageResponse<LeaveRequestResponse> list(
            @RequestParam(required = false) @Pattern(regexp = StaffRules.FACILITY_CODE) String facilityCode,
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        Pageable checked = SORTABLE.check(pageable);
        if (facilityCode == null) {
            return service.listMine(Caller.from(authentication), status, checked);
        }
        return service.listByFacility(facilityCode, status, checked);
    }

    @GetMapping("/{id}")
    public LeaveRequestResponse get(@PathVariable UUID id, Authentication authentication) {
        return service.get(Caller.from(authentication), id);
    }

    @PostMapping("/{id}/cancel")
    public LeaveRequestResponse cancel(@PathVariable UUID id, Authentication authentication) {
        return service.cancel(Caller.from(authentication), id);
    }
}
