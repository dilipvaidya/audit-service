package com.dilip.audit_service.services;

import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import com.dilip.audit_service.data.repository.AuditRepository;
import com.dilip.audit_service.data.repository.AuditRepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Profile({"dev", "prod"})
public class AuditService {

    private final AuditRepository auditRepositoryTransientDb;
    private final AuditRepository auditRepositoryColdStorageDb;

    @Autowired
    public AuditService(AuditRepositoryFactory auditRepositoryFactory) {
        this.auditRepositoryTransientDb = auditRepositoryFactory.getTransientRepository();
        this.auditRepositoryColdStorageDb = auditRepositoryFactory.getColdStorageRepository();
    }

    public void saveAuditLog(AuditLog auditLog) {
        this.auditRepositoryTransientDb.save(auditLog);
        this.auditRepositoryColdStorageDb.save(auditLog);
    }

    public List<AuditLog> queryAuditLogs(Optional<String> startTime,
                                         Optional<String> endTime,
                                         Optional<String> entityType,
                                         Optional<String> entityId,
                                         Optional<String> eventType,
                                         Optional<String> sourceService,
                                         Optional<String> changedByUserId,
                                         int page,
                                         int size,
                                         Optional<String> sort) {

        // Stub implementation, should apply filters dynamically
        return this.auditRepositoryTransientDb.findAll();
    }

    public Optional<AuditLog> findById(String eventId) {
        return this.auditRepositoryTransientDb.findById(eventId);
    }

    public List<AuditLog> advancedSearch(AuditSearchRequest request) {
        // Implement Elasticsearch or DB query logic
        return this.auditRepositoryTransientDb.advancedSearch(request);
    }
}
