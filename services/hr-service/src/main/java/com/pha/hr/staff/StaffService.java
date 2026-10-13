package com.pha.hr.staff;

import com.pha.common.error.ResourceNotFoundException;
import com.pha.hr.audit.HrAudit;
import com.pha.hr.security.Caller;
import com.pha.hr.shared.ConflictException;
import com.pha.hr.shared.PageResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {

    static final String RESOURCE_TYPE = "staff";

    private final StaffRepository staff;
    private final StaffMapper mapper;
    private final HrAudit audit;

    public StaffService(StaffRepository staff, StaffMapper mapper, HrAudit audit) {
        this.staff = staff;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Transactional
    public StaffResponse create(Caller caller, CreateStaffRequest request) {
        if (staff.existsByEmployeeNumber(request.employeeNumber())) {
            throw new ConflictException("duplicate-employee-number",
                    "A staff record with this employee number already exists.");
        }
        if (request.userId() != null && staff.existsByUserId(request.userId())) {
            throw new ConflictException("account-already-linked",
                    "This sign-in account is already linked to another staff record.");
        }
        Staff saved = staff.saveAndFlush(new Staff(request.employeeNumber(), request.userId(), request.givenName(),
                request.familyName(), request.facilityCode(), request.positionTitle()));
        audit.record(caller.userId(), "staff.created", RESOURCE_TYPE, saved.getId(), Map.of(
                "facilityCode", saved.getFacilityCode(),
                "accountLinked", saved.getUserId() != null));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> listByFacility(String facilityCode, Pageable pageable) {
        return PageResponse.of(staff.findByFacilityCode(facilityCode, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Caller caller, UUID id) {
        Staff found = staff.findById(id).orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        boolean self = caller.userId().equals(found.getUserId());
        if (!self && !caller.canManageFacility(found.getFacilityCode())) {
            throw new AccessDeniedException("Staff record is outside the caller's facilities");
        }
        return mapper.toResponse(found);
    }

    @Transactional(readOnly = true)
    public StaffResponse mine(Caller caller) {
        return staff.findByUserId(caller.userId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Staff record", "for this account"));
    }
}
