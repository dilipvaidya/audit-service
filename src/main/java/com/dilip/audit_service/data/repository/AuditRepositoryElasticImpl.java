package com.dilip.audit_service.data.repository;

import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuditRepositoryElasticImpl implements AuditRepository {
    @Override
    public void save(AuditLog auditLog) {

    }

    @Override
    public List<AuditLog> findAll() {
        return List.of();
    }

    @Override
    public Optional<AuditLog> findById(String eventId) {
        return Optional.empty();
    }

    @Override
    public List<AuditLog> advancedSearch(AuditSearchRequest request) {
        return List.of();
    }
}
