package com.pha.hr.leave;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByRequestedByAndIdempotencyKey(String requestedBy, String idempotencyKey);

    @Query("""
            select case when count(l) > 0 then true else false end from LeaveRequest l
            where l.staffId = :staffId and l.status = :status
              and l.startDate <= :endDate and l.endDate >= :startDate""")
    boolean existsOverlapping(@Param("staffId") UUID staffId, @Param("status") LeaveStatus status,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Page<LeaveRequest> findByRequestedBy(String requestedBy, Pageable pageable);

    Page<LeaveRequest> findByRequestedByAndStatus(String requestedBy, LeaveStatus status, Pageable pageable);

    Page<LeaveRequest> findByFacilityCode(String facilityCode, Pageable pageable);

    Page<LeaveRequest> findByFacilityCodeAndStatus(String facilityCode, LeaveStatus status, Pageable pageable);
}
