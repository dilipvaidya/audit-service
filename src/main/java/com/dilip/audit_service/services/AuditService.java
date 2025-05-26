package com.dilip.audit_service.services;

import com.dilip.audit_service.common.DeletionResult;
import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import com.dilip.audit_service.data.repository.AuditRepository;
import com.dilip.audit_service.data.repository.AuditRepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
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
        //TODO: it should save to both the storages otherwise to none. write test case around it.
        this.auditRepositoryTransientDb.save(auditLog);
        this.auditRepositoryColdStorageDb.save(auditLog);
    }

    public List<AuditLog> queryAuditLogs() {
        return this.auditRepositoryTransientDb.findAll();
    }

    public Optional<AuditLog> findByEventId(String eventId) {
        return this.auditRepositoryTransientDb.findByEventId(eventId);
    }

    public List<AuditLog> findByUserId(String userId) {
        return this.auditRepositoryTransientDb.findByUserId(userId);
    }

    public List<AuditLog> advancedSearch(AuditSearchRequest request) {
        // Implement Elasticsearch or DB query logic
        return this.auditRepositoryTransientDb.advancedSearch(request);
    }

    public DeletionResult deleteEvent(String eventId) {
        return this.auditRepositoryTransientDb.deleteEvent(eventId);
    }
}


/*
    public Optional<AuditLog> findById(String eventId) {
        Optional<AuditLog> log = transientRepository.findById(eventId);
        if (log.isPresent()) {
            return log;
        }
        return coldStorageRepository != null ? coldStorageRepository.findById(eventId) : Optional.empty();
    }

    public List<AuditLog> advancedSearch(AuditSearchRequest request) {
        List<AuditLog> transientResults = transientRepository.advancedSearch(request);
        List<AuditLog> coldResults = coldStorageRepository != null ? coldStorageRepository.advancedSearch(request) : List.of();

        Instant threshold = Instant.now().minus(Duration.ofDays(14));

        List<AuditLog> finalResults = new ArrayList<>();
        for (AuditLog log : transientResults) {
            if (log.getTimestamp() != null && log.getTimestamp().isAfter(threshold)) {
                finalResults.add(log);
            }
        }
        for (AuditLog log : coldResults) {
            if (log.getTimestamp() != null && log.getTimestamp().isBefore(threshold)) {
                finalResults.add(log);
            }
        }
        return finalResults;
    }
*/