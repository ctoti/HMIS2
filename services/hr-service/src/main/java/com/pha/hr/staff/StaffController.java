package com.pha.hr.staff;

import com.pha.hr.security.Caller;
import com.pha.hr.shared.PageResponse;
import com.pha.hr.shared.SortableFields;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(StaffController.PATH)
public class StaffController {

    static final String PATH = "/api/v1/hr/staff";
    static final SortableFields SORTABLE = SortableFields.of(
            "employeeNumber", "familyName", "givenName", "positionTitle", "createdAt", "updatedAt");

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@facilityAccess.canManage(authentication, #request.facilityCode())")
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request,
            Authentication authentication) {
        StaffResponse created = service.create(Caller.from(authentication), request);
        return ResponseEntity.created(URI.create(PATH + "/" + created.id())).body(created);
    }

    @GetMapping
    @PreAuthorize("@facilityAccess.canManage(authentication, #facilityCode)")
    public PageResponse<StaffResponse> list(@RequestParam @Pattern(regexp = StaffRules.FACILITY_CODE) String facilityCode,
            @PageableDefault(sort = "employeeNumber") Pageable pageable) {
        return service.listByFacility(facilityCode, SORTABLE.check(pageable));
    }

    @GetMapping("/me")
    public StaffResponse mine(Authentication authentication) {
        return service.mine(Caller.from(authentication));
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable UUID id, Authentication authentication) {
        return service.get(Caller.from(authentication), id);
    }
}
