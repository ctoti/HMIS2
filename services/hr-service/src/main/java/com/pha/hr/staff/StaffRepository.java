package com.pha.hr.staff;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByUserId(String userId);

    Optional<Staff> findByUserId(String userId);

    Page<Staff> findByFacilityCode(String facilityCode, Pageable pageable);

    /** Locks the caller's staff row so that one person's submissions are checked and saved one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Staff s where s.userId = :userId")
    Optional<Staff> findByUserIdForUpdate(@Param("userId") String userId);
}
