package com.pha.common.audit.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AuditChainHeadRepository extends Repository<AuditChainHead, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from AuditChainHead h where h.id = :id")
    Optional<AuditChainHead> findForUpdate(@Param("id") short id);

    Optional<AuditChainHead> findById(short id);
}
