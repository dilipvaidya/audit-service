package com.dilip.audit_service.data.repository;

import com.dilip.audit_service.common.DeletionResult;
import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditRepository {

    void save(AuditLog auditLog);

    List<AuditLog> findAll();

    List<AuditLog> findByUserId(String userId);

    Optional<AuditLog> findByEventId(String eventId);

    List<AuditLog> advancedSearch(AuditSearchRequest request);

    DeletionResult deleteEvent(String eventId);
}
